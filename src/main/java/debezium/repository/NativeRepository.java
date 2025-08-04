package debezium.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NativeRepository {

    @PersistenceContext
    private EntityManager em;


    public List<Tuple> getXContributions(Long contributionId, int numberOfMonths) {
        try {
            Query query = em.createNativeQuery("select c.id, c.ee, c.er, c.tot\n" +
                    "from public.contributions c\n" +
                    "         left join public.contributions c2 on c2.member_id = c.member_id\n" +
                    "where c.sponsor_id = c2.sponsor_id\n" +
                    "  and c.type = c2.type\n" +
                    "  and date(concat_ws('-', c.year, c.month, 1)) < date(concat_ws('-', c2.year, c2.month, 1))\n" +
                    "  and c2.id = :contributionId\n" +
                    "order by date(concat_ws('-', c.year, c.month, 1))", Tuple.class);
            query.setParameter("contributionId", contributionId);
            query.setMaxResults(numberOfMonths);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    public Tuple getPreviousContribution(Long contributionId) {
        try {
            Query query = em.createNativeQuery("select c.id, c.ee, c.er, c.tot,c.year,c.month\n" +
                    "from public.contributions c\n" +
                    "         left join public.contributions c2 on c2.member_id = c.member_id\n" +
                    "where c.sponsor_id = c2.sponsor_id\n" +
                    "  and c.type = c2.type\n" +
                    "  and date(concat_ws('-', c.year, c.month, 1)) < date(concat_ws('-', c2.year, c2.month, 1))\n" +
                    "  and c2.id = :contributionId\n" +
                    "order by date(concat_ws('-', c.year, c.month, 1))", Tuple.class);
            query.setParameter("contributionId", contributionId);
            query.setMaxResults(1);
            return (Tuple) query.getSingleResult();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    public Tuple getAverageAllContributions() {
        try {
            Query query = em.createNativeQuery("select * from mv_ee_er_avg", Tuple.class);
            return (Tuple) query.getSingleResult();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }
}
