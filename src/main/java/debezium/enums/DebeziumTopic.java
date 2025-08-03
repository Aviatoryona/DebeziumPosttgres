package debezium.enums;

import lombok.Getter;

@Getter
public enum DebeziumTopic {
    DEBEZIUM_INVOICES("debezium_master.public.invoices"),
    DEBEZIUM_CONTRIBUTIONS("debezium_master.public.contributions");

    private final String topicName;

    DebeziumTopic(String topicName) {
        this.topicName = topicName;
    }
}
