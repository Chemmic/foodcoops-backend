package de.dhbw.foodcoop.warehouse.domain.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import de.dhbw.foodcoop.warehouse.domain.shopping.BuyType;

@Entity
public class BestellungBuyEntity implements BuyType{

	
	@Id
	private String id;
	
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "bestellung_id")
	private BestellungEntity bestellung;
	
	@Column
	private double amount;
	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}



	public BestellungBuyEntity(String id, BestellungEntity bestellung, double amount) {
		super();
		this.id = id;
		this.bestellung = bestellung;
		this.amount = amount;
	}

	public BestellungEntity getBestellung() {
		return bestellung;
	}

	public void setBestellung(BestellungEntity bestellung) {
		this.bestellung = bestellung;
	}

	public BestellungBuyEntity() {
		// TODO Auto-generated constructor stub
	}


	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}


	

	
	
}
