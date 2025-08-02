package debezium.service;

import debezium.model.Invoice;
import debezium.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService {
    private final InvoiceRepository repository;
    public InvoiceService(InvoiceRepository repository) {
        this.repository = repository;
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
