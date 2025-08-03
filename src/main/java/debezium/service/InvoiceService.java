package debezium.service;

import debezium.model.Invoice;
import debezium.repository.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InvoiceService {
    private final InvoiceRepository repository;
    public InvoiceService(InvoiceRepository repository) {
        this.repository = repository;
    }

    public void processInvoice(Object json){
        try {
            if (json == null) {
                log.warn("Received null invoice data");
                return;
            }
            // Assuming json is a String representation of an Invoice
            Invoice invoice = Invoice.fromJson(json.toString());
            if (invoice != null) {
                save(invoice);
                log.info("Processed invoice: {}", invoice.getId());
            } else {
                log.warn("Failed to parse invoice from JSON: {}", json);
            }
        } catch (Exception e) {
            log.error("Error processing invoice: {}", e.getMessage(), e);
        }
    }

    public void save(Invoice invoice) {
        if (invoice.getId() != null) {
            if (repository.existsById(invoice.getId())){
                repository.save(invoice);
                return;
            }
            invoice.setId(null);
        }
        repository.save(invoice);
    }

    public boolean existsInvoiceByRecordId(long id) {
        return repository.existsInvoiceByRecordId(id);
    }
}
