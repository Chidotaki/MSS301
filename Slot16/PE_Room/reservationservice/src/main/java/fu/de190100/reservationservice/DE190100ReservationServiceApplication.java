package fu.de190100.reservationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DE190100ReservationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                DE190100ReservationServiceApplication.class,
                args
        );
    }
}