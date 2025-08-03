package debezium.kafka;

import debezium.enums.DebeziumTopic;
import debezium.enums.KTopic;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Configuration
public class KafkaConfig {

    // The number of CPU cores available to the JVM, used for configuring Kafka's thread pool.
    private final static int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /**
     * Creates a KafkaAdmin bean to manage Kafka topics.
     * This bean is used to create and manage Kafka topics programmatically.
     *
     * @param properties Kafka properties for configuration.
     * @return KafkaAdmin instance configured with the provided properties.
     */
    @Bean
    public KafkaAdmin kafkaAdmin(KafkaProperties properties) {
        Map<String, Object> configs = new HashMap<>(properties.buildAdminProperties(null));
        return new KafkaAdmin(configs);
    }

    /**
     * Registers Kafka topics defined in the DebeziumTopic enum and KTopic enum.
     * This method combines the topic names from both enums and registers them as beans.
     *
     * @return BeanFactoryPostProcessor that registers the topics.
     */
    @Bean
    public static BeanFactoryPostProcessor topicRegistrar() {
        Stream<String> debeziumTopicNames = Arrays.stream(DebeziumTopic.values())
                .map(DebeziumTopic::getTopicName);

        Stream<String> kTopicNames = Arrays.stream(KTopic.values())
                .map(KTopic::getTopicName);

        // Combine both streams into an array of topic names
        List<String> TOPIC_NAMES = Stream.concat(debeziumTopicNames, kTopicNames).toList();

        return beanFactory -> {

            for (String topicName : TOPIC_NAMES) {
                NewTopic topic = TopicBuilder.name(topicName)
                        .partitions(CPU_COUNT-1) // Use CPU_COUNT - 1 to avoid overloading the system
                        .replicas(1)
                        .build();

                beanFactory.registerSingleton(topicName + "Topic", topic);
            }
        };
    }

}
