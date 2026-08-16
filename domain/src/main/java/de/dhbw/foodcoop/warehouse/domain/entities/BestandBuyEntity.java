package de.dhbw.foodcoop.warehouse.domain.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import de.dhbw.foodcoop.warehouse.domain.shopping.BuyType;

@Entity
public class BestandBuyEntity implements BuyType{

	
	@Id
	private String id;
	
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "bestand_id")
	private Produkt bestand;
	
	@Column
	private double amount;
	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Produkt getBestand() {
		return bestand;
	}

	public void setBestand(Produkt bestand) {
		this.bestand = bestand;
	}

	public BestandBuyEntity() {
		// TODO Auto-generated constructor stub
	}
	public BestandBuyEntity(String id, Produkt bestand, double amount) {
		super();
		this.id = id;
		this.bestand = bestand;
		this.amount = amount;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}


	

	
	
}
