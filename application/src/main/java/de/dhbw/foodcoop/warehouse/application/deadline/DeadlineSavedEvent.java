package de.dhbw.foodcoop.warehouse.application.deadline;

import de.dhbw.foodcoop.warehouse.domain.entities.Deadline;

public record DeadlineSavedEvent(Deadline deadline) {
}