package debezium;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAspectJAutoProxy
@EnableScheduling
@SpringBootApplication
@EnableKafkaStreams
public class DebeziumDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DebeziumDemoApplication.class, args);
    }

}