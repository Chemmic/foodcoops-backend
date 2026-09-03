package de.dhbw.foodcoop.warehouse.adapters.representations;

public class BrotBestellSummeRepresentation {

    private BrotBestandRepresentation brotbestand;
    private double bestellmenge;

    public BrotBestellSummeRepresentation() {
    }

    public BrotBestellSummeRepresentation(
            BrotBestandRepresentation brotbestand,
            double bestellmenge) {

        this.brotbestand = brotbestand;
        this.bestellmenge = bestellmenge;
    }

    public BrotBestandRepresentation getBrotbestand() {
        return brotbestand;
    }

    public void setBrotbestand(
            BrotBestandRepresentation brotbestand) {
        this.brotbestand = brotbestand;
    }

    public double getBestellmenge() {
        return bestellmenge;
    }

    public void setBestellmenge(double bestellmenge) {
        this.bestellmenge = bestellmenge;
    }
}