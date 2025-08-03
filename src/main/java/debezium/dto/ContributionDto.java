package debezium.dto;

import debezium.model.Contribution;

import java.util.Map;

import static debezium.service.UtilService.decodeDecimal;

public record ContributionDto(
        long id,
        int year,
        Long member_id,
        Long sponsor_id,
        String ee,
        String er,
        String tot,
        String type,
        String month,
        String ssno,
        String status
) {
    public Contribution toContribution(Map<String, Integer> fieldScales) {
        Contribution contribution = new Contribution();
        contribution.setRecordId(id == 0 ? null : id); // Set ID if it's not zero
        contribution.setYear(year);
        contribution.setMemberId(member_id);
        contribution.setSponsorId(sponsor_id);
        contribution.setEe(decodeDecimal(ee, fieldScales.get("ee")));
        contribution.setEr(decodeDecimal(er, fieldScales.get("er")));
        contribution.setTotal(decodeDecimal(tot, fieldScales.get("tot")));
        contribution.setType(type);
        contribution.setMonth(month);
        contribution.setSsno(ssno);
        contribution.setStatus(status);
        return contribution;
    }
}
