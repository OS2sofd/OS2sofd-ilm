package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.dao.AuditLogDao;
import dk.digitalidentity.OS2ilm.model.entity.AuditLog;
import dk.digitalidentity.OS2ilm.model.entity.AuditLogDetail;
import dk.digitalidentity.OS2ilm.model.entity.enums.DetailType;
import dk.digitalidentity.OS2ilm.model.entity.enums.LogAction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuditLogService {
    private final AuditLogDao auditLogDao;

    public AuditLog findById(Long id) {
        return auditLogDao.getById(id);
    }

	public AuditLog findByIdWithDetails(long id) {
		return auditLogDao.findByIdWithDetails(id);
	}

    public void addFilesToZipCsv(ZipOutputStream zipOutputStream, String fileNamePrefix, String mode) {
        LocalDateTime from = null;
        LocalDateTime to = LocalDateTime.now();
        try {
            List<AuditLog> auditlogs;
            if (mode.equals("year")) {
                from = LocalDateTime.now().minusYears(1);
                auditlogs = findFromLastYear();
            }
            else {
                from = LocalDateTime.now().minusMonths(1);
                auditlogs = findFromLastMonth();
            }
            
            log.debug("Starting rendering log from " + from + " to " + to);

            if (!auditlogs.isEmpty()) {
                String fileName = fileNamePrefix + " " + from + "-" + to + ".csv";
                ZipEntry taskfile = new ZipEntry(fileName);
                zipOutputStream.putNextEntry(taskfile);

                StringBuilder headerBuilder = new StringBuilder();
                headerBuilder.append("\"").append("Tidspunkt").append("\";");
                headerBuilder.append("\"").append("IP-adresse").append("\";");
                headerBuilder.append("\"").append("Korrelations-ID").append("\";");
                headerBuilder.append("\"").append("Handling").append("\";");
                headerBuilder.append("\"").append("Besked").append("\";");
                headerBuilder.append("\"").append("Aktør").append("\";");
                headerBuilder.append("\"").append("Bruger").append("\"\n");
                zipOutputStream.write(headerBuilder.toString().getBytes(StandardCharsets.UTF_8));

                for (AuditLog auditlog : auditlogs) {
                    StringBuilder rowBuilder = new StringBuilder();
                    rowBuilder.append("\"").append(auditlog.getTts() == null ? "" : auditlog.getTts()).append("\";");
                    rowBuilder.append("\"").append(auditlog.getIpAddress() == null ? "" : auditlog.getIpAddress()).append("\";");
                    rowBuilder.append("\"").append(auditlog.getCorrelationId() == null ? "" : auditlog.getCorrelationId()).append("\";");
                    rowBuilder.append("\"").append(auditlog.getLogAction() == null ? "" : auditlog.getLogAction().getType().getTranslation()).append("\";");
                    rowBuilder.append("\"").append(auditlog.getMessage() == null ? "" : auditlog.getMessage()).append("\";");
                    rowBuilder.append("\"").append(auditlog.getPerformerName() == null ? "" : auditlog.getPerformerName()).append("\";");
                    rowBuilder.append("\"").append(auditlog.getTargetName() == null ? "" : auditlog.getTargetName()).append("\";");
                    zipOutputStream.write(rowBuilder.toString().getBytes(StandardCharsets.UTF_8));
                }
                zipOutputStream.closeEntry();
            }

            log.debug("Done rendering logs from " + from + " to " + to);
        } catch (Exception ex) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String formattedFrom = from.format(formatter);
            String formattedTo = to.format(formatter);

            log.warn("Failed to add file for reports from " + formattedFrom + " to " + formattedTo + " to zip. Error code: " + ex);
        }

        log.debug("Done rendering logs for download");
    }

    public List<AuditLog> findFromLastYear() {
        return auditLogDao.findAllByTtsBetween(LocalDateTime.now().minusYears(1), LocalDateTime.now());
    }

    public List<AuditLog> findFromLastMonth() {
        return auditLogDao.findAllByTtsBetween(LocalDateTime.now().minusMonths(1), LocalDateTime.now());
    }

    public List<AuditLog> findAll() {
        return auditLogDao.findAll();
    }

    public List<AuditLog> findByTtsBefore(LocalDateTime dateTime) {
        return auditLogDao.findByTtsBefore(dateTime);
    }

    public void logSystemAction(Long targetId, String targetName, LogAction logAction, String message, Object target) {
        internalLog(null, "SYSTEM", targetId, targetName, logAction, message, target);
    }

    public void log(String performerId, String performerName, Long targetId, String targetName, LogAction logAction, String message, Object target) {
        internalLog(performerId, performerName, targetId, targetName, logAction, message, target);
    }

    private void internalLog(String performerId, String performerName, Long targetId, String targetName, LogAction logAction, String message, Object target) {
        AuditLog auditLog = new AuditLog();
        auditLog.setCorrelationId(getCorrelationId());
        auditLog.setIpAddress(getIpAddress());

        if (target != null) {
            try {
                AuditLogDetail detail = new AuditLogDetail();
                detail.setDetailContent(objectToJson(target));
                detail.setDetailType(DetailType.JSON);
                auditLog.setDetails(detail);
            } catch(JacksonException e) {
                var errorDetail = new AuditLogDetail();
                errorDetail.setDetailType(DetailType.JSON);
                errorDetail.setDetailContent("{\"error\":\"Failed to convert data to json due to: " + e.getMessage() + "\"}");
                auditLog.setDetails(errorDetail);
            }
        }
        
        if (performerId == null) {
            auditLog.setPerformerName("SYSTEM");
        }
        else {
            auditLog.setPerformerName(performerName);
        }
        
        auditLog.setMessage(message);
        auditLog.setLogAction(logAction);
        auditLog.setTargetId(targetId);
        auditLog.setTargetName(targetName);
        auditLog.setPerformerId(String.valueOf(performerId));

        auditLogDao.save(auditLog);
    }

//    @Transactional
//    public void cleanUpOldAuditLogs() {
//        int interval = configuration.getScheduled().getCleanUpInterval();
//
//        List<AuditLog> auditLogs = findByTtsBefore(LocalDateTime.now().minusMonths(interval));
//        auditLogDao.deleteByTtsBefore(LocalDateTime.now().minusMonths(interval));
//        auditLogDao.deleteUnreferencedAuditlogDetails();
//
//        log(null, "", null, "SystemDB", LogAction.AUDITLOG_CLEANUP, "The automated task for cleanup has run", auditLogs);
//
//        log.debug("Deleting {} auditlogs", auditLogs.size());
//    }
//
    private static String getIpAddress() {
        String remoteAddr = "";

        HttpServletRequest request = getRequest();
        if (request != null) {
            remoteAddr = request.getHeader("X-FORWARDED-FOR");
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }

        return remoteAddr;
    }

    private static HttpServletRequest getRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        }
        catch (IllegalStateException ex) {
            return null;
        }
    }

    private String getCorrelationId() {
        try {
            String sessionID = RequestContextHolder.currentRequestAttributes().getSessionId();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(sessionID.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(encodedHash);
        }
        catch (Exception ex) {
            return "SYSTEM-" + UUID.randomUUID().toString();
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);

        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);

            if (hex.length() == 1) {
                hexString.append('0');
            }

            hexString.append(hex);
        }

        return hexString.toString();
    }

    private String objectToJson(Object object) throws JacksonException {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    public List<AuditLog> findByCorrelationId(String id) {
        return auditLogDao.findByCorrelationId(id);
    }
}
