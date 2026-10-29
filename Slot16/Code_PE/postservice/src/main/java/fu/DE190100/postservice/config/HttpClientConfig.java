package fu.DE190100.postservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.util.TimeZone;

@Configuration
public class HttpClientConfig {

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
                .defaultTimeZone(TimeZone.getDefault())
                .build();
    }
}
