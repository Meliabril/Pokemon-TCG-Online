package ar.edu.utn.frc.tup.piii.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI openApi(
            @Value("${app.name}") String appName,
            @Value("${app.description}") String appDescription,
            @Value("${app.version}") String appVersion,
            @Value("${app.url}") String appUrl,
            @Value("${app.dev-name}") String developerName,
            @Value("${app.dev-email}") String developerEmail) {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title(appName)
                        .version(appVersion)
                        .description(appDescription)
                        .contact(new Contact()
                                .name(developerName)
                                .email(developerEmail)))
                .addServersItem(new Server()
                        .url(appUrl)
                        .description("Local environment"));
    }
}
