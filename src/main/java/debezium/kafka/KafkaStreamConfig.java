package debezium.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import debezium.model.Invoice;
import debezium.service.InvoiceService;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;

@Configuration
public class KafkaStreamConfig {

    private final InvoiceService invoiceService;

    private final ObjectMapper mapper = new ObjectMapper();

    public KafkaStreamConfig(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Bean
    public KStream<String, String> invoicesStream(StreamsBuilder builder) {
        KStream<String, String> stream = builder.stream("debezium_master.public.invoices");

        //stream values and forward to another topic
        stream.mapValues(rawJson -> {
                    JsonNode beforeJson = beforeJson(rawJson);
                    JsonNode afterJson = afterJson(rawJson);
                    if (afterJson == null) {
                        //note: record deleted, check why
                        return null; // Skip if 'after' is missing
                    }
                    Invoice invoice = extractInvoice(beforeJson,afterJson);
                    if (invoice == null) {
                        return null; // Skip if invoice extraction fails
                    }
                    return invoice.toString(); // Convert to string or any other format as needed
                }).filter((key, value) -> value != null) // Filter out null values
                .to("processed_invoices_topic"); // Forward to another topic
        return stream;
    }

    private JsonNode beforeJson(String rawJson) {
        try {
            JsonNode root = mapper.readTree(rawJson);
            JsonNode after = root.path("payload").path("before");
            return after.isMissingNode() ? null : after;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    private JsonNode afterJson(String rawJson) {
        try {
            JsonNode root = mapper.readTree(rawJson);
            JsonNode after = root.path("payload").path("after");
            return after.isMissingNode() ? null : after;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    private Invoice extractInvoice(JsonNode before,JsonNode after) {
        try {

            if (after == null || after.isNull() || after.isEmpty()) return null; //check why record was deleted

            long id = after.path("id").asLong();

            if (before == null || before.isNull() || before.isEmpty()) {
                //note: probably a new record/debezium restart issue, check if exists
                if(invoiceService.existsInvoiceByRecordId(id)){
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

    private BigDecimal decodeDecimal(String base64, int scale) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        BigInteger unscaled = new BigInteger(bytes);
        return new BigDecimal(unscaled, scale);
    }
}
