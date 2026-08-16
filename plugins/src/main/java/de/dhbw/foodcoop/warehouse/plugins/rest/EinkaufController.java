package de.dhbw.foodcoop.warehouse.plugins.rest;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.mail.MessagingException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import de.dhbw.foodcoop.warehouse.adapters.representations.EinkaufCreateRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.EinkaufRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.FrischBestandRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.EinkaufCreateToEntityMapper;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.EinkaufToRepresentationMapper;
import de.dhbw.foodcoop.warehouse.application.admin.ConfigurationService;
import de.dhbw.foodcoop.warehouse.application.einkauf.EinkaufService;
import de.dhbw.foodcoop.warehouse.domain.entities.BestandBuyEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.ConfigurationEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.EinkaufEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestellung;
import de.dhbw.foodcoop.warehouse.domain.utils.ConstantsUtils;
import de.dhbw.foodcoop.warehouse.plugins.email.EmailService;
import de.dhbw.foodcoop.warehouse.plugins.helpObjects.BestandBuyCreator;
import de.dhbw.foodcoop.warehouse.plugins.helpObjects.Einkaufsmanagement;
import de.dhbw.foodcoop.warehouse.plugins.pdf.PdfService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
public class EinkaufController {

	private final EinkaufService einkaufService;
	private final PdfService pdf;
	private final EinkaufToRepresentationMapper toPresentation;
	private final EinkaufCreateToEntityMapper createMapper;
	private final EmailService emailService;
	private final ConfigurationService configService;

	public EinkaufController(
			EinkaufService einkaufService,
			PdfService pdf,
			EinkaufToRepresentationMapper toPresentation,
			EinkaufCreateToEntityMapper createMapper,
			EmailService emailService,
			ConfigurationService configService) {

		this.einkaufService = einkaufService;
		this.pdf = pdf;
		this.toPresentation = toPresentation;
		this.createMapper = createMapper;
		this.emailService = emailService;
		this.configService = configService;
	}

	@GetMapping("/einkauf")
	public List<EinkaufRepresentation> all() {
		return einkaufService.all().stream()
				.map(toPresentation)
				.collect(Collectors.toList());
	}

	@PostMapping("/einkauf/pdf/{id}")
	public byte[] sendPdfAndMail(
			@RequestBody String email,
			@PathVariable String id) {

		try {
			EinkaufEntity einkauf = einkaufService.findById(id);

			String fileName = "Einkauf-FoodCoop-"
					+ einkauf.getPersonId()
					+ "-"
					+ LocalDate.now().format(
					DateTimeFormatter.ofPattern("dd-MM-yyyy"))
					+ ".pdf";

			byte[] pdfd = pdf.createEinkauf(einkauf);

			StringBuilder frischString = new StringBuilder();

			if (einkauf.getBestellungsEinkauf() != null) {
				einkauf.getBestellungsEinkauf().forEach(item -> {
					if (item.getBestellung() instanceof FrischBestellung item2) {
						frischString.append(item2.getFrischbestand().getName())
								.append("  je ");
						frischString.append(item2.getFrischbestand().getPreis())
								.append(" €   ");
						frischString.append("Bestellt: ")
								.append(item2.getBestellmenge())
								.append("   ");
						frischString.append("Genommen: ")
								.append(item.getAmount())
								.append("\n");
					}
				});
			}

			StringBuilder brotString = new StringBuilder();

			if (einkauf.getBestellungsEinkauf() != null) {
				einkauf.getBestellungsEinkauf().forEach(item -> {
					if (item.getBestellung() instanceof BrotBestellung item2) {
						brotString.append(item2.getBrotBestand().getName())
								.append("  je ");
						brotString.append(item2.getBrotBestand().getPreis())
								.append(" €   ");
						brotString.append("Bestellt: ")
								.append(item2.getBestellmenge())
								.append("   ");
						brotString.append("Genommen: ")
								.append(item.getAmount())
								.append("\n");
					}
				});
			}

			StringBuilder zuVielString = new StringBuilder();

			if (einkauf.getTooMuchEinkauf() != null) {
				einkauf.getTooMuchEinkauf().forEach(item -> {
					zuVielString.append(
									item.getDiscrepancy()
											.getBestand()
											.getName())
							.append(" je ");

					zuVielString.append(
									item.getDiscrepancy()
											.getBestand()
											.getPreis())
							.append(" €  ");

					zuVielString.append("Genommen: ")
							.append(item.getAmount())
							.append("\n");
				});
			}

			StringBuilder lagerString = new StringBuilder();

			if (einkauf.getBestandEinkauf() != null) {
				einkauf.getBestandEinkauf().forEach(item -> {
					lagerString.append(item.getBestand().getName())
							.append("  je ");
					lagerString.append(item.getBestand().getPreis())
							.append(" €   ");
					lagerString.append("Genommen: ")
							.append(item.getAmount())
							.append("\n");
				});
			}

			double lieferkosten =
					Math.round(einkauf.getDeliveryCostAtTime() * 100.0) / 100.0;

			double brotkosten =
					Math.round(einkauf.getBreadPriceAtTime() * 100.0) / 100.0;

			double frischkosten =
					Math.round(einkauf.getFreshPriceAtTime() * 100.0) / 100.0;

			double lagerkosten =
					Math.round(einkauf.getBestandPriceAtTime() * 100.0) / 100.0;

			double zuvielkosten =
					Math.round(einkauf.getTooMuchPriceAtTime() * 100.0) / 100.0;

			double gesamt =
					Math.round(
							(lieferkosten + einkauf.getTotalPriceAtTime())
									* 100.0)
							/ 100.0;

			Optional<ConfigurationEntity> optionalConfig =
					configService.getConfig();

			if (optionalConfig.isPresent()) {
				String text = optionalConfig.get()
						.getEinkaufEmailText()
						.replace(
								ConstantsUtils.EINKAUF_PLACEHOLDER_DATE,
								LocalDate.now().format(
										DateTimeFormatter.ofPattern(
												"dd.MM.yyyy")))
						.replace(
								ConstantsUtils.EINKAUF_PLACEHOLDER_FRISCH,
								frischString.toString())
						.replace(
								ConstantsUtils.EINKAUF_PLACEHOLDER_BROT,
								brotString.toString())
						.replace(
								ConstantsUtils.EINKAUF_PLACEHOLDER_LAGER,
								lagerString.toString())
						.replace(
								ConstantsUtils.EINKAUF_PLACEHOLDER_ZUVIEL,
								zuVielString.toString())
						.replace(
								ConstantsUtils.PLACEHOLDER_BROT_KOSTEN,
								String.valueOf(brotkosten))
						.replace(
								ConstantsUtils.PLACEHOLDER_FRISCH_KOSTEN,
								String.valueOf(frischkosten))
						.replace(
								ConstantsUtils.PLACEHOLDER_ZUVIEL_KOSTEN,
								String.valueOf(zuvielkosten))
						.replace(
								ConstantsUtils.PLACEHOLDER_LIEFER_KOSTEN,
								String.valueOf(lieferkosten))
						.replace(
								ConstantsUtils.PLACEHOLDER_LAGER_KOSTEN,
								String.valueOf(lagerkosten))
						.replace(
								ConstantsUtils.PLACEHOLDER_GESAMT_KOSTEN,
								String.valueOf(gesamt))
						.replace(
								ConstantsUtils.PLACEHOLDER_PERSONID,
								einkauf.getPersonId());

				emailService.sendEmailWithPDF(
						email,
						"Einkauf bei der FoodCoop Karlsruhe am "
								+ LocalDate.now().format(
								DateTimeFormatter.ofPattern(
										"dd.MM.yyyy")),
						text,
						pdfd,
						fileName);
			}

			return pdfd;

		} catch (IOException | MessagingException e) {
			e.printStackTrace();
			return null;
		}
	}

	@PostMapping("/einkauf/mailToEinkaufsmanagement/{id}")
	public void sendPdfAndMailToEinkaufsmanagement(
			@RequestBody List<Einkaufsmanagement> management,
			@PathVariable String id) {

		EinkaufEntity einkauf = einkaufService.findById(id);

		double lieferkosten =
				Math.round(einkauf.getDeliveryCostAtTime() * 100.0) / 100.0;

		double brotkosten =
				Math.round(einkauf.getBreadPriceAtTime() * 100.0) / 100.0;

		double frischkosten =
				Math.round(einkauf.getFreshPriceAtTime() * 100.0) / 100.0;

		double lagerkosten =
				Math.round(einkauf.getBestandPriceAtTime() * 100.0) / 100.0;

		double zuvielkosten =
				Math.round(einkauf.getTooMuchPriceAtTime() * 100.0) / 100.0;

		double gesamt =
				Math.round(
						(lieferkosten + einkauf.getTotalPriceAtTime())
								* 100.0)
						/ 100.0;

		Optional<ConfigurationEntity> optionalConfig =
				configService.getConfig();

		if (optionalConfig.isPresent()) {
			for (Einkaufsmanagement managementEntry : management) {

				String text = optionalConfig.get()
						.getEinkaufsmanagementEmailText()
						.replace(
								ConstantsUtils.EINKAUF_PLACEHOLDER_DATE,
								LocalDate.now().format(
										DateTimeFormatter.ofPattern(
												"dd.MM.yyyy")))
						.replace(
								ConstantsUtils.PLACEHOLDER_BROT_KOSTEN,
								String.valueOf(brotkosten))
						.replace(
								ConstantsUtils.PLACEHOLDER_FRISCH_KOSTEN,
								String.valueOf(frischkosten))
						.replace(
								ConstantsUtils.PLACEHOLDER_ZUVIEL_KOSTEN,
								String.valueOf(zuvielkosten))
						.replace(
								ConstantsUtils.PLACEHOLDER_LIEFER_KOSTEN,
								String.valueOf(lieferkosten))
						.replace(
								ConstantsUtils.PLACEHOLDER_LAGER_KOSTEN,
								String.valueOf(lagerkosten))
						.replace(
								ConstantsUtils.PLACEHOLDER_GESAMT_KOSTEN,
								String.valueOf(gesamt))
						.replace(
								ConstantsUtils.PLACEHOLDER_SHOPPER_PERSONID,
								String.valueOf(einkauf.getPersonId()))
						.replace(
								ConstantsUtils.PLACEHOLDER_PERSONID,
								managementEntry.getUsername());

				emailService.sendSimpleMessage(
						managementEntry.getEmail(),
						"Einkaufs Rechnung von "
								+ einkauf.getPersonId()
								+ " bei der FoodCoop am "
								+ LocalDate.now().format(
								DateTimeFormatter.ofPattern(
										"dd.MM.yyyy")),
						text);
			}
		}
	}

	@GetMapping("/einkauf/{id}")
	public EinkaufRepresentation one(@PathVariable String id) {
		EinkaufEntity einkauf = einkaufService.findById(id);

		return toPresentation.apply(einkauf);
	}

	@PostMapping("/einkaufe/create/bestandBuyObject")
	public BestandBuyEntity getBBEFromData(
			@RequestBody BestandBuyCreator creator) {

		return einkaufService.createBestandBuyEntityForPersonOrder(
				creator.getBestandEntity(),
				creator.getAmount());
	}

	@PostMapping("/einkauf")
	@Operation(
			summary = "Führe einen Einkauf durch",
			description = "Liefert ein Einkaufs Entity zurück")
	@ApiResponses(value = {
			@ApiResponse(
					responseCode = "200",
					description = "Success",
					content = {
							@Content(
									schema = @Schema(
											implementation = EinkaufRepresentation.class))
					}),
			@ApiResponse(
					responseCode = "400",
					description = "Bad Request",
					content = @Content),
			@ApiResponse(
					responseCode = "401",
					description = "Not Authorized",
					content = @Content),
			@ApiResponse(
					responseCode = "403",
					description = "Forbidden",
					content = @Content)
	})
	public ResponseEntity<?> executeShopping(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
					content = {
							@Content(
									schema = @Schema(
											implementation = EinkaufCreateRepresentation.class)),
							@Content(
									schema = @Schema(
											implementation = FrischBestandRepresentation.class))
					})
			@RequestBody EinkaufCreateRepresentation newEinkauf) {

		String id = newEinkauf.getId() == null
				|| newEinkauf.getId().isBlank()
				|| newEinkauf.getId().equals("undefined")
				? UUID.randomUUID().toString()
				: newEinkauf.getId();

		newEinkauf.setId(id);

		if (newEinkauf.getBestellungsEinkauf() != null) {
			newEinkauf.getBestellungsEinkauf()
					.forEach(item ->
							item.setId(UUID.randomUUID().toString()));
		}

		if (newEinkauf.getTooMuchEinkauf() != null) {
			newEinkauf.getTooMuchEinkauf()
					.forEach(item ->
							item.setId(UUID.randomUUID().toString()));
		}

		EinkaufEntity entity = createMapper.apply(newEinkauf);

		try {
			EinkaufEntity einkauf =
					einkaufService.einkaufDurchführen(
							entity.getPersonId(),
							entity.getBestellungsEinkauf(),
							entity.getBestandEinkauf(),
							entity.getTooMuchEinkauf());

			EinkaufRepresentation response =
					toPresentation.apply(einkauf);

			return ResponseEntity
					.created(URI.create("/einkauf/" + response.getId()))
					.body(response);

		} catch (Exception e) {
			e.printStackTrace();

			String message = e.getMessage() != null
					? e.getMessage()
					: "Einkauf konnte nicht durchgeführt werden.";

			return ResponseEntity
					.status(HttpStatus.NOT_ACCEPTABLE)
					.body(message);
		}
	}

	@DeleteMapping("/einkauf/{id}")
	public ResponseEntity<Void> delete(@PathVariable String id) {

		einkaufService.deleteById(id);

		return ResponseEntity.noContent().build();
	}
}