package dk.digitalidentity.OS2ilm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "dk.digitalidentity")
public class OS2IlmApplication {

	public static void main(String[] args) {
		SpringApplication.run(OS2IlmApplication.class, args);
	}

}
