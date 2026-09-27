package de.dhbw.foodcoop.warehouse.adapters.representations;

import java.util.List;

public final class ProduktRepresentation extends BestandRepresentation {
    private final KategorieRepresentation kategorie;
    private final LagerbestandRepresentation lagerbestand;
    
    private String produktBezeichnung;

    /** Platz in der Liste (1 = oben); beim Anlegen optional = gewünschter Platz. */
    private Integer sortierung;

    /** Bestand nach Preisen, älteste Lieferung zuerst (wird zuerst verkauft). */
    private List<LagerChargeRepresentation> chargen;

    public ProduktRepresentation(String id, String name, String produktBezeichnung, KategorieRepresentation kategorie, LagerbestandRepresentation lagerbestand, float preis) {
    	super(id, name, lagerbestand == null ? false : lagerbestand.getIstLagerbestand() > 0 ? true : false, preis);

        this.kategorie = kategorie;
        this.produktBezeichnung = produktBezeichnung;
        this.lagerbestand = lagerbestand;
    }



    public KategorieRepresentation getKategorie() {
        return kategorie;
    }

    public LagerbestandRepresentation getLagerbestand() {
        return lagerbestand;
    }




	public Integer getSortierung() {
		return sortierung;
	}



	public void setSortierung(Integer sortierung) {
		this.sortierung = sortierung;
	}



	public List<LagerChargeRepresentation> getChargen() {
		return chargen;
	}



	public void setChargen(List<LagerChargeRepresentation> chargen) {
		this.chargen = chargen;
	}



	public String getProduktBezeichnung() {
		return produktBezeichnung;
	}



	public void setProduktBezeichnung(String produktBezeichnung) {
		this.produktBezeichnung = produktBezeichnung;
	}



	@Override
    public String toString() {
        return "Produkt{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", kategorie='" + kategorie + '\'' +
                ", lagerbestandRepresentation=" + lagerbestand +
                '}';
    }
}
