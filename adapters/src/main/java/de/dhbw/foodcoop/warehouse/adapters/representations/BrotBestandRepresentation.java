package de.dhbw.foodcoop.warehouse.adapters.representations;

public class BrotBestandRepresentation extends BestandRepresentation{

    private double gewicht;

	private AllergenInfoRepresentation allergenInfo;
	
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

