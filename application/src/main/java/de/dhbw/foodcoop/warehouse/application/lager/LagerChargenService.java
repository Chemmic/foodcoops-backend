package de.dhbw.foodcoop.warehouse.application.lager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import de.dhbw.foodcoop.warehouse.domain.entities.LagerCharge;
import de.dhbw.foodcoop.warehouse.domain.entities.Produkt;
import de.dhbw.foodcoop.warehouse.domain.repositories.LagerChargeRepository;

/**
 * ============================================================================
 * Lagerware zu unterschiedlichen Preisen (Chargen, FIFO)
 * ============================================================================
 *
 * Jede Lieferung ist eine Charge mit Menge und Preis. Beim Einkauf wird die
 * älteste Charge zuerst verbraucht – Mitglieder zahlen also erst den alten
 * Preis, dann den neuen.
 *
 * Maßgeblich für die Menge bleibt der Ist-Lagerbestand des Produkts. Weicht
 * die Summe der Chargen davon ab (Altbestand von vor den Chargen, Änderungen
 * an anderer Stelle), wird abgeglichen:
 *
 *   zu wenig in Chargen -> Rest als ältester Bestand zum aktuellen Preis
 *   zu viel in Chargen  -> älteste Chargen werden gekürzt
 *
 * ============================================================================
 */
@Service
public class LagerChargenService {

    private static final double EPSILON = 0.0005;

    /** Datum für Altbestand ohne bekannte Lieferung: vor allen echten Chargen. */
    private static final LocalDateTime ALTBESTAND = LocalDateTime.of(2000, 1, 1, 0, 0);


    private final LagerChargeRepository repository;


    public LagerChargenService(LagerChargeRepository repository) {
        this.repository = repository;
    }


    // =========================================================================
    // Ergebnis einer Entnahme
    // =========================================================================

    public record Teil(double menge, BigDecimal preis) {
    }

    public record Entnahme(double menge, BigDecimal betrag, List<Teil> teile) {
    }


    // =========================================================================
    // Lesen (ohne zu speichern)
    // =========================================================================

    /** Chargen eines Produkts, passend zum Ist-Bestand, älteste zuerst. */
    public List<LagerCharge> chargen(Produkt produkt) {
        return abgleichen(
                repository.vonProdukt(produkt.getId()),
                produkt.getId(),
                ist(produkt),
                produkt.getPreis()
        );
    }


    /** Chargen aller Produkte (eine Abfrage), passend zum jeweiligen Ist-Bestand. */
    public Map<String, List<LagerCharge>> chargen(Collection<Produkt> produkte) {
        Map<String, List<LagerCharge>> jeProdukt =
                repository.alle()
                        .stream()
                        .collect(Collectors.groupingBy(LagerCharge::getProduktId));

        Map<String, List<LagerCharge>> ergebnis = new HashMap<>();

        for (Produkt produkt : produkte) {
            ergebnis.put(produkt.getId(), abgleichen(
                    jeProdukt.getOrDefault(produkt.getId(), List.of()),
                    produkt.getId(),
                    ist(produkt),
                    produkt.getPreis()
            ));
        }

        return ergebnis;
    }


    /**
     * Was kostet eine Menge? Älteste Chargen zuerst; reicht der Bestand
     * nicht, wird der Rest zum Ersatzpreis berechnet.
     */
    public static Entnahme preisFuer(List<LagerCharge> chargen, double menge, BigDecimal ersatzpreis) {
        List<Teil> teile = new ArrayList<>();
        double rest = menge;

        for (LagerCharge charge : chargen) {
            if (rest <= EPSILON) {
                break;
            }

            double genommen = Math.min(rest, charge.getMenge());

            if (genommen > EPSILON) {
                teile.add(new Teil(genommen, charge.getPreis()));
                rest -= genommen;
            }
        }

        if (rest > EPSILON) {
            teile.add(new Teil(rest, ersatzpreis));
        }

        BigDecimal betrag =
                teile.stream()
                        .map(t -> oderNull(t.preis()).multiply(BigDecimal.valueOf(t.menge())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);

        return new Entnahme(menge, betrag, teile);
    }


    // =========================================================================
    // Ändern
    // =========================================================================

    /**
     * Einkauf: Menge entnehmen, älteste Chargen zuerst.
     *
     * @param istVorher Ist-Bestand vor der Entnahme (aus der Datenbank)
     * @return Betrag und Aufteilung nach Preisen
     */
    public Entnahme entnehmen(String produktId, double istVorher, BigDecimal aktuellerPreis, double menge) {
        List<LagerCharge> chargen = synchronisieren(produktId, istVorher, aktuellerPreis);

        Entnahme entnahme = preisFuer(chargen, menge, aktuellerPreis);

        verbrauchen(chargen, menge);

        return entnahme;
    }


    /**
     * Bestand wurde geändert (Lager bearbeiten oder neue Ware eingelagert).
     *
     *   mehr Bestand       -> neue Charge zum neuen Preis (alte behalten ihren);
     *                         gleicher Preis wie die jüngste Charge -> wird dazugezählt
     *   weniger Bestand    -> älteste Chargen werden gekürzt (z.B. Inventur)
     *   preisFuerBestand   -> alle vorhandenen Chargen bekommen den neuen Preis
     */
    public void bestandGeaendert(
            String produktId,
            double istVorher,
            BigDecimal preisVorher,
            double istNachher,
            BigDecimal preisNachher,
            boolean preisFuerBestand
    ) {
        List<LagerCharge> chargen = synchronisieren(produktId, istVorher, preisVorher);

        double differenz = istNachher - istVorher;

        if (differenz > EPSILON) {
            LagerCharge juengste = chargen.isEmpty() ? null : chargen.get(chargen.size() - 1);

            if (juengste != null && gleicherPreis(juengste.getPreis(), preisNachher)) {
                // Gleicher Preis: einfach dazuzählen statt einer neuen Charge
                juengste.setMenge(runden(juengste.getMenge() + differenz));
                repository.speichern(juengste);
            } else {
                LagerCharge neu = new LagerCharge(produktId, differenz, preisNachher, LocalDateTime.now());
                chargen.add(repository.speichern(neu));
            }
        } else if (differenz < -EPSILON) {
            verbrauchen(chargen, -differenz);
        }

        if (preisFuerBestand && preisNachher != null) {
            for (LagerCharge charge : chargen) {
                if (charge.getMenge() > EPSILON && preisNachher.compareTo(oderNull(charge.getPreis())) != 0) {
                    charge.setPreis(preisNachher);
                    repository.speichern(charge);
                }
            }

            zusammenfassen(chargen, true);
        }
    }


    /** Chargen eines gelöschten Produkts entfernen. */
    public void loeschen(String produktId) {
        repository.vonProdukt(produktId).forEach(repository::loeschen);
    }


    /**
     * Einmalig beim Start: vorhandener Bestand ohne Chargen wird zu einer
     * Charge zum aktuellen Preis.
     *
     * @return Anzahl angelegter Chargen
     */
    public int migrieren(Collection<Produkt> produkte) {
        Map<String, List<LagerCharge>> vorhanden =
                repository.alle()
                        .stream()
                        .collect(Collectors.groupingBy(LagerCharge::getProduktId));

        int angelegt = 0;

        for (Produkt produkt : produkte) {
            double ist = ist(produkt);

            if (ist > EPSILON && !vorhanden.containsKey(produkt.getId())) {
                repository.speichern(new LagerCharge(produkt.getId(), ist, produkt.getPreis(), ALTBESTAND));
                angelegt++;
            }
        }

        return angelegt;
    }


    // =========================================================================
    // Intern
    // =========================================================================

    /** Gespeicherte Chargen an den Ist-Bestand anpassen und speichern. */
    private List<LagerCharge> synchronisieren(String produktId, double ist, BigDecimal preis) {
        List<LagerCharge> chargen = new ArrayList<>(repository.vonProdukt(produktId));

        double summe = summe(chargen);

        if (summe < ist - EPSILON) {
            LagerCharge alt = new LagerCharge(produktId, ist - summe, preis, ALTBESTAND);
            chargen.add(0, repository.speichern(alt));
        } else if (summe > ist + EPSILON) {
            verbrauchen(chargen, summe - Math.max(0, ist));
        }

        return zusammenfassen(chargen, true);
    }


    /**
     * Aufeinanderfolgende Chargen zum gleichen Preis zu einer zusammenfassen
     * (die ältere bleibt). Mit speichern=false nur für die Anzeige.
     */
    private List<LagerCharge> zusammenfassen(List<LagerCharge> chargen, boolean speichern) {
        List<LagerCharge> ergebnis = new ArrayList<>();

        for (LagerCharge charge : chargen) {
            LagerCharge vorige = ergebnis.isEmpty() ? null : ergebnis.get(ergebnis.size() - 1);

            if (vorige != null && gleicherPreis(vorige.getPreis(), charge.getPreis())) {
                vorige.setMenge(runden(vorige.getMenge() + charge.getMenge()));

                if (speichern) {
                    repository.speichern(vorige);
                    repository.loeschen(charge);
                }
            } else {
                ergebnis.add(charge);
            }
        }

        chargen.clear();
        chargen.addAll(ergebnis);
        return chargen;
    }


    private static boolean gleicherPreis(BigDecimal a, BigDecimal b) {
        return oderNull(a).compareTo(oderNull(b)) == 0;
    }


    /** Älteste Chargen zuerst verbrauchen; leere werden gelöscht. */
    private void verbrauchen(List<LagerCharge> chargen, double menge) {
        double rest = menge;

        for (LagerCharge charge : new ArrayList<>(chargen)) {
            if (rest <= EPSILON) {
                break;
            }

            double genommen = Math.min(rest, charge.getMenge());
            rest -= genommen;
            charge.setMenge(runden(charge.getMenge() - genommen));

            if (charge.getMenge() <= EPSILON) {
                repository.loeschen(charge);
                chargen.remove(charge);
            } else {
                repository.speichern(charge);
            }
        }
    }


    /** Abgleich wie synchronisieren – aber nur als Kopie, ohne zu speichern. */
    List<LagerCharge> abgleichen(List<LagerCharge> gespeichert, String produktId, double ist, BigDecimal preis) {
        List<LagerCharge> chargen = new ArrayList<>();

        for (LagerCharge c : gespeichert) {
            chargen.add(new LagerCharge(c.getId(), c.getProduktId(), c.getMenge(), c.getPreis(), c.getEingelagertAm()));
        }

        double summe = summe(chargen);

        if (summe < ist - EPSILON) {
            chargen.add(0, new LagerCharge("altbestand-" + produktId, produktId, runden(ist - summe), preis, ALTBESTAND));
        } else if (summe > ist + EPSILON) {
            double rest = summe - Math.max(0, ist);

            for (LagerCharge c : new ArrayList<>(chargen)) {
                if (rest <= EPSILON) {
                    break;
                }

                double genommen = Math.min(rest, c.getMenge());
                rest -= genommen;
                c.setMenge(runden(c.getMenge() - genommen));

                if (c.getMenge() <= EPSILON) {
                    chargen.remove(c);
                }
            }
        }

        return zusammenfassen(chargen, false);
    }


    private static double summe(List<LagerCharge> chargen) {
        return chargen.stream().mapToDouble(LagerCharge::getMenge).sum();
    }


    private static double ist(Produkt produkt) {
        return produkt.getLagerbestand() == null || produkt.getLagerbestand().getIstLagerbestand() == null
                ? 0
                : produkt.getLagerbestand().getIstLagerbestand();
    }


    private static double runden(double wert) {
        return Math.round(wert * 1000.0) / 1000.0;
    }


    private static BigDecimal oderNull(BigDecimal wert) {
        return wert == null ? BigDecimal.ZERO : wert;
    }
}
