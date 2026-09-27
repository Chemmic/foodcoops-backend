package de.dhbw.foodcoop.warehouse.adapters.representations;

import java.time.LocalDateTime;

/** Eine Charge Lagerware: noch vorhandene Menge zu einem Preis. */
public class LagerChargeRepresentation {

    private double menge;
    private double preis;
    private LocalDateTime eingelagertAm;

    public LagerChargeRepresentation() {
    }

    public LagerChargeRepresentation(double menge, double preis, LocalDateTime eingelagertAm) {
        this.menge = menge;
        this.preis = preis;
        this.eingelagertAm = eingelagertAm;
    }

    public double getMenge() {
        return menge;
    }

    public void setMenge(double menge) {
        this.menge = menge;
    }

    public double getPreis() {
        return preis;
    }

    public void setPreis(double preis) {
        this.preis = preis;
    }

    public LocalDateTime getEingelagertAm() {
        return eingelagertAm;
    }

    public void setEingelagertAm(LocalDateTime eingelagertAm) {
        this.eingelagertAm = eingelagertAm;
    }
}
