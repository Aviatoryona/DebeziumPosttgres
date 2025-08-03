package debezium.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class UtilService {
    public final ObjectMapper mapper = new ObjectMapper();

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

    public JsonNode beforeJson(String rawJson) {
        try {
            if (rawJson == null || rawJson.isEmpty()) {
                return null;
            }
            JsonNode root = mapper.readTree(rawJson);
            JsonNode after = root.path("payload").path("before");
            return after.isMissingNode() ? null : after;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    public JsonNode afterJson(String rawJson) {
        try {
            if (rawJson == null || rawJson.isEmpty()) {
                return null;
            }
            JsonNode root = mapper.readTree(rawJson);
            JsonNode after = root.path("payload").path("after");
            return after.isMissingNode() ? null : after;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static BigDecimal decodeDecimal(String base64, int scale) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        BigInteger unscaled = new BigInteger(bytes);
        return new BigDecimal(unscaled, scale);
    }
}
