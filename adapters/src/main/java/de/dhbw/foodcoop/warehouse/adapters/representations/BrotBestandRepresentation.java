package de.dhbw.foodcoop.warehouse.adapters.representations;

public class BrotBestandRepresentation extends BestandRepresentation{

    private double gewicht;

	private AllergenInfoRepresentation allergenInfo;

	/** Platz in der Liste (1 = oben); beim Anlegen optional = gewünschter Platz. */
	private Integer sortierung;

	public Integer getSortierung() {
		return sortierung;
	}

	public void setSortierung(Integer sortierung) {
		this.sortierung = sortierung;
	}
	
	public BrotBestandRepresentation(String id, String name, boolean verfuegbarkeit, float preis, double gewicht, AllergenInfoRepresentation allergenInfo) {
		super(id, name, verfuegbarkeit, preis);
		this.gewicht = gewicht;
		this.allergenInfo = allergenInfo;
	}

	public AllergenInfoRepresentation getAllergenInfo() {
		return allergenInfo;
	}

	public double getGewicht() {
		return gewicht;
	}
}

