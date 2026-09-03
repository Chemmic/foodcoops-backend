package de.dhbw.foodcoop.warehouse.adapters.representations.mappers;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import de.dhbw.foodcoop.warehouse.adapters.representations.BestellungRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.BrotBestellungRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.FrischBestellungRepresentation;
import de.dhbw.foodcoop.warehouse.application.brot.BrotBestandService;
import de.dhbw.foodcoop.warehouse.application.frischbestellung.FrischBestandService;
import de.dhbw.foodcoop.warehouse.domain.entities.BestellungEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestand;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestand;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestellung;
import de.dhbw.foodcoop.warehouse.domain.exceptions.BrotBestandNotFoundException;
import de.dhbw.foodcoop.warehouse.domain.exceptions.FrischBestandNotFoundException;

@Component
public class RepresentationToBestellungMapper
		implements Function<BestellungRepresentation, BestellungEntity> {

	private final BrotBestandService brotBestandService;
	private final FrischBestandService frischBestandService;

	public RepresentationToBestellungMapper(
			BrotBestandService brotBestandService,
			FrischBestandService frischBestandService) {

		this.brotBestandService = brotBestandService;
		this.frischBestandService = frischBestandService;
	}

	// =========================================================================
	// Update
	// =========================================================================

	public BestellungEntity update(
			BestellungEntity oldBestellung,
			BestellungRepresentation newBestellung) {

		// ---------------------------------------------------------------------
		// Brot
		// ---------------------------------------------------------------------

		if (newBestellung instanceof BrotBestellungRepresentation bbr) {

			BrotBestand brotBestand =
					brotBestandService
							.findById(
									bbr.getBrotbestand().getId()
							)
							.orElseThrow(
									() ->
											new BrotBestandNotFoundException(
													bbr.getBrotbestand().getId()
											)
							);

			BrotBestellung updated =
					new BrotBestellung(
							oldBestellung.getId(),
							bbr.getPersonId(),
							brotBestand,
							bbr.getBestellmenge(),

							/*
							 * Wichtig:
							 *
							 * Das ursprüngliche Datum behalten.
							 * Ein PUT darf die Bestellung nicht
							 * plötzlich zu einer neuen Bestellung machen.
							 */
							oldBestellung.getDatum()
					);

			/*
			 * Extrem wichtig:
			 *
			 * Die Bestellung gehört weiterhin zur
			 * ursprünglichen Bestellrunde.
			 */
			updated.setDeadline(
					oldBestellung.getDeadline()
			);

			/*
			 * Falls BrotBestellungRepresentation ebenfalls
			 * das done-Feld besitzt.
			 */
			updated.setDone(
					bbr.isDone()
			);

			return updated;
		}

		// ---------------------------------------------------------------------
		// Frisch
		// ---------------------------------------------------------------------

		if (newBestellung instanceof FrischBestellungRepresentation fbr) {

			FrischBestand frischBestand =
					frischBestandService
							.findById(
									fbr.getFrischbestand().getId()
							)
							.orElseThrow(
									() ->
											new FrischBestandNotFoundException(
													fbr.getFrischbestand().getId()
											)
							);

			FrischBestellung updated =
					new FrischBestellung(
							oldBestellung.getId(),
							fbr.getPersonId(),
							frischBestand,
							fbr.getBestellmenge(),

							oldBestellung.getDatum(),

							fbr.isDone()
					);

			/*
			 * Ursprüngliche Bestellrunde behalten.
			 */
			updated.setDeadline(
					oldBestellung.getDeadline()
			);

			return updated;
		}

		throw new IllegalArgumentException(
				"Unbekannter Bestellungstyp: "
						+ newBestellung.getClass().getName()
		);
	}

	// =========================================================================
	// Create
	// =========================================================================

	@Override
	public BestellungEntity apply(
			BestellungRepresentation representation) {

		// ---------------------------------------------------------------------
		// Brot
		// ---------------------------------------------------------------------

		if (representation instanceof BrotBestellungRepresentation bbr) {

			BrotBestand brotBestand =
					brotBestandService
							.findById(
									bbr.getBrotbestand().getId()
							)
							.orElseThrow(
									() ->
											new BrotBestandNotFoundException(
													bbr.getBrotbestand().getId()
											)
							);

			BrotBestellung bestellung =
					new BrotBestellung(
							bbr.getId(),
							bbr.getPersonId(),
							brotBestand,
							bbr.getBestellmenge(),
							bbr.getDatum()
					);

			bestellung.setDone(
					bbr.isDone()
			);

			/*
			 * Deadline absichtlich NICHT setzen.
			 *
			 * Das übernimmt BrotBestellungService.save()
			 * mit deadlineService.last().
			 */

			return bestellung;
		}

		// ---------------------------------------------------------------------
		// Frisch
		// ---------------------------------------------------------------------

		if (representation instanceof FrischBestellungRepresentation fbr) {

			FrischBestand frischBestand =
					frischBestandService
							.findById(
									fbr.getFrischbestand().getId()
							)
							.orElseThrow(
									() ->
											new FrischBestandNotFoundException(
													fbr.getFrischbestand().getId()
											)
							);


			return new FrischBestellung(
					fbr.getId(),
					fbr.getPersonId(),
					frischBestand,
					fbr.getBestellmenge(),
					fbr.getDatum(),
					fbr.isDone()
			);

			/*

			 * FrischBestellungService.save()
			 * setzt die aktuelle Deadline.
			 */
		}

		throw new IllegalArgumentException(
				"Unbekannter Bestellungstyp: "
						+ representation.getClass().getName()
		);
	}
}