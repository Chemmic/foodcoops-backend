package de.dhbw.foodcoop.warehouse.adapters.representations.mappers;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import de.dhbw.foodcoop.warehouse.adapters.representations.BestandRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.BrotBestandRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.FrischBestandRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.ProduktRepresentation;
import de.dhbw.foodcoop.warehouse.domain.entities.BestandEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestand;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestand;
import de.dhbw.foodcoop.warehouse.domain.entities.Produkt;

@Component
public class BestandToRepresentationMapper
		implements Function<BestandEntity, BestandRepresentation> {

	@Autowired
	private KategorieToRepresentationMapper kategorieMapper;

	@Autowired
	private EinheitToRepresentationMapper einheitMapper;

	@Autowired
	private LagerbestandToRepresentationMapper lagerbestandMapper;

	@Autowired
	private AllergenInfoToRepresentationMapper allergenInfoMapper;

	@Lazy
	public BestandToRepresentationMapper() {
	}

	@Override
	public BestandRepresentation apply(BestandEntity t) {

		if (t instanceof BrotBestand) {
			BrotBestand bb = (BrotBestand) t;

			BrotBestandRepresentation brot = new BrotBestandRepresentation(
					bb.getId(),
					bb.getName(),
					bb.getVerfuegbarkeit(),
					bb.getPreis().floatValue(),
					bb.getGewicht(),
					allergenInfoMapper.apply(
							bb.getAllergenInfo()
					)
			);

			brot.setSortierung(bb.getSortierung());

			return brot;
		}

		if (t instanceof FrischBestand) {
			FrischBestand bb = (FrischBestand) t;

			FrischBestandRepresentation frisch = new FrischBestandRepresentation(
					bb.getId(),
					bb.getName(),
					bb.getVerfuegbarkeit(),
					bb.getHerkunftsland(),
					bb.getGebindegroesse(),
					einheitMapper.apply(bb.getEinheit()),
					kategorieMapper.apply(bb.getKategorie()),
					bb.getPreis().floatValue(),
					bb.getVerband(),
					bb.isSpezialfallBestelleinheit()
			);

			frisch.setSortierung(bb.getSortierung());

			return frisch;
		}

		if (t instanceof Produkt) {
			Produkt p = (Produkt) t;

			ProduktRepresentation produkt = new ProduktRepresentation(
					p.getId(),
					p.getName(),
					p.getProduktBezeichnung(),
					kategorieMapper.apply(p.getKategorie()),
					lagerbestandMapper.apply(p.getLagerbestand()),
					p.getPreis().floatValue()
			);

			produkt.setSortierung(p.getSortierung());

			return produkt;
		}

		return null;
	}
}