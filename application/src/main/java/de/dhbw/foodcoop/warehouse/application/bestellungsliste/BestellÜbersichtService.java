package de.dhbw.foodcoop.warehouse.application.bestellungsliste;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import de.dhbw.foodcoop.warehouse.application.admin.ConfigurationService;
import de.dhbw.foodcoop.warehouse.application.brot.BrotBestellungService;
import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineSavedEvent;
import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.application.frischbestellung.FrischBestellungService;
import de.dhbw.foodcoop.warehouse.application.gebindemanagement.GebindemanagementService;
import de.dhbw.foodcoop.warehouse.domain.entities.BestellUebersicht;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.Deadline;
import de.dhbw.foodcoop.warehouse.domain.entities.DiscrepancyEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.Kategorie;
import de.dhbw.foodcoop.warehouse.domain.repositories.BestellÜbersichtRepository;

@Service
public class BestellÜbersichtService {

	private final BestellÜbersichtRepository repo;
	private final GebindemanagementService gebindeService;
	private final DeadlineService deadlineService;
	private final FrischBestellungService frischService;
	private final BrotBestellungService brotService;
	private final ConfigurationService cfgService;

	public BestellÜbersichtService(
			BestellÜbersichtRepository repo,
			GebindemanagementService gebindeService,
			DeadlineService deadlineService,
			FrischBestellungService frischService,
			BrotBestellungService brotService,
			ConfigurationService cfgService) {

		this.repo = repo;
		this.gebindeService = gebindeService;
		this.deadlineService = deadlineService;
		this.frischService = frischService;
		this.brotService = brotService;
		this.cfgService = cfgService;
	}

	@EventListener
	public void onDeadlineSaved(DeadlineSavedEvent event) {
		createList();
	}

	public BestellUebersicht getLastUebersicht() {
		Deadline deadline = deadlineService.last();
		return repo.findeMitDeadline(deadline);
	}

	public BestellUebersicht getByDeadline(Deadline deadline) {
		return repo.findeMitDeadline(deadline);
	}

	public void deleteById(String id) {
		repo.deleteById(id);
	}

	public BestellUebersicht findById(String id) {
		return repo.findeMitId(id)
				.orElseThrow();
	}

	public BestellUebersicht update(
			BestellUebersicht bestellUebersicht) {

		return repo.speichern(bestellUebersicht);
	}

	public Optional<BestellUebersicht> createList() {

		BestellUebersicht bestellÜbersicht =
				new BestellUebersicht();

		bestellÜbersicht.setId(
				UUID.randomUUID().toString());

		double threshold = cfgService.getConfig()
				.orElseThrow()
				.getThreshold();

		Deadline neuErstellte =
				deadlineService.last();

		bestellÜbersicht.setToOrderWithinDeadline(
				neuErstellte);

		Optional<Deadline> date1 =
				deadlineService.getByPosition(0);

		Optional<Deadline> date2 =
				deadlineService.getByPosition(1);

		if (date1.isEmpty() || date2.isEmpty()) {
			return Optional.empty();
		}

		List<DiscrepancyEntity> discrepancy =
				new ArrayList<>();

		Set<Kategorie> orderedCategories =
				new HashSet<>();

		List<FrischBestellung> frischBestellungen =
				frischService.findByDateBetween(
						date1.get().getDatum(),
						date2.get().getDatum());

		for (FrischBestellung bestellung
				: frischBestellungen) {

			if (bestellung.getFrischbestand() != null
					&& bestellung.getFrischbestand()
					.getKategorie() != null) {

				orderedCategories.add(
						bestellung.getFrischbestand()
								.getKategorie());
			}
		}

		for (Kategorie kategorie
				: orderedCategories) {

			List<DiscrepancyEntity> result;

			if (kategorie.isMixable()) {

				result =
						gebindeService
								.getDiscrepancyListForMixableCategorie(
										kategorie,
										threshold);

			} else {

				result =
						gebindeService
								.getDiscrepancyForNotMixableOrder(
										kategorie,
										threshold);
			}

			if (result != null) {
				discrepancy.addAll(result);
			}
		}

		bestellÜbersicht.setDiscrepancy(
				discrepancy);

		List<BrotBestellung> brotBestellungen =
				brotService.findByDateBetween(
						date1.get().getDatum(),
						date2.get().getDatum());

		bestellÜbersicht.setBrotBestellung(
				brotBestellungen);

		return Optional.of(
				repo.speichern(bestellÜbersicht));
	}
}