package de.dhbw.foodcoop.warehouse.adapters.representations;

public class BestandBuyRepresentation {

	private String id;
	private ProduktRepresentation bestand;
	private double amount;
	/** Tatsächlich bezahlter Betrag (ältere Lieferungen zuerst); null bei alten Einkäufen. */
	private Double betrag;
	public BestandBuyRepresentation(String id, ProduktRepresentation bestand, double amount) {
		super();
		this.id = id;
		this.bestand = bestand;
		this.amount = amount;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public ProduktRepresentation getBestand() {
		return bestand;
	}
	public void setBestand(ProduktRepresentation bestand) {
		this.bestand = bestand;
	}
	public Double getBetrag() {
		return betrag;
	}
	public void setBetrag(Double betrag) {
		this.betrag = betrag;
	}
	public double getAmount() {
		return amount;
	}
	public void setAmount(double amount) {
		this.amount = amount;
	}
	
	
}
