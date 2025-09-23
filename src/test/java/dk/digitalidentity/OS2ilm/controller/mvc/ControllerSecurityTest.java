package dk.digitalidentity.OS2ilm.controller.mvc;

import dk.digitalidentity.OS2ilm.TestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tests to ensure that our MVC controllers are protected with the right annotations
@AutoConfigureMockMvc
@Import(TestContainerConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void failsWhenUnsecure() throws Exception {
        assertUrlProtected("/admin");
        assertUrlProtected("/approval");
        assertUrlProtected("/auditlog");
    }

    @Test
    void isMissingAnnotation() {
        Reflections reflections = new Reflections("dk.digitalidentity");
        Set<Class<?>> controllers = reflections.getTypesAnnotatedWith(Controller.class);
        List<String> classesMissingAnnotations = new ArrayList<>();
        for (Class<?> controller : controllers) {
            boolean match = false;
            boolean noRoleRequired = false;
            for (Annotation annotation : controller.getAnnotations()) {
				if (Objects.equals(annotation.annotationType().getSimpleName(), "NoRoleRequired")) {
					noRoleRequired = true;
                    break;
				}

                if (annotation.annotationType().getSimpleName().startsWith("Require")) {
                    match = true;
                }
            }
            if (!noRoleRequired && !match) {
                classesMissingAnnotations.add(controller.getSimpleName());
            }
        }
        assertTrue(classesMissingAnnotations.isEmpty(), "The following classes are missing security annotations: " + classesMissingAnnotations);
    }

    // The expectation is that we should be redirected to login
    public void assertUrlProtected(final String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/discovery"));
    }
}
