package debezium.kafka;

import debezium.model.Invoice;
import debezium.service.InvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaConsumer {
    private final InvoiceService invoiceService;

    public KafkaConsumer(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @KafkaListener(topicPattern = "processed.*", groupId = "processed-data-group")
    public void listen(ConsumerRecord<String, Object> record) {
        Object o = record.value();
        String topic = record.topic();
        System.out.println("Topic :::: " + topic);

        if (topic.equals("processed_invoices_topic")) {
            processInvoice(o);
        }
    }

    private void processInvoice(Object json){
        try {
            if (json == null) {
                log.warn("Received null invoice data");
                return;
            }
            // Assuming json is a String representation of an Invoice
            Invoice invoice = Invoice.fromJson(json.toString());
            if (invoice != null) {
                System.out.println("Received invoice: " + invoice);
                invoiceService.save(invoice);
                log.info("Processed invoice: {}", invoice);
            } else {
                log.warn("Failed to parse invoice from JSON: {}", json);
            }
        } catch (Exception e) {
            log.error("Error processing invoice: {}", e.getMessage(), e);
        }
    }
}
