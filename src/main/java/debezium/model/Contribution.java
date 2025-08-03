package debezium.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.RecursiveTask;

@Getter
@Setter
@Entity
@Table(name = "contributions", schema = "fraud")
public class Contribution implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long recordId;
    private Long memberId;
    private Long sponsorId;
    private int year;
    private BigDecimal ee;
    private BigDecimal er;
    private BigDecimal total;

    private String type;
    private String status;
    private String month;
    private String ssno;

    @Column(length = 2000)
    private String reasonFlagged;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public static Contribution fromJson(String string) {
        try {
            return new ObjectMapper().readValue(string, Contribution.class);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }
}
