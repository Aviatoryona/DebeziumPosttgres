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

    public ContributionDto getPreviousContribution(Long contributionId) {
        Tuple tuple= nativeRepository.getPreviousContribution(contributionId);
        if (tuple == null) {
            return null;
        }
    }

    public BigDecimal getAverageXContributions(Long contributionId, int numberOfMonths) {
        var tuple = nativeRepository.getXContributions(contributionId, numberOfMonths);
        if (tuple == null) {
            return null;
        }
        return tuple.get("average", BigDecimal.class);
    }

    public Contribution getAverageContribution() {
        Tuple tuple = nativeRepository.getAverageAllContributions();
        if (tuple == null) {
            return null;
        }
        BigDecimal averageEe = tuple.get("avg_ee", BigDecimal.class);
        BigDecimal averageEr = tuple.get("avg_er", BigDecimal.class);
        Contribution contribution= new Contribution();
        contribution.setEe(averageEe);
        contribution.setEr(averageEr);
        return contribution;
    }
}
