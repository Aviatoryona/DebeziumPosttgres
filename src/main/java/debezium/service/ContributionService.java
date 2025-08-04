package debezium.service;

import debezium.model.Contribution;
import debezium.repository.ContributionRepository;
import debezium.repository.NativeRepository;
import jakarta.persistence.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
public class ContributionService {
    private final ContributionRepository repository;
    private final NativeRepository nativeRepository;

    public ContributionService(ContributionRepository contributionRepository, NativeRepository nativeRepository) {
        this.repository = contributionRepository;
        this.nativeRepository = nativeRepository;
    }

    public void processContribution(Object json) {
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
            if (repository.existsById(co.getId())) {
                repository.save(co);
                return;
            }
            co.setId(null);
        }
        repository.save(co);
    }

    public boolean existsContributionByRecordId(long id) {
        return repository.existsContributionByRecordId(id);
    }

    public Contribution getPreviousContribution(Long contributionId) {
        Tuple tuple = nativeRepository.getPreviousContribution(contributionId);
        if (tuple == null) {
            return null;
        }
        BigDecimal ee = tuple.get("ee", BigDecimal.class);
        BigDecimal er = tuple.get("er", BigDecimal.class);
        BigDecimal tot = tuple.get("tot", BigDecimal.class);
        var year = tuple.get("year", Integer.class);
        var month = tuple.get("month", String.class);
        Contribution contribution = new Contribution();
        contribution.setRecordId(tuple.get("id", Long.class));
        contribution.setEe(ee);
        contribution.setEr(er);
        contribution.setTotal(tot);
        contribution.setYear(year);
        contribution.setMonth(month);

        return contribution;
    }

    public BigDecimal getAverageXContributions(Long contributionId, int numberOfMonths) {
        List<Tuple> xContributions = nativeRepository.getXContributions(contributionId, numberOfMonths);
        if (xContributions == null || xContributions.isEmpty()) {
            return null;
        }

        BigDecimal sum = xContributions.parallelStream()
                .map(tuple -> {
                    BigDecimal ee = tuple.get("ee", BigDecimal.class);
                    BigDecimal er = tuple.get("er", BigDecimal.class);
                    if (ee == null) ee = BigDecimal.ZERO;
                    if (er == null) er = BigDecimal.ZERO;
                    return ee.add(er);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        //note: compute frequency of contributions

        if (sum.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return sum.divide(BigDecimal.valueOf(xContributions.size()), RoundingMode.HALF_UP);
    }


    public Contribution getAverageContribution() {
        Tuple tuple = nativeRepository.getAverageAllContributions();
        if (tuple == null) {
            return null;
        }
        BigDecimal averageEe = tuple.get("avg_ee", BigDecimal.class);
        BigDecimal averageEr = tuple.get("avg_er", BigDecimal.class);
        Contribution contribution = new Contribution();
        contribution.setEe(averageEe);
        contribution.setEr(averageEr);
        return contribution;
    }
}
