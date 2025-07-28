package debezium.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaConsumer {
    // This method will be called whenever a message is received on the specified topic
    @KafkaListener(topicPattern = "debezium.*", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(ConsumerRecord<String, Object> record) {
        Object o = record.value();
        String topic = record.topic();
        System.out.println("Topic :::: " + topic);
        log.info("Data: {}", o);
    }
}
