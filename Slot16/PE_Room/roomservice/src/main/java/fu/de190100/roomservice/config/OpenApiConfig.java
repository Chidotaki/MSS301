package fu.de190100.roomservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI roomServiceOpenApi() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Room Service API")
                                .version("1.0.0")
                                .description(
                                        "MSS301 SU26 PE - Room Service"
                                )
                );
    }
}