package de.dhbw.foodcoop.warehouse.domain.entities;

/**
 * Produkt mit einem Platz in seiner Liste (1 = oben), z.B. wie in der
 * Liste des Händlers.
 */
public interface Sortierbar {

    String getId();

    String getName();

    Integer getSortierung();

    void setSortierung(Integer sortierung);
}
