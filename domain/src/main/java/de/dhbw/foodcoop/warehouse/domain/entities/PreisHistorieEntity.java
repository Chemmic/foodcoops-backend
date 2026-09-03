package de.dhbw.foodcoop.warehouse.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(
        name = "preis_historie",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_preis_historie_bestand_deadline",
                        columnNames = {
                                "bestand_id",
                                "deadline_id"
                        }
                )
        }
)
public class PreisHistorieEntity {

    @Id
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "bestand_id",
            nullable = false
    )
    private BestandEntity bestand;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "deadline_id",
            nullable = false
    )
    private DeadlineEntity deadline;

    @Column(nullable = false)
    private BigDecimal preis;

    public PreisHistorieEntity() {
    }

    public PreisHistorieEntity(
            String id,
            BestandEntity bestand,
            DeadlineEntity deadline,
            BigDecimal preis
    ) {
        this.id = id;
        this.bestand = bestand;
        this.deadline = deadline;
        this.preis = preis;
    }

    public String getId() {
        return id;
    }

    public void setId(
            String id
    ) {
        this.id = id;
    }

    public BestandEntity getBestand() {
        return bestand;
    }

    public void setBestand(
            BestandEntity bestand
    ) {
        this.bestand = bestand;
    }

    public DeadlineEntity getDeadline() {
        return deadline;
    }

    public void setDeadline(
            DeadlineEntity deadline
    ) {
        this.deadline = deadline;
    }

    public BigDecimal getPreis() {
        return preis;
    }

    public void setPreis(
            BigDecimal preis
    ) {
        this.preis = preis;
    }
}