package debezium.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import debezium.component.ContributionFraudDetector;
import debezium.dto.ContributionDto;
import debezium.enums.DebeziumTopic;
import debezium.enums.KTopic;
import debezium.model.Contribution;
import debezium.service.ContributionService;
import debezium.service.UtilService;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;

@Configuration
public class KContributionStreamConfig {

    private final ContributionFraudDetector contributionFraudDetector;
    private final UtilService utilService;

    public KContributionStreamConfig(ContributionFraudDetector contributionFraudDetector, UtilService utilService) {
        this.contributionFraudDetector = contributionFraudDetector;
        this.utilService = utilService;
    }


    @Bean
    public KStream<String, String> stream(StreamsBuilder builder) {
        KStream<String, String> stream = builder.stream(DebeziumTopic.DEBEZIUM_CONTRIBUTIONS.getTopicName());

        // Use split() to create a new Splitter instance
        stream.split()
                .branch((key, rawJson) -> utilService.beforeJson(rawJson) == null, // Branch for new records
                        Branched.withConsumer(newStream ->
                                newStream
                                        .mapValues(rawJson -> {
                                            Contribution contribution = processNewContribution(rawJson);
                                            if (contribution == null) {
                                                return null;
                                            }
                                            return contribution.toString();
                                        })
                                        .filter((key, value) -> value != null)
                                        .to(KTopic.PROCESSED_CONTRIBUTIONS_TOPIC.getTopicName())
                        )
                )
                .branch((key, rawJson) -> utilService.beforeJson(rawJson) != null, // Branch for updates/deletes
                        Branched.withConsumer(existingStream ->
                                existingStream
                                        .mapValues(rawJson -> {
                                            Contribution contribution = extractContribution(rawJson);
                                            if (contribution == null) {
                                                return null;
                                            }
                                            return contribution.toString();
                                        })
                                        .filter((key, value) -> value != null)
                                        .to(KTopic.PROCESSED_CONTRIBUTIONS_TOPIC.getTopicName())//stream values and forward to another topic
                        )
                )
                .noDefaultBranch(); // Optional: Specify that there's no default branch for un-matched records

        return stream;


    }


    private Contribution processNewContribution(String rawJson) {
        try {
            ObjectMapper mapper = utilService.mapper;
            JsonNode before = utilService.beforeJson(rawJson);
            JsonNode after = utilService.afterJson(rawJson);

            if (after == null || after.isNull() || after.isEmpty()) return null;

            // Deserialize the JSON to ContributionDto
            ContributionDto contributionDtoAfter = mapper.readValue(after.toString(), ContributionDto.class);
            long id = contributionDtoAfter.id();
            if (before == null || before.isNull() || before.isEmpty()) {
                //note: probably a new record/debezium restart issue, check if exists
                if (contributionFraudDetector.existsContributionByRecordId(id)) {
                    //return if exists, we checked before
                    System.out.println("Record with ID " + id + " already exists, skipping.");
                    return null;
                }
            }

            //check fraud
            Contribution contribution = contributionDtoAfter.toContribution(utilService.getFieldScales(rawJson));
            Optional<String> reason = contributionFraudDetector.detectFraud(contribution);


            // check previous amounts
            // check frequency of contributions
            // check for duplicate contributions
            Map<String, Integer> fieldScales = utilService.getFieldScales(rawJson);


            return contributionDtoAfter.toContribution(fieldScales);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    private Contribution extractContribution(String rawJson) {
        try {
            ObjectMapper mapper = utilService.mapper;
            Map<String, Integer> fieldScales = utilService.getFieldScales(rawJson);
            JsonNode before = utilService.beforeJson(rawJson);
            JsonNode after = utilService.afterJson(rawJson);

            if (after == null || after.isNull() || after.isEmpty()) return null; //check why record was deleted

            ContributionDto contributionDtoBefore = mapper.readValue(before.toString(), ContributionDto.class);
            ContributionDto contributionDtoAfter = mapper.readValue(after.toString(), ContributionDto.class);

            Contribution contributionBefore = contributionDtoBefore.toContribution(fieldScales);
            Contribution contributionAfter = contributionDtoAfter.toContribution(fieldScales);

            // check for fraud
            Optional<String> reason = contributionFraudDetector.detectFraud(contributionBefore, contributionAfter);
            if (reason.isPresent()) {
                contributionAfter.setReasonFlagged(reason.get());
                return contributionAfter;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

}
