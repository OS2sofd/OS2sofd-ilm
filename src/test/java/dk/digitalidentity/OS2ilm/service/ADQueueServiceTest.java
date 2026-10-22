package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.exception.ADOperationException;
import dk.digitalidentity.OS2ilm.exception.RetryableException;
import dk.digitalidentity.OS2ilm.model.entity.AdGroup;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Business;
import dk.digitalidentity.OS2ilm.model.entity.BusinessEmploymentSettings;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.enums.AccountNameStrategy;
import dk.digitalidentity.OS2ilm.service.ADQueueService.ADMessageRequest;
import dk.digitalidentity.OS2ilm.service.ADQueueService.OperationType;
import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.simple_queue.QueueMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ADQueueServiceTest {

    @Mock private EmployeeService employeeService;
    @Mock private ActiveDirectorySyncService activeDirectorySyncService;
    @Mock private AdGroupService adGroupService;
    @Mock private QueueHelperService queueHelperService;
    @Mock private AuditLogService auditLogService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private EmailTemplateService emailTemplateService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private IlmConfiguration configuration;
    @Mock private MessageService messageService;
    @Mock private AccountNameTemplateService accountNameTemplateService;

    private EncryptionUtil encryptionUtil;
    private ADQueueService adQueueService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lenient().when(configuration.getActiveDirectoryConfig().getPasswordLength()).thenReturn(12);
        lenient().when(configuration.getPasswordSecret()).thenReturn("test-secret-key-1");
        EmailTemplate disabledTemplate = new EmailTemplate();
        disabledTemplate.setEnabled(false);
        lenient().when(emailTemplateService.getByType(any())).thenReturn(disabledTemplate);
        encryptionUtil = new EncryptionUtil();
        ReflectionTestUtils.setField(encryptionUtil, "configuration", configuration);
        adQueueService = new ADQueueService(
                eventPublisher, employeeService, objectMapper,
                activeDirectorySyncService, adGroupService, queueHelperService, auditLogService,
                emailTemplateService, messageService, encryptionUtil, accountNameTemplateService
        );
    }

    private String ep(String plain) {
        try {
            return encryptionUtil.encryptPassword(plain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private QueueMessage messageFor(ADMessageRequest request) {
        String body = objectMapper.writeValueAsString(request);
        return QueueMessage.builder().queue("ACTIVE_DIRECTORY_QUEUE").priority(1L).body(body).build();
    }

    private Employee basicEmployee(long id) {
        Employee e = new Employee();
        e.setId(id);
        e.setName("Test Employee");
        e.setAdGroups(new HashSet<>());
        return e;
    }

    private AdGroup adGroup(long id) {
        AdGroup g = new AdGroup();
        g.setId(id);
        g.setName("group-" + id);
        return g;
    }

    @Nested
    class HandleMessage {

        @Nested
        class WhenEmployeeNotFound {
            @Test
            void shouldKeepMessageInQueueWhenEmployeeIsNull() {
                // Arrange
                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.CREATE)
						.employeeId(99L)
						.build();
                when(employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(99L)).thenReturn(null);

				assertThatThrownBy(() -> adQueueService.handleMessage(messageFor(request)))
						.isInstanceOf(RuntimeException.class)
						.hasCauseInstanceOf(IllegalArgumentException.class);
				verifyNoInteractions(activeDirectorySyncService);
            }
        }

		@Nested
		class CreateOperation {

			private AdUser adUser(String name) {
				AdUser u = new AdUser();
				u.setName(name);
				return u;
			}

			private void twoCallSetup(long id, Employee before, Employee after) {
				when(employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(id))
						.thenReturn(before)
						.thenReturn(after);
			}

			@Test
			void shouldCallAllCreationStepsOnFirstAttempt() {
				Employee before = basicEmployee(1L);
				Employee after = basicEmployee(1L);
				AdUser user = adUser("jsmith");
				after.setUser(user);
				twoCallSetup(1L, before, after);
				when(activeDirectorySyncService.makeAdUserName(before)).thenReturn("jsmith");
				when(activeDirectorySyncService.createAccount(eq(1L), any(), eq("jsmith"))).thenReturn(user);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.password(ep("test"))
						.build();
				boolean result = adQueueService.handleMessage(messageFor(request));

				assertThat(result).isTrue();
				verify(activeDirectorySyncService).createAccount(eq(1L), any(), eq("jsmith"));
				verify(activeDirectorySyncService).setPassword(eq(1L), any(), eq(false));
				verify(activeDirectorySyncService).setCprAttribute(eq(1L), any());
				verify(activeDirectorySyncService, never()).setExpireDate(any(), any());
			}

			@Test
			void shouldSetExpireDateWhenEmployeeHasStopDate() {
				LocalDate stop = LocalDate.of(2027, 6, 30);
				Employee before = basicEmployee(1L);
				before.setStopDate(stop);
				Employee after = basicEmployee(1L);
				AdUser user = adUser("jsmith");
				after.setUser(user);
				twoCallSetup(1L, before, after);
				when(activeDirectorySyncService.makeAdUserName(before)).thenReturn("jsmith");
				when(activeDirectorySyncService.createAccount(eq(1L), any(), eq("jsmith"))).thenReturn(user);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.password(ep("test"))
						.build();
				adQueueService.handleMessage(messageFor(request));

				verify(activeDirectorySyncService).setExpireDate(1L, stop);
			}

			@Test
			void shouldSkipExpireDateWhenNoStopDate() {
				Employee before = basicEmployee(1L);
				Employee after = basicEmployee(1L);
				AdUser user = adUser("jsmith");
				after.setUser(user);
				twoCallSetup(1L, before, after);
				when(activeDirectorySyncService.makeAdUserName(before)).thenReturn("jsmith");
				when(activeDirectorySyncService.createAccount(eq(1L), any(), eq("jsmith"))).thenReturn(user);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.password(ep("test"))
						.build();
				adQueueService.handleMessage(messageFor(request));

				verify(activeDirectorySyncService, never()).setExpireDate(any(), any());
			}

			@Test
			void shouldSkipSetPasswordWhenInitialPasswordAlreadySet() {
				Employee before = basicEmployee(1L);
				Employee after = basicEmployee(1L);
				AdUser user = adUser("jsmith");
				user.setInitialPasswordSet(true);
				after.setUser(user);
				twoCallSetup(1L, before, after);
				when(activeDirectorySyncService.makeAdUserName(before)).thenReturn("jsmith");
				when(activeDirectorySyncService.createAccount(eq(1L), any(), eq("jsmith"))).thenReturn(user);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.password(ep("test"))
						.build();
				adQueueService.handleMessage(messageFor(request));

				verify(activeDirectorySyncService, never()).setPassword(any(), any(), anyBoolean());
			}

			@Test
			void shouldSkipSetCprWhenAlreadySet() {
				Employee before = basicEmployee(1L);
				Employee after = basicEmployee(1L);
				AdUser user = adUser("jsmith");
				user.setCpr("1234567890");
				after.setUser(user);
				twoCallSetup(1L, before, after);
				when(activeDirectorySyncService.makeAdUserName(before)).thenReturn("jsmith");
				when(activeDirectorySyncService.createAccount(eq(1L), any(), eq("jsmith"))).thenReturn(user);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.password(ep("test"))
						.build();
				adQueueService.handleMessage(messageFor(request));

				verify(activeDirectorySyncService, never()).setCprAttribute(any(), any());
			}

			@Test
			void shouldSkipCreateAccountOnRetryAndRunRemainingSteps() {
				Employee employee = basicEmployee(1L);
				AdUser user = adUser("jsmith");
				employee.setUser(user);
				when(employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(1L)).thenReturn(employee);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.password(ep("test"))
						.build();
				adQueueService.handleMessage(messageFor(request));

				verify(activeDirectorySyncService, never()).createAccount(any(), any(), any());
				verify(activeDirectorySyncService).setPassword(eq(1L), any(), eq(false));
				verify(activeDirectorySyncService).setCprAttribute(eq(1L), any());
			}

			@Test
			void shouldQueueGroupMembershipsOnlyOnFirstCreation() {
				Employee before = basicEmployee(1L);
				Employee after = basicEmployee(1L);
				AdUser user = adUser("jsmith");
				after.setUser(user);
				twoCallSetup(1L, before, after);
				when(activeDirectorySyncService.makeAdUserName(before)).thenReturn("jsmith");
				when(activeDirectorySyncService.createAccount(eq(1L), any(), eq("jsmith"))).thenReturn(user);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.groupIds(List.of(10L, 20L))
						.password(ep("test"))
						.build();
				adQueueService.handleMessage(messageFor(request));

				// Assert — two SET_GROUP_MEMBERSHIP events published
				ArgumentCaptor<QueueMessage> captor = ArgumentCaptor.forClass(QueueMessage.class);
				// one for createAdUser (internal), two for group membership
				verify(eventPublisher, atLeast(2)).publishEvent(captor.capture());
				long groupMembershipEvents = captor.getAllValues().stream()
						.filter(m -> m.getBody() != null && m.getBody().contains("SET_GROUP_MEMBERSHIP"))
						.count();
				assertThat(groupMembershipEvents).isEqualTo(2);

			}

			@Test
			void shouldNotQueueGroupMembershipsOnRetry() {
				Employee employee = basicEmployee(1L);
				AdUser user = adUser("jsmith");
				employee.setUser(user);
				when(employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(1L)).thenReturn(employee);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.groupIds(List.of(10L, 20L))
						.password(ep("test"))
						.build();
				adQueueService.handleMessage(messageFor(request));

				verify(activeDirectorySyncService, never()).createAccount(any(), any(), any());
				verify(eventPublisher, never()).publishEvent(any());
			}

			@Test
			void shouldUseSofdGeneratedNameWhenConfigured() {
				Business business = new Business();
				BusinessEmploymentSettings settings = new BusinessEmploymentSettings();
				settings.setNameStrategy(AccountNameStrategy.SOFD);
				business.setBusinessEmploymentSettings(settings);

				Employee before = basicEmployee(1L);
				before.setBusiness(business);
				Employee after = basicEmployee(1L);
				after.setBusiness(business);
				AdUser user = adUser("sofd-name");
				after.setUser(user);
				twoCallSetup(1L, before, after);
				when(queueHelperService.sofdGenerationHelper(1L, "person-uuid-1")).thenReturn("sofd-name");
				when(activeDirectorySyncService.createAccount(eq(1L), any(), eq("sofd-name"))).thenReturn(user);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.personUuid("person-uuid-1")
						.password(ep("test"))
						.build();
				adQueueService.handleMessage(messageFor(request));

				verify(activeDirectorySyncService).createAccount(eq(1L), any(), eq("sofd-name"));
				verify(activeDirectorySyncService, never()).makeAdUserName(any());
			}

			@Test
			void shouldFallBackToPrefixedWhenNoBusinessEmploymentSettings() {
				Employee before = basicEmployee(1L);
				Employee after = basicEmployee(1L);
				AdUser user = adUser("jsmith");
				after.setUser(user);
				twoCallSetup(1L, before, after);
				when(activeDirectorySyncService.makeAdUserName(before)).thenReturn("jsmith");
				when(activeDirectorySyncService.createAccount(eq(1L), any(), eq("jsmith"))).thenReturn(user);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.password(ep("test"))
						.build();
				adQueueService.handleMessage(messageFor(request));

				verify(activeDirectorySyncService).makeAdUserName(before);
				verify(activeDirectorySyncService).createAccount(eq(1L), any(), eq("jsmith"));
			}

			@Test
			void shouldFallBackToPrefixedWhenNameStrategyIsNull() {
				Business business = new Business();
				BusinessEmploymentSettings settings = new BusinessEmploymentSettings();
				settings.setNameStrategy(null);
				business.setBusinessEmploymentSettings(settings);

				Employee before = basicEmployee(1L);
				before.setBusiness(business);
				Employee after = basicEmployee(1L);
				after.setBusiness(business);
				AdUser user = adUser("jsmith");
				after.setUser(user);
				twoCallSetup(1L, before, after);
				when(activeDirectorySyncService.makeAdUserName(before)).thenReturn("jsmith");
				when(activeDirectorySyncService.createAccount(eq(1L), any(), eq("jsmith"))).thenReturn(user);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.password(ep("test"))
						.build();
				adQueueService.handleMessage(messageFor(request));

				verify(activeDirectorySyncService).makeAdUserName(before);
			}

			@Test
			void shouldUsePendingAccountNameOverrideRegardlessOfStrategy() {
				Business business = new Business();
				BusinessEmploymentSettings settings = new BusinessEmploymentSettings();
				settings.setNameStrategy(AccountNameStrategy.PREFIXED);
				business.setBusinessEmploymentSettings(settings);

				Employee before = basicEmployee(1L);
				before.setBusiness(business);
				before.setPendingAccountName("retried-name");
				Employee after = basicEmployee(1L);
				after.setBusiness(business);
				AdUser user = adUser("retried-name");
				after.setUser(user);
				twoCallSetup(1L, before, after);
				when(activeDirectorySyncService.createAccount(eq(1L), any(), eq("retried-name"))).thenReturn(user);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.password(ep("test"))
						.build();
				adQueueService.handleMessage(messageFor(request));

				verify(activeDirectorySyncService).createAccount(eq(1L), any(), eq("retried-name"));
				verify(activeDirectorySyncService, never()).makeAdUserName(any());
			}

			@Test
			void shouldUseTailoredTemplateWhenConfigured() {
				Business business = new Business();
				BusinessEmploymentSettings settings = new BusinessEmploymentSettings();
				settings.setNameStrategy(AccountNameStrategy.TAILORED);
				settings.setAccountNameTemplate("lev-{person-initialer}-{løbenummer}");
				business.setBusinessEmploymentSettings(settings);

				Employee before = basicEmployee(1L);
				before.setBusiness(business);
				Employee after = basicEmployee(1L);
				after.setBusiness(business);
				AdUser user = adUser("lev-jd-0001");
				after.setUser(user);
				twoCallSetup(1L, before, after);
				when(accountNameTemplateService.resolve("lev-{person-initialer}-{løbenummer}", before)).thenReturn("lev-jd-0001");
				when(activeDirectorySyncService.createAccount(eq(1L), any(), eq("lev-jd-0001"))).thenReturn(user);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.password(ep("test"))
						.build();
				adQueueService.handleMessage(messageFor(request));

				verify(activeDirectorySyncService).createAccount(eq(1L), any(), eq("lev-jd-0001"));
				verify(activeDirectorySyncService, never()).makeAdUserName(any());
			}

			@Test
			void shouldUseCustomPendingAccountNameWhenConfigured() {
				Business business = new Business();
				BusinessEmploymentSettings settings = new BusinessEmploymentSettings();
				settings.setNameStrategy(AccountNameStrategy.CUSTOM);
				business.setBusinessEmploymentSettings(settings);

				Employee before = basicEmployee(1L);
				before.setBusiness(business);
				before.setPendingAccountName("chosen-name");
				Employee after = basicEmployee(1L);
				after.setBusiness(business);
				AdUser user = adUser("chosen-name");
				after.setUser(user);
				twoCallSetup(1L, before, after);
				when(activeDirectorySyncService.createAccount(eq(1L), any(), eq("chosen-name"))).thenReturn(user);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.password(ep("test"))
						.build();
				adQueueService.handleMessage(messageFor(request));

				verify(activeDirectorySyncService).createAccount(eq(1L), any(), eq("chosen-name"));
				verify(activeDirectorySyncService, never()).makeAdUserName(any());
			}

			@Test
			void shouldThrowRetryableExceptionWhenCustomStrategyHasNoPendingAccountName() {
				Business business = new Business();
				BusinessEmploymentSettings settings = new BusinessEmploymentSettings();
				settings.setNameStrategy(AccountNameStrategy.CUSTOM);
				business.setBusinessEmploymentSettings(settings);

				Employee before = basicEmployee(1L);
				before.setBusiness(business);
				before.setPendingAccountName(null);
				when(employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(1L)).thenReturn(before);

				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.CREATE)
						.employeeId(1L)
						.password(ep("test"))
						.build();

				assertThatThrownBy(() -> adQueueService.handleMessage(messageFor(request)))
						.isInstanceOf(RetryableException.class)
						.hasCauseInstanceOf(ADOperationException.class);

				verify(activeDirectorySyncService, never()).createAccount(any(), any(), any());
			}
		}

        @Nested
        class SetGroupMembershipOperation {
            @Test
            void shouldCallSetGroupMembershipAndConsumeMessage() {
                // Arrange
                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.SET_GROUP_MEMBERSHIP)
						.employeeId(1L)
						.groupId(5L)
						.build();

                // Act
                boolean result = adQueueService.handleMessage(messageFor(request));

                // Assert
                assertThat(result).isTrue();
                verify(activeDirectorySyncService).setGroupMembership(1L, 5L);
            }

            @Test
            void shouldThrowRetryableExceptionWhenAdOperationFails() {
                // Arrange
                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.SET_GROUP_MEMBERSHIP)
						.employeeId(1L)
						.groupId(5L)
						.build();
                doThrow(new ADOperationException("AD unreachable"))
                        .when(activeDirectorySyncService).setGroupMembership(1L, 5L);

                // Act + Assert
                assertThatThrownBy(() -> adQueueService.handleMessage(messageFor(request)))
                        .isInstanceOf(RetryableException.class)
                        .hasCauseInstanceOf(ADOperationException.class);
            }
        }

        @Nested
        class DeleteGroupMembershipOperation {
            @Test
            void shouldCallDeleteGroupMembershipAndConsumeMessage() {
                // Arrange
                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.DELETE_GROUP_MEMBER_SHIP)
						.employeeId(1L)
						.groupId(7L)
						.build();

                // Act
                boolean result = adQueueService.handleMessage(messageFor(request));

                // Assert
                assertThat(result).isTrue();
                verify(activeDirectorySyncService).deleteGroupMembership(1L, 7L);
            }

            @Test
            void shouldThrowRetryableExceptionWhenAdOperationFails() {
                // Arrange
                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.DELETE_GROUP_MEMBER_SHIP)
						.employeeId(1L)
						.groupId(7L)
						.build();
                doThrow(new ADOperationException("AD unreachable"))
                        .when(activeDirectorySyncService).deleteGroupMembership(1L, 7L);

                // Act + Assert
                assertThatThrownBy(() -> adQueueService.handleMessage(messageFor(request)))
                        .isInstanceOf(RetryableException.class)
                        .hasCauseInstanceOf(ADOperationException.class);
            }
        }

        @Nested
        class SetExpireDateOperation {
            @Test
            void shouldCallSetExpireDateAndConsumeMessage() {
                // Arrange
                LocalDate expiry = LocalDate.of(2026, 12, 31);
                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.SET_EXPIRE_DATE)
						.employeeId(1L)
						.date(expiry)
						.build();

                // Act
                boolean result = adQueueService.handleMessage(messageFor(request));

                // Assert
                assertThat(result).isTrue();
                verify(activeDirectorySyncService).setExpireDate(1L, expiry);
            }

            @Test
            void shouldThrowRetryableExceptionWhenAdOperationFails() {
                // Arrange
                LocalDate expiry = LocalDate.of(2026, 12, 31);
                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.SET_EXPIRE_DATE)
						.employeeId(1L)
						.date(expiry)
						.build();
                doThrow(new ADOperationException("AD unreachable"))
                        .when(activeDirectorySyncService).setExpireDate(1L, expiry);

                // Act + Assert
                assertThatThrownBy(() -> adQueueService.handleMessage(messageFor(request)))
                        .isInstanceOf(RetryableException.class)
                        .hasCauseInstanceOf(ADOperationException.class);
            }
        }

        @Nested
        class SetPasswordOperation {
            @Test
            void shouldCallSetPasswordAndConsumeMessage() {
                // Arrange
                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.SET_PASSWORD)
						.employeeId(1L)
						.password(ep("s3cr3t!"))
						.build();

                // Act
                boolean result = adQueueService.handleMessage(messageFor(request));

                // Assert
                assertThat(result).isTrue();
                verify(activeDirectorySyncService).setPassword(1L, "s3cr3t!", true);
            }

            @Test
            void shouldThrowRetryableExceptionWhenAdOperationFails() {
                // Arrange
                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.SET_PASSWORD)
						.employeeId(1L)
						.password(ep("s3cr3t!"))
						.build();
                doThrow(new ADOperationException("AD unreachable"))
                        .when(activeDirectorySyncService).setPassword(1L, "s3cr3t!", true);

                // Act + Assert
                assertThatThrownBy(() -> adQueueService.handleMessage(messageFor(request)))
                        .isInstanceOf(RetryableException.class)
                        .hasCauseInstanceOf(ADOperationException.class);
            }
        }

        @Nested
        class MalformedBody {
            @Test
            void shouldThrowRuntimeExceptionWhenBodyIsNotValidJson() {
                // Arrange
                QueueMessage message = QueueMessage.builder()
                        .queue("ACTIVE_DIRECTORY_QUEUE")
						.body("not-json")
						.build();

                // Act + Assert
                assertThatThrownBy(() -> adQueueService.handleMessage(message))
                        .isInstanceOf(RuntimeException.class);
            }
        }
    }

    @Nested
    class DoHandleFailedMessage {

        @Nested
        class SetGroupMembershipCompensation {
            @Test
            void shouldRemoveGroupFromEmployeeWhenGroupWasPresent() {
                // Arrange
                AdGroup group = adGroup(5L);
                Employee employee = basicEmployee(1L);
                employee.getAdGroups().add(group);

                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.SET_GROUP_MEMBERSHIP)
						.employeeId(1L)
						.groupId(5L)
						.build();
                when(employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(1L)).thenReturn(employee);
                when(adGroupService.findById(5L)).thenReturn(group);

                // Act
                // doHandleFailedMessage is called by the base class handleFailedMessage when retries are exhausted.
                QueueMessage msg = messageFor(request);
                msg.setHeaders(Map.of("x-attempt", "3")); // at/beyond maxAttempts
                adQueueService.handleFailedMessage(msg, new RuntimeException("perm failure"));

                // Assert
                assertThat(employee.getAdGroups()).doesNotContain(group);
                verify(employeeService).save(employee);
            }

            @Test
            void shouldSkipRemovalWhenGroupNotPresentOnEmployee() {
                // Arrange
                AdGroup group = adGroup(5L);
                Employee employee = basicEmployee(1L);
                // group is NOT in employee.adGroups

                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.SET_GROUP_MEMBERSHIP)
						.employeeId(1L)
						.groupId(5L)
						.build();
                when(employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(1L)).thenReturn(employee);
                when(adGroupService.findById(5L)).thenReturn(group);

                // Act
                QueueMessage msg = messageFor(request);
                msg.setHeaders(Map.of("x-attempt", "3"));
                adQueueService.handleFailedMessage(msg, new RuntimeException("perm failure"));

                // Assert — no save because nothing changed
                verify(employeeService, never()).save(any());
            }
        }

        @Nested
        class DeleteGroupMembershipCompensation {
            @Test
            void shouldReAddGroupToEmployeeWhenGroupWasMissing() {
                // Arrange
                AdGroup group = adGroup(7L);
                Employee employee = basicEmployee(1L);
                // group is NOT present (deletion failed, so we must re-add it)

                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.DELETE_GROUP_MEMBER_SHIP)
						.employeeId(1L)
						.groupId(7L)
						.build();
                when(employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(1L)).thenReturn(employee);
                when(adGroupService.findById(7L)).thenReturn(group);

                // Act
                QueueMessage msg = messageFor(request);
                msg.setHeaders(Map.of("x-attempt", "3"));
                adQueueService.handleFailedMessage(msg, new RuntimeException("perm failure"));

                // Assert
                assertThat(employee.getAdGroups()).contains(group);
                verify(employeeService).save(employee);
            }

            @Test
            void shouldSkipReAddWhenGroupAlreadyPresentOnEmployee() {
                // Arrange
                AdGroup group = adGroup(7L);
                Employee employee = basicEmployee(1L);
                employee.getAdGroups().add(group); // already there

                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.DELETE_GROUP_MEMBER_SHIP)
						.employeeId(1L)
						.groupId(7L)
						.build();
                when(employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(1L)).thenReturn(employee);
                when(adGroupService.findById(7L)).thenReturn(group);

                // Act
                QueueMessage msg = messageFor(request);
                msg.setHeaders(Map.of("x-attempt", "3"));
                adQueueService.handleFailedMessage(msg, new RuntimeException("perm failure"));

                // Assert
                verify(employeeService, never()).save(any());
            }
        }

		@Nested
		class SetExpireDateCompensation {
			@Test
			void shouldNotCompensateAndReturnDeadLetterForSetExpireDate() {
				Employee employee = basicEmployee(1L);
				ADMessageRequest request = ADMessageRequest.builder()
						.operation(OperationType.SET_EXPIRE_DATE)
						.employeeId(1L)
						.date(LocalDate.of(2026, 12, 31))
						.build();
				when(employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(1L)).thenReturn(employee);

				QueueMessage msg = messageFor(request);
				msg.setHeaders(Map.of("x-attempt", "3"));
				boolean result = adQueueService.handleFailedMessage(msg, new RuntimeException("perm failure"));

				assertThat(result).isFalse();
				verify(employeeService, never()).save(any());
			}
		}

        @Nested
        class WhenEmployeeNotFound {
            @Test
            void shouldReturnDeadLetterWhenEmployeeGoneOnCompensation() {
                // Arrange
                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.SET_GROUP_MEMBERSHIP)
						.employeeId(99L)
						.groupId(5L)
						.build();
                when(employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(99L)).thenReturn(null);

                // Act
                QueueMessage msg = messageFor(request);
                msg.setHeaders(Map.of("x-attempt", "3"));
                boolean result = adQueueService.handleFailedMessage(msg, new RuntimeException("perm failure"));

                // Assert
                assertThat(result).isFalse();
            }
        }

        @Nested
        class CreateAndSetPasswordOperations {
            @Test
            void shouldNotCompensateAndReturnDeadLetterForCreate() {
                // No meaningful rollback exists for AD account creation
                Employee employee = basicEmployee(1L);
                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.CREATE)
						.employeeId(1L)
						.build();
                when(employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(1L)).thenReturn(employee);

                QueueMessage msg = messageFor(request);
                msg.setHeaders(Map.of("x-attempt", "3"));
                boolean result = adQueueService.handleFailedMessage(msg, new RuntimeException("perm failure"));

                assertThat(result).isFalse();
                verify(employeeService, never()).save(any());
            }

            @Test
            void shouldNotCompensateAndReturnDeadLetterForSetPassword() {
                Employee employee = basicEmployee(1L);
                ADMessageRequest request = ADMessageRequest.builder()
                        .operation(OperationType.SET_PASSWORD)
						.employeeId(1L)
						.password("x")
						.build();
                when(employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(1L)).thenReturn(employee);

                QueueMessage msg = messageFor(request);
                msg.setHeaders(Map.of("x-attempt", "3"));
                boolean result = adQueueService.handleFailedMessage(msg, new RuntimeException("perm failure"));

                assertThat(result).isFalse();
                verify(employeeService, never()).save(any());
            }
        }
    }

    @Nested
    class RetryBehaviour {
        @Test
        void shouldRequeueWithIncrementedAttemptOnRetryableFailure() {
            // Arrange
            ADMessageRequest request = ADMessageRequest.builder()
                    .operation(OperationType.SET_GROUP_MEMBERSHIP)
					.employeeId(1L)
					.groupId(5L)
					.build();

            QueueMessage msg = messageFor(request);
            msg.setHeaders(Map.of("x-attempt", "0"));

            // Act
            boolean result = adQueueService.handleFailedMessage(msg, new RetryableException("retry", new ADOperationException("timeout")));

            // Assert — requeued, not dead-lettered
            assertThat(result).isTrue();
            ArgumentCaptor<QueueMessage> captor = ArgumentCaptor.forClass(QueueMessage.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().getHeaders()).containsEntry("x-attempt", "1");
        }

        @Test
        void shouldDeadLetterWhenMaxAttemptsExceeded() {
            // Arrange
            Employee employee = basicEmployee(1L);
            ADMessageRequest request = ADMessageRequest.builder()
                    .operation(OperationType.SET_GROUP_MEMBERSHIP)
					.employeeId(1L)
					.groupId(5L)
					.build();
            when(employeeService.findByIdWithUserRolesAndAdGroupsAndRoleGroups(1L)).thenReturn(employee);
            when(adGroupService.findById(5L)).thenReturn(adGroup(5L));

            QueueMessage msg = messageFor(request);
            // x-attempt == maxAttempts (3) → exceeded
            msg.setHeaders(Map.of("x-attempt", "3"));

            // Act
            boolean result = adQueueService.handleFailedMessage(msg, new RetryableException("retry", new ADOperationException("still failing")));

            // Assert — compensation ran, dead-lettered
            assertThat(result).isFalse();
        }
    }
}
