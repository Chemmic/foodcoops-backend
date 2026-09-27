package de.dhbw.foodcoop.warehouse.application.deadline;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;
import de.dhbw.foodcoop.warehouse.domain.exceptions.DeadlineNotFoundException;
import de.dhbw.foodcoop.warehouse.domain.repositories.DeadlineRepository;

@Service
public class DeadlineService {

    private final DeadlineRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public DeadlineService(
            DeadlineRepository repository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }


    // =========================================================================
    // Lesen
    // =========================================================================

    public List<DeadlineEntity> all() {
        return repository.alle();
    }


    public DeadlineEntity last() {

        return repository
                .letzte()
                .orElseThrow(
                        DeadlineNotFoundException::new
                );
    }


    public Optional<DeadlineEntity> findById(
            String id
    ) {

        return repository
                .findeMitId(
                        id
                );
    }


    public Optional<DeadlineEntity> getByPosition(
            int position
    ) {

        return repository
                .findeNachReihenfolge(
                        position
                );
    }


    // =========================================================================
    // Neue Deadline speichern
    // =========================================================================

    /**
     * Speichert eine NEUE Deadline.
     *
     * Danach werden die Listener für die neue Bestellrunde
     * synchron ausgeführt:
     *
     * - Preis-Historie
     * - Bestellübersicht
     */
    public DeadlineEntity save(
            DeadlineEntity deadline
    ) {

        DeadlineEntity saved =
                repository.speichern(
                        deadline
                );

        long start =
                System.currentTimeMillis();

        eventPublisher.publishEvent(
                new DeadlineSavedEvent(
                        saved
                )
        );

        long dauer =
                System.currentTimeMillis()
                        - start;

        System.out.println(
                "[DeadlineService] Alle DeadlineSavedEvent-Listener "
                        + "für Deadline "
                        + saved.getId()
                        + " in "
                        + dauer
                        + " ms abgeschlossen."
        );

        return saved;
    }


    // =========================================================================
    // Bestehende Deadline aktualisieren
    // =========================================================================

    /**
     * Aktualisiert eine bereits vorhandene Deadline.
     *
     * Hier wird absichtlich KEIN DeadlineSavedEvent
     * veröffentlicht.
     *
     * Ein normales Bearbeiten einer Deadline soll nicht
     * erneut Preis-Snapshots oder Bestellübersichten erzeugen.
     */
    public DeadlineEntity update(
            DeadlineEntity deadline
    ) {

        return repository.speichern(
                deadline
        );
    }


    // =========================================================================
    // Löschen
    // =========================================================================

    public void deleteById(
            String id
    ) {

        repository.deleteById(
                id
        );
    }


    // =========================================================================
    // Cold Start
    // =========================================================================

    /**
     * Initiale Deadline beim erstmaligen Start.
     *
     * Kein Event, weil beim Datenbank-Initializer die
     * einzelnen Produkte ohnehin nach und nach angelegt
     * werden.
     */
    public DeadlineEntity coldStart(
            DeadlineEntity deadline
    ) {

        return repository.speichern(
                deadline
        );
    }


    // =========================================================================
    // Automatisch nächste Deadline erzeugen
    // =========================================================================

    public Optional<DeadlineEntity> updateDeadline() {

        Optional<DeadlineEntity> optionalDeadline =
                repository.letzte();

        if (optionalDeadline.isEmpty()) {
            return Optional.empty();
        }

        DeadlineEntity currentDeadline =
                optionalDeadline.get();

        LocalDateTime dateForDeadline =
                calculateDateFromDeadline(
                        currentDeadline
                );

        if (
                LocalDateTime.now()
                        .isAfter(
                                dateForDeadline
                        )
        ) {

            DeadlineEntity newDeadline =
                    new DeadlineEntity(
                            UUID.randomUUID().toString(),
                            currentDeadline.getWeekday(),
                            currentDeadline.getTime(),
                            LocalDateTime.now()
                    );

            /*
             * save() ist hier korrekt:
             *
             * Neue Bestellrunde
             * -> Preis-Snapshot
             * -> Bestellübersicht
             */
            return Optional.of(
                    save(
                            newDeadline
                    )
            );
        }

        return Optional.empty();
    }


    // =========================================================================
    // Wochentage
    // =========================================================================

    public static final Map<String, DayOfWeek>
            germanDaysOfWeek =
            Arrays
                    .stream(
                            DayOfWeek.values()
                    )
                    .collect(
                            Collectors.toMap(
                                    day ->
                                            day.getDisplayName(
                                                    TextStyle.FULL,
                                                    Locale.GERMAN
                                            ),

                                    day ->
                                            day
                            )
                    );


    public static final Map<DayOfWeek, String>
            germanDaysOfWeekReversed =
            Arrays
                    .stream(
                            DayOfWeek.values()
                    )
                    .collect(
                            Collectors.toMap(
                                    day ->
                                            day,

                                    day ->
                                            day.getDisplayName(
                                                    TextStyle.FULL,
                                                    Locale.GERMAN
                                            )
                            )
                    );


    // =========================================================================
    // Tatsächliches Deadline-Datum berechnen
    // =========================================================================

    public LocalDateTime calculateDateFromDeadline(
            DeadlineEntity deadline
    ) {

        LocalDateTime date =
                deadline.getDatum();

        LocalTime currentTime =
                date.toLocalTime();

        LocalTime targetTime =
                deadline
                        .getTime()
                        .toLocalTime();

        DayOfWeek targetDay =
                germanDaysOfWeek
                        .get(
                                deadline.getWeekday()
                        );

        if (
                targetDay.getValue()
                        ==
                        date
                                .getDayOfWeek()
                                .getValue()
        ) {

            if (
                    currentTime
                            .isBefore(
                                    targetTime
                            )
            ) {

                return LocalDateTime.of(
                        date.toLocalDate(),
                        targetTime
                );
            }

            return LocalDateTime.of(
                    date
                            .toLocalDate()
                            .plusDays(7),

                    targetTime
            );
        }

        return LocalDateTime.of(
                date
                        .with(
                                TemporalAdjusters
                                        .next(
                                                targetDay
                                        )
                        )
                        .toLocalDate(),

                targetTime
        );
    }


    // =========================================================================
    // Debugging
    // =========================================================================

    public DeadlineEntity forceNextDeadline() {

        DeadlineEntity currentDeadline =
                repository
                        .letzte()
                        .orElseThrow(
                                DeadlineNotFoundException::new
                        );

        DeadlineEntity newDeadline =
                new DeadlineEntity(
                        UUID.randomUUID().toString(),
                        currentDeadline.getWeekday(),
                        currentDeadline.getTime(),
                        LocalDateTime.now()
                );

        /*
         * Absichtlich save():
         *
         * Der Debug-Endpoint soll sich genau wie
         * eine echte neue Bestellrunde verhalten.
         */
        return save(
                newDeadline
        );
    }
}