package de.dhbw.foodcoop.warehouse.domain.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import de.dhbw.foodcoop.warehouse.domain.shopping.BuyType;

@Entity
public class TooMuchBuyEntity implements BuyType {
	@Id
	private String id;
	
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "discrepancy_id")
	private DiscrepancyEntity discrepancy;
	
	
	@Column
	private double amount;


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public DiscrepancyEntity getDiscrepancy() {
		return discrepancy;
	}


	public void setDiscrepancy(DiscrepancyEntity discrepancy) {
		this.discrepancy = discrepancy;
	}


	public double getAmount() {
		return amount;
	}


	public void setAmount(double amount) {
		this.amount = amount;
	}
	
	public TooMuchBuyEntity() {
		// TODO Auto-generated constructor stub
	}


	public TooMuchBuyEntity(String id, DiscrepancyEntity discrepancy, double amount) {
		super();
		this.id = id;
		this.discrepancy = discrepancy;
		this.amount = amount;
	}
	
	
}
