package de.dhbw.foodcoop.warehouse.adapters.representations;

public class FrischBestandRepresentation extends BestandRepresentation{

    private String herkunftsland;
    private float gebindegroesse;
    private EinheitRepresentation einheit;
    private KategorieRepresentation kategorie;
    private String verband;
    private boolean spezialfallBestelleinheit;

    /** Platz in der Liste (1 = oben); beim Anlegen optional = gewünschter Platz. */
    private Integer sortierung;
    
    public FrischBestandRepresentation(String id, String name, boolean verfuegbarkeit, String herkunftsland, float gebindegroesse, EinheitRepresentation einheit, KategorieRepresentation kategorie, float preis, String verband, boolean spezialfallBestelleinheit) {
    	super(id, name, verfuegbarkeit, preis);
        this.herkunftsland = herkunftsland;
        this.gebindegroesse = gebindegroesse;
        this.einheit = einheit;
        this.kategorie = kategorie;
        this.verband = verband;
        this.spezialfallBestelleinheit = spezialfallBestelleinheit;
    }


    public Integer getSortierung() {
        return sortierung;
    }


    public void setSortierung(Integer sortierung) {
        this.sortierung = sortierung;
    }


    public String getVerband() {
		return verband;
	}


	public String getHerkunftsland() {
        return herkunftsland;
    }

    public float getGebindegroesse() {
        return gebindegroesse;
    }

    public EinheitRepresentation getEinheit() {
        return einheit;
    }

    public KategorieRepresentation getKategorie() {
        return kategorie;
    }

	public boolean isSpezialfallBestelleinheit() {
		return spezialfallBestelleinheit;
	}
    

}

