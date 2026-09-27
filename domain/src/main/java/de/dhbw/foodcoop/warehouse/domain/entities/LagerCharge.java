package de.dhbw.foodcoop.warehouse.domain.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Eine Lieferung Lagerware zu einem bestimmten Preis.
 *
 * Kommt neue Ware zu einem anderen Preis dazu, bevor die alte verbraucht
 * ist, gibt es mehrere Chargen. Verkauft wird die älteste zuerst (FIFO),
 * d.h. Mitglieder zahlen erst den alten Preis.
 *
 * Verweist nur über die ID auf das Produkt: Lagerprodukte werden beim
 * Ändern und beim Einkauf aus den Daten des Browsers neu aufgebaut – eine
 * Beziehung am Produkt würde die Chargen dabei überschreiben.
 */
@Entity
@Table(name = "lager_charge")
public class LagerCharge {

    @Id
    private String id;

    @Column(name = "produkt_id", nullable = false)
    private String produktId;

    /** Noch vorhandene Menge (in der Einheit des Lagerbestands). */
    @Column
    private double menge;

    /** Preis pro Einheit. */
    @Column
    private BigDecimal preis;

    @Column
    private LocalDateTime eingelagertAm;


    public LagerCharge() {
    }


    public LagerCharge(String produktId, double menge, BigDecimal preis, LocalDateTime eingelagertAm) {
        this(UUID.randomUUID().toString(), produktId, menge, preis, eingelagertAm);
    }


    public LagerCharge(String id, String produktId, double menge, BigDecimal preis, LocalDateTime eingelagertAm) {
        this.id = id;
        this.produktId = produktId;
        this.menge = menge;
        this.preis = preis;
        this.eingelagertAm = eingelagertAm;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProduktId() {
        return produktId;
    }

    public void setProduktId(String produktId) {
        this.produktId = produktId;
    }

    public double getMenge() {
        return menge;
    }

    public void setMenge(double menge) {
        this.menge = menge;
    }

    public BigDecimal getPreis() {
        return preis;
    }

    public void setPreis(BigDecimal preis) {
        this.preis = preis;
    }

    public LocalDateTime getEingelagertAm() {
        return eingelagertAm;
    }

    public void setEingelagertAm(LocalDateTime eingelagertAm) {
        this.eingelagertAm = eingelagertAm;
    }
}
