package de.dhbw.foodcoop.warehouse.adapters.representations;

public class FrischBestellSummeRepresentation {

    private FrischBestandRepresentation frischbestand;
    private double bestellmenge;

    public FrischBestellSummeRepresentation() {
    }

    public FrischBestellSummeRepresentation(
            FrischBestandRepresentation frischbestand,
            double bestellmenge) {

        this.frischbestand = frischbestand;
        this.bestellmenge = bestellmenge;
    }

    public FrischBestandRepresentation getFrischbestand() {
        return frischbestand;
    }

    public void setFrischbestand(
            FrischBestandRepresentation frischbestand) {
        this.frischbestand = frischbestand;
    }

    public double getBestellmenge() {
        return bestellmenge;
    }

    public void setBestellmenge(double bestellmenge) {
        this.bestellmenge = bestellmenge;
    }
}