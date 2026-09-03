package de.dhbw.foodcoop.warehouse.application.deadline;

import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;

public record DeadlineSavedEvent(DeadlineEntity deadline) {
}