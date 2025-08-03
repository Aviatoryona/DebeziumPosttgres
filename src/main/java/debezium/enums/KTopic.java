package debezium.enums;

import lombok.Getter;

@Getter
public enum KTopic {
    PROCESSED_INVOICES_TOPIC("processed_invoices_topic"),
    PROCESSED_CONTRIBUTIONS_TOPIC("processed_contributions_topic");

    private final String topicName;
    KTopic(String topicName) {
        this.topicName = topicName;
    }

    public static KTopic fromTopicName(String topic) {
        for (KTopic kTopic : KTopic.values()) {
            if (kTopic.getTopicName().equals(topic)) {
                return kTopic;
            }
        }
        return null;
    }
}
