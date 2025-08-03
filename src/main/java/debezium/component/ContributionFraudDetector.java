package debezium.component;

import debezium.dto.ContributionDto;
import debezium.model.Contribution;
import debezium.service.ContributionService;
import debezium.service.UtilService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ContributionFraudDetector {
    private final ContributionService contributionService;
    private final UtilService utilService;

    public ContributionFraudDetector(ContributionService contributionService, UtilService utilService) {
        this.contributionService = contributionService;
        this.utilService = utilService;
    }

    public Optional<String> detectFraud(Contribution before, Contribution after) {

        BigDecimal beforeTotal = Optional.ofNullable(before.getEe()).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(before.getEr()).orElse(BigDecimal.ZERO));

        BigDecimal afterTotal = Optional.ofNullable(after.getEe()).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(after.getEr()).orElse(BigDecimal.ZERO));


        double rate = afterTotal.divide(beforeTotal, RoundingMode.HALF_UP).doubleValue();
        if (rate > 1.99) {
            return Optional.of(String.format(
                    "Contribution amount increased by x%.2f (from %.2f to %.2f)",
                    rate,
                    beforeTotal.doubleValue(),
                    afterTotal.doubleValue()
            ));
        }
        //check other conditions

        return Optional.empty(); // No fraud
    }

    public Optional<String> detectFraud(Contribution after) {
        List<String> reasons = new ArrayList<>();

        //check if the contribution date is x months in the past
        if (utilService.isMonthsAgo(
                after.getYear(),
                after.getMonth(),
                4
        )) {
            //contribution should be posted earlier, why ARREARS?
            reasons.add(String.format("Contribution date is more than %s months in the past (%s/%s)",4, after.getYear(), after.getMonth()));
        }

        // check last contribution date
        ContributionDto last
                = contributionService.getPreviousContribution(after.getRecordId(), after.getType());
        if (last != null) {
            YearMonth lastContributionDate = YearMonth.of(last.year(), Month.valueOf(last.month().toUpperCase()));
            YearMonth currentContributionDate = YearMonth.of(after.getYear(), Month.valueOf(after.getMonth().toUpperCase()));

            //calculate the difference in year and months
            long monthsDiff = ChronoUnit.MONTHS.between(lastContributionDate, currentContributionDate);

            //check if the currentContributionDate-lastContributionDate is more than x months
            if (monthsDiff >= 4) {
                //before this contribution, member had x dormant months. Check why
                reasons.add(String.format("Last contribution was %s months before this.",monthsDiff));
            }
        }

        //check average contribution amount to detect suspicious increases
        BigDecimal averageContribution = contributionService.getAverageXContributions(after.getRecordId(), after.getType(), 10);
        BigDecimal totalContribution = Optional.ofNullable(after.getEe()).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(after.getEr()).orElse(BigDecimal.ZERO));
        if (averageContribution != null) {
            if (totalContribution.compareTo(averageContribution.multiply(BigDecimal.valueOf(2))) > 0)
                reasons.add(String.format("Contribution amount is suspiciously high: %.2f (average: %.2f)",
                        totalContribution.doubleValue(),
                        averageContribution.doubleValue()));
        } else {
            reasons.add("No contributions before this, sudden large contribution detected.");
        }

        //check other conditions

        if (!reasons.isEmpty()) {
            return Optional.of(String.join("<br>", reasons));
        }

        return Optional.empty(); // No fraud
    }

    public boolean existsContributionByRecordId(long id) {
        return contributionService.existsContributionByRecordId(id);
    }
}
