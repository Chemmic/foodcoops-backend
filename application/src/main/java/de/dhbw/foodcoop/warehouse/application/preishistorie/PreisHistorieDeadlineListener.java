package de.dhbw.foodcoop.warehouse.application.preishistorie;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineSavedEvent;

@Component
public class PreisHistorieDeadlineListener {

    private final PreisHistorieService preisHistorieService;

    public PreisHistorieDeadlineListener(
            PreisHistorieService preisHistorieService
    ) {
        this.preisHistorieService =
                preisHistorieService;
    }


    @EventListener
    public void onDeadlineSaved(
            DeadlineSavedEvent event
    ) {

        long start =
                System.currentTimeMillis();

        int anzahl =
                preisHistorieService
                        .erstelleSnapshotFuerDeadline(
                                event.deadline().getId()
                        );

        long dauer =
                System.currentTimeMillis()
                        - start;

        System.out.println(
                "[PreisHistorie] Snapshot für Deadline "
                        + event.deadline().getId()
                        + ": "
                        + anzahl
                        + " Preise in "
                        + dauer
                        + " ms"
        );
    }
}