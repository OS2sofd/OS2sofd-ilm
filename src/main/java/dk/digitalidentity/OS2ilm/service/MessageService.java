package dk.digitalidentity.OS2ilm.service;

import dk.digitalidentity.OS2ilm.model.entity.Employee;
import dk.digitalidentity.OS2ilm.service.eboks.EboksService;
import dk.digitalidentity.emailService.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {
	private final EmailService emailService;
	private final EboksService eboksService;

	public void sendNotification(Employee employee, String subject, String message) {
		if (employee.getCpr() != null) {
			eboksService.addMessageToQueue(new EboksService.EboksMessageRequest(subject, message, employee.getCpr()));
			return;
		}
		else if (employee.getEmail() != null) {
			emailService.queueEmail(employee.getEmail(), subject, message);
			return;
		}
		log.warn("No form of communication enabled for employee: {}", employee.getName());
	}

	public void sendNotificationUsingMail(String mail, String subject, String message) {
		if (StringUtils.hasText(mail) && StringUtils.hasText(subject) && StringUtils.hasText(message) ) {
			log.debug("Sending email for mail: {}", mail);
			emailService.queueEmail(mail, subject, message);
		}
	}
}
