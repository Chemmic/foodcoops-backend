package de.dhbw.foodcoop.warehouse.adapters.representations;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PreisHistorieRepresentation {

    private String id;

    private String bestandId;

    private String bestandName;

    private String deadlineId;

    private LocalDateTime deadline;

    private BigDecimal preis;

    public PreisHistorieRepresentation() {
    }

    public PreisHistorieRepresentation(
            String id,
            String bestandId,
            String bestandName,
            String deadlineId,
            LocalDateTime deadline,
            BigDecimal preis
    ) {
        this.id = id;
        this.bestandId = bestandId;
        this.bestandName =
                bestandName;
        this.deadlineId =
                deadlineId;
        this.deadline =
                deadline;
        this.preis =
                preis;
    }

    public String getId() {
        return id;
    }

    public String getBestandId() {
        return bestandId;
    }

    public String getBestandName() {
        return bestandName;
    }

    public String getDeadlineId() {
        return deadlineId;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public BigDecimal getPreis() {
        return preis;
    }
}