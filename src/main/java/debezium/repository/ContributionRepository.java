package debezium.repository;

import debezium.model.Contribution;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContributionRepository extends CrudRepository<Contribution, Long> {
    boolean existsContributionByRecordId(Long recordId);
    List<Contribution> findContributionByRecordId(Long recordId);
}
