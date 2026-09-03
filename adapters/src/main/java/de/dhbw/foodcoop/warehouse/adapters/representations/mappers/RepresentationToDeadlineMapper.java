package de.dhbw.foodcoop.warehouse.adapters.representations.mappers;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dhbw.foodcoop.warehouse.adapters.representations.DeadlineRepresentation;
import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;

@Component
public class RepresentationToDeadlineMapper implements Function<DeadlineRepresentation, DeadlineEntity> {

    @Autowired
    public RepresentationToDeadlineMapper() {
    }

    @Override
    public DeadlineEntity apply(DeadlineRepresentation deadlineRepresentation) {
       return new DeadlineEntity(
                deadlineRepresentation.getId(),
                deadlineRepresentation.getWeekday(),
                deadlineRepresentation.getTime(),
                deadlineRepresentation.getDatum()
        );
    }

    public DeadlineEntity update(DeadlineEntity oldDeadline, DeadlineRepresentation newDeadline) {
        return new DeadlineEntity(
                oldDeadline.getId(),
                newDeadline.getWeekday(),
                newDeadline.getTime(),
                newDeadline.getDatum()
        );
    }
}

