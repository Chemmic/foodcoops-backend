package de.dhbw.foodcoop.warehouse.adapters.representations;

import java.time.LocalDateTime;

public class BrotBestellungRepresentation extends BestellungRepresentation {
    private BrotBestandRepresentation brotbestand;

    public BrotBestellungRepresentation(String id, String personId, BrotBestandRepresentation brotbestand, double bestellmenge, LocalDateTime datum, Boolean done) {
     super(id, personId, datum, bestellmenge, Boolean.TRUE.equals(done));
        this.brotbestand = brotbestand;
    }

//    public FrischBestellungRepresentation(String id, String person_id, FrischBestandRepresentation frischbestand, long bestellmenge) {
//        this.id = id;
//        this.person_id = person_id;
//        this.frischbestand = frischbestand;
//        this.bestellmenge = bestellmenge;
//    }




    public BrotBestandRepresentation getBrotbestand(){
        return brotbestand;
    }

    public void setBrotbestand(BrotBestandRepresentation brotbestand){
        this.brotbestand = brotbestand;
    }

	public void setId(String id) {
		// TODO Auto-generated method stub
		super.id = id;
		
	}


}
