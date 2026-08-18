package de.dhbw.foodcoop.warehouse.adapters.representations;

public final class KategorieRepresentation {
    private final String name;
    private String id;
    private boolean mixable;

    public KategorieRepresentation(String id, String name, Boolean mixable) {
        this.id = id;
        this.mixable = Boolean.TRUE.equals(mixable);
        this.name = name;
    }
    
    
    

	public boolean isMixable() {
		return mixable;
	}




	public void setMixable(boolean isMixable) {
		this.mixable = isMixable;
	}




	public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
}
