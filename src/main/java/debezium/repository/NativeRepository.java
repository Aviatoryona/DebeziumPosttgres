package debezium.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

@Repository
public class NativeRepository {

    @PersistenceContext
    private EntityManager em;


    public Tuple getAverageXContributions(Long contributionId, String type, int numberOfMonths) {

    }

    public Tuple getPreviousContribution(Long contributionId, String type) {

    }
}
