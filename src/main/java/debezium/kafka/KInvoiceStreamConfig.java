package debezium.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import debezium.enums.DebeziumTopic;
import debezium.enums.KTopic;
import debezium.model.Invoice;
import debezium.service.InvoiceService;
import debezium.service.UtilService;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

import static debezium.service.UtilService.decodeDecimal;

@Configuration
public class KInvoiceStreamConfig {

    private final InvoiceService invoiceService;
    private final UtilService  utilService;
    public KInvoiceStreamConfig(InvoiceService invoiceService, UtilService utilService) {
        this.invoiceService = invoiceService;
        this.utilService = utilService;
    }

    @Bean
    public KStream<String, String> invoicesStream(StreamsBuilder builder) {
        KStream<String, String> stream = builder.stream(DebeziumTopic.DEBEZIUM_INVOICES.getTopicName());

        //stream values and forward to another topic
        stream.mapValues(rawJson -> {
                    JsonNode beforeJson = utilService.beforeJson(rawJson);
                    if (beforeJson == null) {
                        //note: new record
                        return null;
                    }
                    JsonNode afterJson = utilService.afterJson(rawJson);
                    if (afterJson == null) {
                        //note: record deleted, check why
                        return null; // Skip if 'after' is missing
                    }
                    Invoice invoice = extractInvoice(beforeJson, afterJson);
                    if (invoice == null) {
                        return null; // Skip if invoice extraction fails
                    }
                    return invoice.toString(); // Convert to string or any other format as needed
                }).filter((key, value) -> value != null) // Filter out null values
                .to(KTopic.PROCESSED_INVOICES_TOPIC.getTopicName()); // Forward to another topic
        return stream;
    }

    private Invoice extractInvoice(JsonNode before, JsonNode after) {
        try {

            if (after == null || after.isNull() || after.isEmpty()) return null; //check why record was deleted

            long id = after.path("id").asLong();

            if (before == null || before.isNull() || before.isEmpty()) {
                //note: probably a new record/debezium restart issue, check if exists
                if (invoiceService.existsInvoiceByRecordId(id)) {
                    //return if exists, we checked before
                    System.out.println("Record with ID " + id + " already exists, skipping.");
                    return null;
                }
            }

            String encoded = after.path("amount").asText();
            BigDecimal amount = decodeDecimal(encoded, 5); // scale = 2

            // check for fraud
            Invoice invoice = new Invoice();
            invoice.setRecordId(id == 0 ? null : id); // Set ID if it's not zero
            invoice.setTotalAmount(amount.doubleValue());
            return invoice;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

}
