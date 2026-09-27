package de.dhbw.foodcoop.warehouse.plugins.historie;

import java.util.Collection;
import java.util.List;

import de.dhbw.foodcoop.warehouse.domain.entities.BestandEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.BestellungEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestand;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.Produkt;

/** Gemeinsame Hilfsfunktionen für Historie und Bestellrunden-Übersicht. */
final class HistorieHilfen {

    private HistorieHilfen() {
    }


    static BestandEntity bestandVon(BestellungEntity bestellung) {
        if (bestellung instanceof FrischBestellung frisch) {
            return frisch.getFrischbestand();
        }

        if (bestellung instanceof BrotBestellung brot) {
            return brot.getBrotBestand();
        }

        return null;
    }


    static boolean istSpezialfall(BestandEntity bestand) {
        return bestand instanceof FrischBestand frisch && frisch.isSpezialfallBestelleinheit();
    }


    static String produktName(BestandEntity bestand) {
        return bestand == null || bestand.getName() == null
                ? "Unbekanntes Produkt"
                : bestand.getName();
    }


    static String einheitVon(BestandEntity bestand) {
        if (bestand instanceof FrischBestand frisch) {
            return frisch.getEinheit() == null ? null : frisch.getEinheit().getName();
        }

        if (bestand instanceof Produkt produkt) {
            return produkt.getLagerbestand() == null || produkt.getLagerbestand().getEinheit() == null
                    ? null
                    : produkt.getLagerbestand().getEinheit().getName();
        }

        return bestand == null ? null : "Stück";
    }


    static String kategorieVon(BestandEntity bestand) {
        if (bestand instanceof FrischBestand frisch && frisch.getKategorie() != null) {
            return frisch.getKategorie().getName();
        }

        if (bestand instanceof Produkt produkt && produkt.getKategorie() != null) {
            return produkt.getKategorie().getName();
        }

        return bestand == null ? null : "Brot";
    }


    static <T> Collection<T> liste(Collection<T> werte) {
        return werte == null ? List.of() : werte;
    }


    static double runden2(double wert) {
        return Math.round(wert * 100.0) / 100.0;
    }


    static double runden3(double wert) {
        return Math.round(wert * 1000.0) / 1000.0;
    }
}
