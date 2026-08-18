package de.dhbw.foodcoop.warehouse.domain.values;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Embeddable
public class AllergenInfo {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "brotbestand_getreide",
            joinColumns = @JoinColumn(name = "brotbestand_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "getreide")
    private Set<GetreideTyp> getreide = new HashSet<>();

    @Column(name = "allergen_eier", nullable = false)
    private boolean eier;

    @Column(name = "allergen_milch", nullable = false)
    private boolean milch;

    @Column(name = "allergen_sesam", nullable = false)
    private boolean sesam;

    @Column(name = "allergen_schalenfruechte", nullable = false)
    private boolean schalenfruechte;

    @Column(name = "allergen_sellerie", nullable = false)
    private boolean sellerie;

    @Column(name = "allergen_soja", nullable = false)
    private boolean soja;

    @Column(name = "allergen_hinweis")
    private String hinweis;

    protected AllergenInfo() {
        // Für JPA
    }

    public AllergenInfo(
            Set<GetreideTyp> getreide,
            boolean eier,
            boolean milch,
            boolean sesam,
            boolean schalenfruechte,
            boolean sellerie,
            boolean soja,
            String hinweis
    ) {
        this.getreide = new HashSet<>(getreide);
        this.eier = eier;
        this.milch = milch;
        this.sesam = sesam;
        this.schalenfruechte = schalenfruechte;
        this.sellerie = sellerie;
        this.soja = soja;
        this.hinweis = hinweis;
    }

    public static AllergenInfo empty() {
        return new AllergenInfo(
                Set.of(),
                false,
                false,
                false,
                false,
                false,
                false,
                null
        );
    }

    public Set<GetreideTyp> getGetreide() {
        return Set.copyOf(getreide);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AllergenInfo that)) {
            return false;
        }

        return eier == that.eier
                && milch == that.milch
                && sesam == that.sesam
                && schalenfruechte == that.schalenfruechte
                && sellerie == that.sellerie
                && soja == that.soja
                && Objects.equals(getreide, that.getreide)
                && Objects.equals(hinweis, that.hinweis);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getreide,
                eier,
                milch,
                sesam,
                schalenfruechte,
                sellerie,
                soja,
                hinweis
        );
    }

    @Override
    public String toString() {
        return "AllergenInfo{" +
                "getreide=" + getreide +
                ", eier=" + eier +
                ", milch=" + milch +
                ", sesam=" + sesam +
                ", schalenfruechte=" + schalenfruechte +
                ", sellerie=" + sellerie +
                ", soja=" + soja +
                ", hinweis='" + hinweis + '\'' +
                '}';
    }
}