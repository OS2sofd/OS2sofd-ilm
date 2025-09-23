package dk.digitalidentity.OS2ilm.controller.rest;

import dk.digitalidentity.OS2ilm.model.datatable.dao.AuditLogDatatableDao;
import dk.digitalidentity.OS2ilm.model.entity.ActivationTimeFrame;
import dk.digitalidentity.OS2ilm.model.entity.OUTreeForm;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import dk.digitalidentity.OS2ilm.model.role.RequireReadOnlyAdministrator;
import dk.digitalidentity.OS2ilm.model.role.RequireSystemAdministrator;
import dk.digitalidentity.OS2ilm.model.view.AuditLogView;
import dk.digitalidentity.OS2ilm.security.SecurityUtil;
import dk.digitalidentity.OS2ilm.service.ActivationTimeFrameService;
import dk.digitalidentity.OS2ilm.service.AuditLogService;
import dk.digitalidentity.OS2ilm.service.OrgUnitService;
import dk.digitalidentity.OS2ilm.service.cms.CmsMessageBundle;
import dk.digitalidentity.OS2ilm.service.cms.CmsMessageService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@Slf4j
@Hidden
@RequestMapping("/rest/admin")
@RestController
@RequiredArgsConstructor
@RequireReadOnlyAdministrator
public class AdminRestController {
	public static final String CMS_EDIT_PAGE = "admin/cms/edit";
	private final AuditLogService auditLogService;
	private final CmsMessageService cmsMessageService;
	private final CmsMessageBundle cmsMessageBundle;
	private final AuditLogDatatableDao auditLogDatatableDao;
	private final OrgUnitService orgUnitService;
	private final ActivationTimeFrameService activationTimeFrameService;
	private final SecurityUtil securityUtil;

	@PostMapping("/download/log/yearly")
	public ResponseEntity<StreamingResponseBody> downloadLogYear(HttpServletRequest request, HttpServletResponse response) {
		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"AuditLog.zip\"").body(out -> {
			var zipOutputStream = new ZipOutputStream(out);
			// Call toCSV here
			auditLogService.addFilesToZipCsv(zipOutputStream, "auditlog", "year");
			zipOutputStream.close();
		});
	}

	@PostMapping("/download/log/monthly")
	public ResponseEntity<StreamingResponseBody> downloadLogMonth(HttpServletRequest request, HttpServletResponse response) {
		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"AuditLog.zip\"").body(out -> {
			var zipOutputStream = new ZipOutputStream(out);
			// Call toCSV here
			auditLogService.addFilesToZipCsv(zipOutputStream, "auditlog", "month");
			zipOutputStream.close();
		});
	}

	record CmsMessage(String key, String value, String description) {}

	@RequireSystemAdministrator
	@GetMapping("/admin/cms-edit")
	public String editCms(Model model, @RequestParam("key") String key) {
		CmsMessage dto = new CmsMessage(key, cmsMessageBundle.getText(key, true), cmsMessageBundle.getDescription(key));
		populateModel(model, dto, false);

		return CMS_EDIT_PAGE;
	}

	@RequireSystemAdministrator
	@PostMapping("/cms-edit")
	public String saveCms(Model model, CmsMessage cmsMessage) {
		if (cmsMessage.value != null && cmsMessage.value.length() > 65536) {
			CmsMessage dto = new CmsMessage(cmsMessage.key, cmsMessage.value, cmsMessageBundle.getDescription(cmsMessage.key));
			populateModel(model, dto, true);

			return CMS_EDIT_PAGE;
		}

		PolicyFactory policy = new HtmlPolicyBuilder()
				.allowCommonBlockElements()
				.allowCommonInlineFormattingElements()
				.allowElements("a")
				.allowUrlProtocols("https")
				.allowAttributes("href", "target").onElements("a")
				.allowAttributes("class", "style", "id", "name").globally()
				.toFactory();
		String safeHTML = policy.sanitize(cmsMessage.value);

		dk.digitalidentity.OS2ilm.model.entity.CmsMessage cms = cmsMessageService.getByCmsKey(cmsMessage.key);

		if (cms == null) {
			cms = new dk.digitalidentity.OS2ilm.model.entity.CmsMessage();
			cms.setCmsKey(cmsMessage.key);
		}

		cms.setCmsValue(safeHTML);
		cms.setLastUpdated(LocalDateTime.now());
		cmsMessageService.save(cms);

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), null, "", LogAction.ADDED_CMS_MESSAGE, "CMS besked tilføjet!", cmsMessage);

		return "redirect:/admin/cms";
	}

	@RequireSystemAdministrator
	@PostMapping("/cms-edit/logo")
	public String saveCms(@RequestParam("value") MultipartFile file, @RequestParam("key") String key, Model model) throws IOException {
		String base64 = Base64.getEncoder().encodeToString(file.getBytes());

		dk.digitalidentity.OS2ilm.model.entity.CmsMessage logo = cmsMessageService.getByCmsKey("cms.logo");

		if (base64.length() > 65536) {
			CmsMessage dto = new CmsMessage(logo.getCmsKey(), null, cmsMessageBundle.getDescription(logo.getCmsKey()));
			populateModel(model, dto, true);

			return CMS_EDIT_PAGE;
		}

		if (logo == null) {
			logo = new dk.digitalidentity.OS2ilm.model.entity.CmsMessage();
			logo.setCmsKey("cms.logo");
		}

		logo.setCmsValue(base64);
		logo.setLastUpdated(LocalDateTime.now());
		cmsMessageService.save(logo);

		return "redirect:/admin/cms";
	}

	private static void populateModel(Model model, CmsMessage dto, boolean showErrors) {
		model.addAttribute("cmsMessage", dto);
		model.addAttribute("showError", showErrors);
	}

	@RequireSystemAdministrator
	@PostMapping("/cms/delete/{id}")
	public ResponseEntity<String> deleteCms(@PathVariable String id) {
		dk.digitalidentity.OS2ilm.model.entity.CmsMessage cms = cmsMessageService.getByCmsKey(id);

		if (cms != null) {
			// Instead of deleting we set it to null as it is easier to update across servers
			cms.setCmsValue(null);
			cms.setLastUpdated(LocalDateTime.now());
			auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), null, "", LogAction.DELETED_CMS_MESSAGE, "CMS besked slettet", cms);
			cmsMessageService.save(cms);
		}

		return ResponseEntity.ok().build();
	}

	public record AuditLogViewDTO(long id, LocalDateTime tts, String targetId, String targetName, String performerId, String performerName, String logAction, String message, Long detailId) {}

	@PostMapping("/auditlog/view")
	public DataTablesOutput<AuditLogViewDTO> paginatingTable(@RequestBody DataTablesInput input) {
		DataTablesOutput<AuditLogView> all = auditLogDatatableDao.findAll(input);
		return convertAuditLogDataTablesModelToDTO(all);
	}

	private DataTablesOutput<AuditLogViewDTO> convertAuditLogDataTablesModelToDTO(DataTablesOutput<AuditLogView> output) {
		List<AuditLogViewDTO> dataWithMessages = output.getData().stream()
				.map(auditlog -> new AuditLogViewDTO(auditlog.getId(), auditlog.getTts(), auditlog.getTargetId(), auditlog.getTargetName(), auditlog.getPerformerId(), auditlog.getPerformerName(), auditlog.getLogAction().getMessage(), auditlog.getMessage(), auditlog.getDetailId()))
				.collect(Collectors.toCollection(ArrayList::new));

		DataTablesOutput<AuditLogViewDTO> result = new DataTablesOutput<>();
		result.setData(dataWithMessages);
		result.setDraw(output.getDraw());
		result.setError(output.getError());
		result.setRecordsFiltered(output.getRecordsFiltered());
		result.setRecordsTotal(output.getRecordsTotal());

		return result;
	}

	@RequireSystemAdministrator
	@PostMapping("/orgUnits/tree")
	public List<OUTreeForm> getOrgUnitTree() {
		return orgUnitService.getAllTree();
	}

	@RequireSystemAdministrator
	public record CreateActivation(LocalTime startTime, LocalTime stopTime, boolean excludeWeekends, String name) {}
	@PostMapping("/activation/create")
	public ResponseEntity<?> createActivation(@RequestBody CreateActivation activationRecord) {
		ActivationTimeFrame activation = new ActivationTimeFrame();
		activation.setName(activationRecord.name);
		activation.setStartTime(activationRecord.startTime);
		activation.setStopTime(activationRecord.stopTime);
		activation.setExcludeWeekends(activationRecord.excludeWeekends);

		activationTimeFrameService.save(activation);

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), null, "", LogAction.ACTIVATION_TIME_FRAME_CREATED, "Aktiveringsperiode blev oprettet", activationRecord);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireSystemAdministrator
	@PostMapping("/activation/edit/{id}")
	public ResponseEntity<?> editActivation(@PathVariable long id, @RequestBody CreateActivation activationRecord) {
		ActivationTimeFrame activation = activationTimeFrameService.findById(id);
		if (activation == null) {
			log.warn("Activation with id {} not found", id);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		log.info(activationRecord.excludeWeekends + "");

		activation.setName(activationRecord.name);
		activation.setStartTime(activationRecord.startTime);
		activation.setStopTime(activationRecord.stopTime);
		activation.setExcludeWeekends(activationRecord.excludeWeekends);

		activationTimeFrameService.save(activation);

		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), null, "", LogAction.ACTIVATION_TIME_FRAME_UPDATED, "Aktiveringsperiode blev opdateret", activationRecord);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireSystemAdministrator
	record ActivationRecord(String name) {}
	@DeleteMapping("/activation/delete/{id}")
	public ResponseEntity<?> deleteActivation(@PathVariable long id) {
		ActivationTimeFrame activation = activationTimeFrameService.findById(id);
		if (activation == null) {
			log.warn("Activation with id {} not found", id);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		ActivationRecord record = new ActivationRecord(activation.getName());

		activationTimeFrameService.delete(activation);
		auditLogService.log(securityUtil.getAdmin().getUuid(), securityUtil.getName(), id, record.name, LogAction.ACTIVATION_TIME_FRAME_DELETED, "Aktiveringsperiode blev slettet", record);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
