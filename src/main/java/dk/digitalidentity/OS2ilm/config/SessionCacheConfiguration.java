package dk.digitalidentity.OS2ilm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.MySqlJdbcIndexedSessionRepositoryCustomizer;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
// Overwrites server.servlet.session.timeout in default.properties so we have to set interval here
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 4 * 60 * 60) // 4 hours
public class SessionCacheConfiguration {

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("JSESSIONID");
        serializer.setCookiePath("/");
        serializer.setUseSecureCookie(true);
        serializer.setSameSite("None");
        serializer.setCookieMaxAge(4 * 60 * 60); // 4 hours

        return serializer;
    }

    @Bean
    // Spring Session JDBC optimizations for MySQL
    public MySqlJdbcIndexedSessionRepositoryCustomizer sessionRepositoryCustomizer() {
        return new MySqlJdbcIndexedSessionRepositoryCustomizer();
    }
}
