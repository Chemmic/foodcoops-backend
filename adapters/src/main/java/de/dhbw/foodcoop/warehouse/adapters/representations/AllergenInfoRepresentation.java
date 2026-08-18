package de.dhbw.foodcoop.warehouse.adapters.representations;

import java.util.Set;

public class AllergenInfoRepresentation {

    private final Set<String> getreide;
    private final boolean eier;
    private final boolean milch;
    private final boolean sesam;
    private final boolean schalenfruechte;
    private final boolean sellerie;
    private final boolean soja;
    private final String hinweis;

    public AllergenInfoRepresentation(
            Set<String> getreide,
            boolean eier,
            boolean milch,
            boolean sesam,
            boolean schalenfruechte,
            boolean sellerie,
            boolean soja,
            String hinweis
    ) {
        this.getreide = getreide;
        this.eier = eier;
        this.milch = milch;
        this.sesam = sesam;
        this.schalenfruechte = schalenfruechte;
        this.sellerie = sellerie;
        this.soja = soja;
        this.hinweis = hinweis;
    }

    public Set<String> getGetreide() {
        return getreide;
    }

    public boolean isEier() {
        return eier;
    }

    public boolean isMilch() {
        return milch;
    }

    public boolean isSesam() {
        return sesam;
    }

    public boolean isSchalenfruechte() {
        return schalenfruechte;
    }

    public boolean isSellerie() {
        return sellerie;
    }

    public boolean isSoja() {
        return soja;
    }

    public String getHinweis() {
        return hinweis;
    }
}