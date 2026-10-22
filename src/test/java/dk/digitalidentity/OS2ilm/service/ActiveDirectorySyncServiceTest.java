package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.config.settings.IlmConfiguration;
import dk.digitalidentity.OS2ilm.exception.ADOperationException;
import dk.digitalidentity.OS2ilm.model.entity.AdUser;
import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.model.entity.EmailTemplate;
import dk.digitalidentity.framework.ad.service.ActiveDirectoryService;
import dk.digitalidentity.framework.ad.service.model.SetAttributeRequest;
import dk.digitalidentity.framework.ad.service.model.SetAttributeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActiveDirectorySyncServiceTest {

	@Mock private ActiveDirectoryService activeDirectoryService;
	@Mock private AdGroupService adGroupService;
	@Mock private AdUserService adUserService;
	@Mock(answer = Answers.RETURNS_DEEP_STUBS) private IlmConfiguration configuration;
	@Mock private EmployeeService employeeService;
	@Mock private EmailTemplateService emailTemplateService;
	@Mock private MessageService messageService;
	@Mock private ActivationTimeFrameService activationTimeFrameService;
	@Mock private AccountNameValidator accountNameValidator;
	@Mock private BusinessService businessService;

	private ActiveDirectorySyncService service;
	private AdUser adUser;

	@BeforeEach
	void setUp() {
		service = new ActiveDirectorySyncService(activeDirectoryService, adGroupService, adUserService,
				configuration, employeeService, emailTemplateService, messageService,
				activationTimeFrameService, accountNameValidator, businessService);

		EmailTemplate disabledTemplate = new EmailTemplate();
		disabledTemplate.setEnabled(false);
		lenient().when(emailTemplateService.getByType(any())).thenReturn(disabledTemplate);

		adUser = new AdUser();
		adUser.setName("ILM-TST-0001");

		Employee employee = new Employee();
		employee.setId(1L);
		employee.setName("Test Employee");
		employee.setUser(adUser);

		lenient().when(employeeService.findById(1L)).thenReturn(employee);
		lenient().when(configuration.getActiveDirectoryConfig().getCprAttribute()).thenReturn("employeeNumber");
	}

	private void withDash(boolean withDash) {
		when(configuration.getActiveDirectoryConfig().isCprWithDash()).thenReturn(withDash);
	}

	private void adCallSucceeds() {
		SetAttributeResponse response = mock(SetAttributeResponse.class);
		when(response.isSuccess()).thenReturn(true);
		when(activeDirectoryService.setAttribute(any())).thenReturn(response);
	}

	private String valueSentToAd() {
		ArgumentCaptor<SetAttributeRequest> captor = ArgumentCaptor.forClass(SetAttributeRequest.class);
		verify(activeDirectoryService).setAttribute(captor.capture());
		return captor.getValue().getValue();
	}

	@Nested
	class CprFormatting {

		@Test
		void sendsDashedValueToAdAndStoresItNormalized() {
			withDash(true);
			adCallSucceeds();

			service.setCprAttribute(1L, "0101901234");

			assertThat(valueSentToAd()).isEqualTo("010190-1234");
			assertThat(adUser.getCpr()).isEqualTo("0101901234");
		}

		@Test
		void leavesValueUntouchedWhenDashIsNotRequested() {
			withDash(false);
			adCallSucceeds();

			service.setCprAttribute(1L, "010190-1234");

			assertThat(valueSentToAd()).isEqualTo("010190-1234");
			assertThat(adUser.getCpr()).isEqualTo("0101901234");
		}

		@Test
		void storesNormalizedValueRegardlessOfTheShapeItCameIn() {
			withDash(true);
			adCallSucceeds();

			service.setCprAttribute(1L, " 010190 1234 ");

			assertThat(valueSentToAd()).isEqualTo("010190-1234");
			assertThat(adUser.getCpr()).isEqualTo("0101901234");
		}

		@Test
		void handlesCprContainingLetters() {
			withDash(true);
			adCallSucceeds();

			service.setCprAttribute(1L, "01019012AB");

			assertThat(valueSentToAd()).isEqualTo("010190-12AB");
			assertThat(adUser.getCpr()).isEqualTo("01019012AB");
		}

		@Test
		void writesCprThatIsNotTenCharactersUnchangedRatherThanSkippingIt() {
			withDash(true);
			adCallSucceeds();

			service.setCprAttribute(1L, "123456789");

			assertThat(valueSentToAd()).isEqualTo("123456789");
			assertThat(adUser.getCpr()).isEqualTo("123456789");
		}

		@Test
		void writesTargetAttributeFromConfiguration() {
			withDash(false);
			adCallSucceeds();

			service.setCprAttribute(1L, "0101901234");

			ArgumentCaptor<SetAttributeRequest> captor = ArgumentCaptor.forClass(SetAttributeRequest.class);
			verify(activeDirectoryService).setAttribute(captor.capture());
			assertThat(captor.getValue().getField()).isEqualTo("employeeNumber");
			assertThat(captor.getValue().getUserId()).isEqualTo("ILM-TST-0001");
		}
	}

	@Nested
	class WhenNothingShouldBeWritten {

		@Test
		void skipsWhenNoCprAttributeIsConfigured() {
			when(configuration.getActiveDirectoryConfig().getCprAttribute()).thenReturn("");

			service.setCprAttribute(1L, "0101901234");

			verifyNoInteractions(activeDirectoryService);
			assertThat(adUser.getCpr()).isNull();
		}

		@Test
		void skipsWhenTheEmployeeHasNoCpr() {
			service.setCprAttribute(1L, "  ");

			verifyNoInteractions(activeDirectoryService);
			assertThat(adUser.getCpr()).isNull();
		}

		@Test
		void doesNotStoreCprWhenTheAdCallFails() {
			withDash(true);
			SetAttributeResponse response = mock(SetAttributeResponse.class);
			when(response.isSuccess()).thenReturn(false);
			when(activeDirectoryService.setAttribute(any())).thenReturn(response);

			assertThatThrownBy(() -> service.setCprAttribute(1L, "0101901234"))
					.isInstanceOf(ADOperationException.class);

			assertThat(adUser.getCpr()).isNull();
			verify(adUserService, never()).save(any());
		}
	}
}
