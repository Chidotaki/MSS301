package fu.DE190100.postservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DE190100PostServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                DE190100PostServiceApplication.class,
                args
        );
    }
}