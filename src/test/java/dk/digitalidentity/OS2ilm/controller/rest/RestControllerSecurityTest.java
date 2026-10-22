package dk.digitalidentity.OS2ilm.controller.rest;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.OS2ilm.TestContainerConfiguration;

// Tests to ensure that our RestControllers are protected with the right annotations
@AutoConfigureMockMvc
@Import(TestContainerConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void failsWhenUnsecure() throws Exception {
        assertUrlProtected("/rest/admin");
        assertUrlProtected("/rest/approval");
        assertUrlProtected("/rest/approve");
        assertUrlProtected("/rest/deny");
        assertUrlProtected("/rest/archetype");
        assertUrlProtected("/rest/business");
        assertUrlProtected("/rest/employee");
        assertUrlProtected("/rest/search");
        assertUrlProtected("/rest/profile");
        assertUrlProtected("/rest/register");
        assertUrlProtected("/rest/leader");
    }

    @Test
    void isMissingAnnotation() {
        Reflections reflections = new Reflections("dk.digitalidentity");
        Set<Class<?>> restControllers = reflections.getTypesAnnotatedWith(RestController.class);
        List<String> classesMissingAnnotations = new ArrayList<>();
        for (Class<?> restController : restControllers) {
            // Apis have their own security mechanisms, we don't check those here
            if (restController.getSimpleName().contains("Api")) {
                continue;
            }
            boolean match = false;
            for (Annotation annotation : restController.getAnnotations()) {
                if (annotation.annotationType().getSimpleName().startsWith("Require") || annotation.annotationType().getSimpleName().equals("NoRoleRequired")) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                classesMissingAnnotations.add(restController.getSimpleName());
            }
        }
        assertTrue(classesMissingAnnotations.isEmpty(), "The following classes are missing security annotations: " + classesMissingAnnotations);
    }

    // The expectation is that we should be redirected to login
    public void assertUrlProtected(final String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/discovery"));
    }
}
