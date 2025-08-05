package debezium.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import debezium.enums.MonthEnum;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class UtilService {
    public final ObjectMapper mapper = new ObjectMapper();

    /**
     * Extracts field scales from the given raw JSON string.
     *
     * @param rawJson The raw JSON string to parse.
     * @return A map where keys are field names and values are their corresponding scales.
     */
    public Map<String, Integer> getFieldScales(String rawJson) {
        try {

            JsonNode root = mapper.readTree(rawJson);
            Map<String, Integer> fieldScaleMap = new HashMap<>();

            JsonNode fields = root.at("/schema/fields/0/fields");
            if (fields.isArray()) {
                for (JsonNode field : fields) {
                    JsonNode fieldNameNode = field.get("field");
                    JsonNode parametersNode = field.get("parameters");

                    if (fieldNameNode != null && parametersNode != null && parametersNode.has("scale")) {
                        String fieldName = fieldNameNode.asText();
                        int scale = parametersNode.get("scale").asInt();
                        fieldScaleMap.put(fieldName, scale);
                    }
                }
            }
            return fieldScaleMap;
        } catch (JsonProcessingException e) {
            System.out.println("Error processing JSON: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Extracts the "before" JSON node from the given raw JSON string.
     *
     * @param rawJson The raw JSON string to parse.
     * @return The "before" JSON node, or null if it is missing, null, or empty.
     */
    public JsonNode beforeJson(String rawJson) {
        try {
            if (rawJson == null || rawJson.isEmpty()) {
                return null;
            }
            JsonNode root = mapper.readTree(rawJson);
            JsonNode before = root.path("payload").path("before");
            return before.isMissingNode() || before.isNull() || before.isEmpty() ? null : before;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    /**
     * Extracts the "after" JSON node from the given raw JSON string.
     *
     * @param rawJson The raw JSON string to parse.
     * @return The "after" JSON node, or null if it is missing, null, or empty.
     */
    public JsonNode afterJson(String rawJson) {
        try {
            if (rawJson == null || rawJson.isEmpty()) {
                return null;
            }
            JsonNode root = mapper.readTree(rawJson);
            JsonNode after = root.path("payload").path("after");
            return after.isMissingNode() || after.isNull() || after.isEmpty() ? null : after;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    /**
     * Checks if the given year and month string is before the current date minus a specified number of months.
     *
     * @param year          The year to check.
     * @param monthString   The month as a string (e.g., "JANUARY", "FEBRUARY").
     * @param numberOfMonths The number of months to subtract from the current date.
     * @return true if the contribution period is before the calculated months ago, false otherwise.
     */
    public boolean isMonthsAgo(int year, String monthString, int numberOfMonths) {
        try {
            Month month = Month.valueOf(MonthEnum.valueOf(monthString.toUpperCase()).getName());
            YearMonth contributionPeriod = YearMonth.of(year, month);
            YearMonth monthsAgo = YearMonth.from(LocalDate.now().minusMonths(numberOfMonths));

            // Check if the contribution period is before the calculated months ago
            return contributionPeriod.isBefore(monthsAgo);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
    }

    /**
     * Decodes a Base64 encoded decimal string into a BigDecimal with the specified scale.
     *
     * @param base64 The Base64 encoded string representing the decimal.
     * @param scale  The scale to be applied to the BigDecimal.
     * @return The decoded BigDecimal.
     */
    public static BigDecimal decodeDecimal(String base64, int scale) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        BigInteger unscaled = new BigInteger(bytes);
        return new BigDecimal(unscaled, scale);
    }
}
