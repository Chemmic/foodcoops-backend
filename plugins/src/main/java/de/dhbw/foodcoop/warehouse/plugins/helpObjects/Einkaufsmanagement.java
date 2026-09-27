package de.dhbw.foodcoop.warehouse.plugins.helpObjects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Empfänger der Mail ans Einkaufsmanagement. Das Frontend schickt die
 * Benutzer der Rolle (id, username, email, Name) – gebraucht werden
 * email, username und firstName (für die Anrede).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Einkaufsmanagement {

	private String email;
	private String username;
	private String firstName;

	public Einkaufsmanagement() {
	}

	public Einkaufsmanagement(String email, String username) {
		this.email = email;
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/** Vorname, sonst Benutzername. */
	public String getAnrede() {
		if (firstName != null && !firstName.isBlank()) {
			return firstName.trim();
		}

		return username != null ? username : "";
	}
}
