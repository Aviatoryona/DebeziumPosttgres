package debezium.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "invoices", schema = "fraud")
public class Invoice implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long recordId;
    private Double totalAmount;

    public static Invoice fromJson(String string) {
        try {
            return new ObjectMapper().readValue(string, Invoice.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace(System.err);
            return null; // Handle the exception as needed
        }
    }
}
