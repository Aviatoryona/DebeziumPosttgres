package debezium.service;

import debezium.dto.ContributionDto;
import debezium.model.Contribution;
import debezium.repository.ContributionRepository;
import debezium.repository.NativeRepository;
import jakarta.persistence.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class ContributionService {
    private final ContributionRepository repository;
    private final NativeRepository nativeRepository;

    public ContributionService(ContributionRepository contributionRepository, NativeRepository nativeRepository) {
        this.repository = contributionRepository;
        this.nativeRepository = nativeRepository;
    }

    public void processContribution(Object json){
        try {
            if (json == null) {
                log.warn("Received null invoice data");
                return;
            }
            // Assuming json is a String representation of an Invoice
            Contribution contribution = Contribution.fromJson(json.toString());
            if (contribution != null) {
                save(contribution);
                log.info("Processed contribution: {}", contribution.getId());
            } else {
                log.warn("Failed to parse contribution from JSON: {}", json);
            }
        } catch (Exception e) {
            log.error("Error processing contribution: {}", e.getMessage(), e);
        }
    }

    public void save(Contribution co) {
        if (co.getId() != null) {
            if (repository.existsById(co.getId())){
                repository.save(co);
                return;
            }
            co.setId(null);
        }
        repository.save(co);
    }

    public boolean existsContributionByRecordId(long id) {
        return  repository.existsContributionByRecordId(id);
    }

    public ContributionDto getPreviousContribution(Long contributionId, String type) {
        Tuple tuple= nativeRepository.getPreviousContribution(contributionId, type);
        if (tuple == null) {
            return null;
        }
    }

    public BigDecimal getAverageXContributions(Long contributionId, String type, int numberOfMonths) {
        Tuple tuple = nativeRepository.getAverageXContributions(contributionId, type, numberOfMonths);
        if (tuple == null) {
            return null;
        }
        return tuple.get("average", BigDecimal.class);
    }
}
