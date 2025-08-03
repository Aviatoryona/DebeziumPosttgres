package debezium.kafka;

import debezium.enums.KTopic;
import debezium.service.ContributionService;
import debezium.service.InvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaConsumer {
    private final InvoiceService invoiceService;
    private final ContributionService contributionService;

    public KafkaConsumer(InvoiceService invoiceService, ContributionService contributionService) {
        this.invoiceService = invoiceService;
        this.contributionService = contributionService;
    }

    @KafkaListener(topicPattern = "processed.*", groupId = "processed-data-group")
    public void listen(ConsumerRecord<String, Object> record) {
        Object o = record.value();
        String topic = record.topic();
        KTopic kTopic = KTopic.fromTopicName(topic);
        if (kTopic==null){
            log.warn("Received data from unknown topic: {}", topic);
            return;
        }
        switch (kTopic) {
            case PROCESSED_INVOICES_TOPIC:
                log.info("Received processed invoice data from topic: {}", topic);
                invoiceService.processInvoice(o);
                break;
            case PROCESSED_CONTRIBUTIONS_TOPIC:
                log.info("Received processed contribution data from topic: {}", topic);
                contributionService.processContribution(o);
                break;
            default:
                log.warn("Received data from unknown topic: {}", topic);
        }
    }


}
