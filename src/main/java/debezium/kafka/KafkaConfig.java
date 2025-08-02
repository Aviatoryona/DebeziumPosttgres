package debezium.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic createCustomersTopic() {
        return TopicBuilder.name("debezium_master.public.customers")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic createInvoicesStage() {
        return TopicBuilder.name("debezium_master.public.invoices")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic createProcessedInvoicesTopic() {
        return TopicBuilder.name("processed_invoices_topic")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
