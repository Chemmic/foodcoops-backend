package de.dhbw.foodcoop.warehouse.plugins.rest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import de.dhbw.foodcoop.warehouse.application.bestellungsliste.BestellÜbersichtService;
import de.dhbw.foodcoop.warehouse.application.diskrepanz.DiscrepancyService;
import de.dhbw.foodcoop.warehouse.application.gebindemanagement.GebindemanagementService;
import de.dhbw.foodcoop.warehouse.domain.entities.BestellUebersicht;
import de.dhbw.foodcoop.warehouse.domain.entities.DiscrepancyEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestand;
import de.dhbw.foodcoop.warehouse.plugins.helpObjects.CategoryAndPercentHolder;

@RestController
public class DiscrepancyController {

	private final DiscrepancyService discrepancyService;
	private final GebindemanagementService service;
	private final BestellÜbersichtService bestellService;

	public DiscrepancyController(
			DiscrepancyService discrepancyService,
			GebindemanagementService service,
			BestellÜbersichtService bestellService) {

		this.discrepancyService = discrepancyService;
		this.service = service;
		this.bestellService = bestellService;
	}

	@PostMapping("/gebinde/discrepancy/listForMixableCategorie")
	public List<DiscrepancyEntity> getDiscrepancyListForMixableCategorie(
			@RequestBody CategoryAndPercentHolder categoryAndPercent) {

		return service.getDiscrepancyListForMixableCategorie(
				categoryAndPercent.getKategorie(),
				categoryAndPercent.getPercentage());
	}

	@PostMapping("/gebinde/discrepancy/forNonMixableOrder")
	public List<DiscrepancyEntity> getDiscrepancyForNonMixableOrder(
			@RequestBody CategoryAndPercentHolder orderAndPercentHolder) {

		return service.getDiscrepancyForNotMixableOrder(
				orderAndPercentHolder.getKategorie(),
				orderAndPercentHolder.getPercentage());
	}

	@PostMapping("/gebinde/discrepancy/autoDecide")
	public List<DiscrepancyEntity> getDiscrepancyForBoth(
			@RequestBody CategoryAndPercentHolder categoryAndPercent) {

		if (categoryAndPercent.getKategorie().isMixable()) {
			return service.getDiscrepancyListForMixableCategorie(
					categoryAndPercent.getKategorie(),
					categoryAndPercent.getPercentage());
		}

		return service.getDiscrepancyForNotMixableOrder(
				categoryAndPercent.getKategorie(),
				categoryAndPercent.getPercentage());
	}

	@PutMapping("/gebinde/discrepancy/update/tooMuchTooLittle/{id}")
	public ResponseEntity<DiscrepancyEntity> updatedDiscrepancy(
			@PathVariable String id,
			@RequestBody String body) {

		float zuVielZuWenig;

		try {
			zuVielZuWenig = Float.parseFloat(body);
		} catch (NumberFormatException e) {
			return ResponseEntity.badRequest().build();
		}

		Optional<DiscrepancyEntity> discrepancy = discrepancyService.findById(id);

		if (discrepancy.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		DiscrepancyEntity entity = discrepancy.get();
		entity.setZuVielzuWenig(zuVielZuWenig);

		return ResponseEntity.ok(discrepancyService.save(entity));
	}

	@PutMapping("/gebinde/discrepancy/update/gebindeAmountToOrder/{id}")
	public ResponseEntity<DiscrepancyEntity> updatedZuBestellen(
			@PathVariable String id,
			@RequestBody String body) {

		double zuBestellendeGebinde;

		try {
			zuBestellendeGebinde = Double.parseDouble(body);
		} catch (NumberFormatException e) {
			return ResponseEntity.badRequest().build();
		}

		Optional<DiscrepancyEntity> discrepancy = discrepancyService.findById(id);

		if (discrepancy.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		DiscrepancyEntity entity = discrepancy.get();
		entity.setZuBestellendeGebinde(zuBestellendeGebinde);

		if (entity.getBestand() instanceof FrischBestand bestand) {
			entity.setZuVielzuWenig(
					(float) entity.getZuBestellendeGebinde()
							* bestand.getGebindegroesse()
							- entity.getGewollteMenge());

			return ResponseEntity.ok(discrepancyService.save(entity));
		}

		return ResponseEntity.unprocessableEntity().build();
	}

	@PostMapping("/gebinde/discrepancy/add")
	public ResponseEntity<BestellUebersicht> addDiscrepancyToLastOrderList(
			@RequestBody DiscrepancyEntity body) {

		if (body.getId() == null
				|| body.getId().isBlank()
				|| body.getId().equalsIgnoreCase("undefined")) {

			body.setId(UUID.randomUUID().toString());
		}

		if (body.getBestand() == null) {
			return ResponseEntity.noContent().build();
		}

		BestellUebersicht bestellUebersicht = bestellService.getLastUebersicht();

		if (bestellUebersicht == null) {
			return ResponseEntity.notFound().build();
		}

		List<DiscrepancyEntity> list = bestellUebersicht.getDiscrepancy();

		if (list.stream().anyMatch(discrepancy ->
				discrepancy.getBestand() != null
						&& body.getBestand().getId()
						.equalsIgnoreCase(discrepancy.getBestand().getId()))) {

			return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
		}

		list.add(body);

		BestellUebersicht updated = bestellService.update(bestellUebersicht);

		return ResponseEntity.ok(updated);
	}

	@GetMapping("/gebinde")
	public List<DiscrepancyEntity> getAll() {
		return discrepancyService.findAll();
	}
}