package de.dhbw.foodcoop.warehouse.adapters.representations.mappers;

import de.dhbw.foodcoop.warehouse.adapters.representations.AllergenInfoRepresentation;
import de.dhbw.foodcoop.warehouse.domain.values.AllergenInfo;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AllergenInfoToRepresentationMapper
        implements Function<AllergenInfo, AllergenInfoRepresentation> {

    @Override
    public AllergenInfoRepresentation apply(AllergenInfo info) {

        if (info == null) {
            return null;
        }

        Set<String> getreide = info.getGetreide()
                .stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return new AllergenInfoRepresentation(
                getreide,
                info.isEier(),
                info.isMilch(),
                info.isSesam(),
                info.isSchalenfruechte(),
                info.isSellerie(),
                info.isSoja(),
                info.getHinweis()
        );
    }
}