package de.dhbw.foodcoop.warehouse.application.preishistorie;

import java.time.LocalDateTime;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineSavedEvent;
import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;
import de.dhbw.foodcoop.warehouse.domain.repositories.BestandRepository;

@Component
public class PreisHistorieDeadlineListener {

    private final BestandRepository bestandRepository;
    private final PreisHistorieService preisHistorieService;
    private final DeadlineService deadlineService;

    public PreisHistorieDeadlineListener(
            BestandRepository bestandRepository,
            PreisHistorieService preisHistorieService,
            DeadlineService deadlineService
    ) {
        this.bestandRepository =
                bestandRepository;

        this.preisHistorieService =
                preisHistorieService;

        this.deadlineService =
                deadlineService;
    }

    @EventListener
    public void onDeadlineSaved(
            DeadlineSavedEvent event
    ) {

        DeadlineEntity deadline =
                event.deadline();

        /*
         * Nur die aktuellste Deadline interessiert uns.
         *
         * Dadurch würde z.B. ein administratives Bearbeiten
         * einer alten Deadline keine alten Preise verändern.
         */
        DeadlineEntity aktuelleDeadline =
                deadlineService.last();

        if (!aktuelleDeadline
                .getId()
                .equals(deadline.getId())) {

            return;
        }

        /*
         * Zusätzlich nur offene Bestellrunden snapshotten.
         */
        LocalDateTime deadlineDatum =
                deadlineService
                        .calculateDateFromDeadline(
                                deadline
                        );

        if (LocalDateTime.now()
                .isAfter(deadlineDatum)) {

            return;
        }

        bestandRepository
                .alle()
                .forEach(
                        bestand ->
                                preisHistorieService
                                        .speicherePreis(
                                                bestand,
                                                deadline
                                        )
                );
    }
}