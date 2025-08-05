package debezium.component;

import debezium.enums.MonthEnum;
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

    /**
     * Detects fraud based on the contribution before and after a change.
     *
     * @param before The contribution before the change.
     * @param after  The contribution after the change.
     * @return An Optional containing a string with reasons for fraud detection, or empty if no fraud is detected.
     */
    public Optional<String> detectFraud(Contribution before, Contribution after) {

        List<String> reasons = new ArrayList<>();

        BigDecimal beforeTotal = Optional.ofNullable(before.getEe()).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(before.getEr()).orElse(BigDecimal.ZERO));

        BigDecimal afterTotal = Optional.ofNullable(after.getEe()).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(after.getEr()).orElse(BigDecimal.ZERO));


        double rate = afterTotal.divide(beforeTotal, RoundingMode.HALF_UP).doubleValue();
        if (rate > 1.99 || rate < 0.51) {
            reasons.add(String.format(
                    "&#128681 Contribution amount increased/decreased by large margin of x%.6f (from %.2f to %.2f)",
                    rate,
                    beforeTotal.doubleValue(),
                    afterTotal.doubleValue()
            ));
        }

        //check average contribution amount to detect suspicious increases
        checkPreviousAverageContribution(after, 10, reasons);

        //check average contribution amount to detect suspicious increases
        checkAverageAllContributions(afterTotal, reasons);

        //check maximum allowed contribution
        checkMaximumAllowedContribution(afterTotal, BigDecimal.valueOf(9999.99), reasons);

        //check other conditions

        if (!reasons.isEmpty()) {
            return Optional.of(String.join("<br><br>", reasons));
        }

        return Optional.empty(); // No fraud
    }

    /**
     * Detects fraud based on the contribution date, last contribution date, and average contribution amount.
     *
     * @param after The contribution after the change.
     * @return An Optional containing a string with reasons for fraud detection, or empty if no fraud is detected.
     */
    public Optional<String> detectFraud(Contribution after) {
        List<String> reasons = new ArrayList<>();

        //check if the contribution date is x months in the past
        if (utilService.isMonthsAgo(
                after.getYear(),
                after.getMonth(),
                4
        )) {
            //contribution should be posted earlier, why ARREARS?
            reasons.add(String.format("&#9889 Contribution date is more than %s months in the past (%s/%s)", 4, after.getYear(), after.getMonth()));
        }

        // check last contribution date
        Contribution last = contributionService.getPreviousContribution(after.getRecordId());
        if (last != null) {
            YearMonth lastContributionDate = YearMonth.of(last.getYear(), Month.valueOf(
                    MonthEnum.valueOf(last.getMonth().toUpperCase()).getName()
            ));
            YearMonth currentContributionDate = YearMonth.of(after.getYear(), Month.valueOf(
                    MonthEnum.valueOf(after.getMonth().toUpperCase()).getName()
            ));


            //calculate the difference in year and months
            long monthsDiff = ChronoUnit.MONTHS.between(lastContributionDate, currentContributionDate);

            //check if the currentContributionDate-lastContributionDate is more than x months
            if (monthsDiff >= 4) {
                //before this contribution, member had x dormant months. Check why
                reasons.add(String.format("&#128165 Sudden large contribution detected. Last contribution was %s months before this. ", monthsDiff));
            }

        }

        //check average contribution amount to detect suspicious increases
        checkPreviousAverageContribution(after, 10, reasons);

        BigDecimal totalContribution = Optional.ofNullable(after.getEe()).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(after.getEr()).orElse(BigDecimal.ZERO));
        //check the average of all contributions to detect suspicious increases
        checkAverageAllContributions(totalContribution, reasons);

        //check maximum allowed contribution
        checkMaximumAllowedContribution(totalContribution, BigDecimal.valueOf(9999.99), reasons);

        //check other conditions

        if (!reasons.isEmpty()) {
            return Optional.of(String.join("<br><br>", reasons));
        }

        return Optional.empty(); // No fraud
    }

    /**
     * Checks if the contribution amount is suspiciously high compared to the average of the last 10 contributions.
     * If it is, it adds a reason to the list.
     *
     * @param after          The contribution after the change.
     * @param numberOfMonths The number of months to consider for the average.
     * @param reasons        The list of reasons for fraud detection.
     */
    private void checkPreviousAverageContribution(Contribution after, int numberOfMonths, List<String> reasons) {
        //get the average of the last 10 contributions
        BigDecimal averageContribution = contributionService.getAverageXContributions(after.getRecordId(), numberOfMonths);
        BigDecimal totalContribution = Optional.ofNullable(after.getEe()).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(after.getEr()).orElse(BigDecimal.ZERO));
        if (averageContribution != null) {
            if (totalContribution.compareTo(averageContribution.multiply(BigDecimal.valueOf(2))) > 0)
                reasons.add(String.format("&#128293 Contribution amount is suspiciously high: %.2f (Members' Average: %.2f)",
                        totalContribution.doubleValue(),
                        averageContribution.doubleValue()));
        }
    }


    /**
     * Checks if the total contribution amount is suspiciously high compared to the average contributions.
     * If the total contribution is more than 9x the average, it adds a reason to the list.
     *
     * @param afterTotal The total contribution amount after the change.
     * @param reasons    The list of reasons for fraud detection.
     */
    private void checkAverageAllContributions(BigDecimal afterTotal, List<String> reasons) {
        Contribution averageContribution = contributionService.getAverageContribution();
        if (averageContribution != null) {
            BigDecimal averageTotal = Optional.ofNullable(averageContribution.getEe()).orElse(BigDecimal.ZERO)
                    .add(Optional.ofNullable(averageContribution.getEr()).orElse(BigDecimal.ZERO));
            if (afterTotal.compareTo(averageTotal.multiply(BigDecimal.valueOf(9))) > 0) {
                reasons.add(String.format("&#128293 Contribution amount is suspiciously high: %.2f (DB Average: %.2f)",
                        afterTotal.doubleValue(),
                        averageTotal.doubleValue()));
            }
        }
    }

    /**
     * Checks if the total contribution amount exceeds the maximum allowed contribution.
     * If it does, it adds a reason to the list.
     *
     * @param afterTotal The total contribution amount after the change.
     * @param maxAllowed The maximum allowed contribution amount.
     * @param reasons    The list of reasons for fraud detection.
     */
    public void checkMaximumAllowedContribution(BigDecimal afterTotal, BigDecimal maxAllowed, List<String> reasons) {
        if (afterTotal.compareTo(maxAllowed) > 0) {
            reasons.add(String.format("&#128293 Contribution amount is too high: %.2f (max allowed: %.2f)", afterTotal.doubleValue(), maxAllowed.doubleValue()));
        }
    }

    /**
     * Checks if a contribution with the given record ID already exists.
     *
     * @param id The record ID of the contribution to check.
     * @return true if a contribution with the given record ID exists, false otherwise.
     */
    public boolean existsContributionByRecordId(long id) {
        return contributionService.existsContributionByRecordId(id);
    }
}
