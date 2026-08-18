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

import de.dhbw.foodcoop.warehouse.domain.entities.Deadline;
import de.dhbw.foodcoop.warehouse.domain.exceptions.DeadlineNotFoundException;
import de.dhbw.foodcoop.warehouse.domain.repositories.DeadlineRepository;

@Service
public class DeadlineService {

    private final DeadlineRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public DeadlineService(
            DeadlineRepository repository,
            ApplicationEventPublisher eventPublisher) {

        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public List<Deadline> all() {
        return repository.alle();
    }

    public Deadline last() {
        return repository.letzte()
                .orElseThrow(DeadlineNotFoundException::new);
    }

    public Deadline save(Deadline deadline) {
        Deadline saved = repository.speichern(deadline);

        eventPublisher.publishEvent(
                new DeadlineSavedEvent(saved));

        return saved;
    }

    public Optional<Deadline> findById(String id) {
        return repository.findeMitId(id);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }

    public Optional<Deadline> getByPosition(int position) {
        return repository.findeNachReihenfolge(position);
    }

    public Deadline coldStart(Deadline deadline) {
        return repository.speichern(deadline);
    }

    public Optional<Deadline> updateDeadline() {
        Optional<Deadline> optionalDeadline = repository.letzte();

        if (optionalDeadline.isEmpty()) {
            return Optional.empty();
        }

        Deadline currentDeadline = optionalDeadline.get();

        LocalDateTime dateForDeadline =
                calculateDateFromDeadline(currentDeadline);

        if (LocalDateTime.now().isAfter(dateForDeadline)) {

            Deadline newDeadline = new Deadline(
                    UUID.randomUUID().toString(),
                    currentDeadline.getWeekday(),
                    currentDeadline.getTime(),
                    LocalDateTime.now());

            return Optional.of(save(newDeadline));
        }

        return Optional.empty();
    }

    public static final Map<String, DayOfWeek> germanDaysOfWeek =
            Arrays.stream(DayOfWeek.values())
                    .collect(Collectors.toMap(
                            day -> day.getDisplayName(
                                    TextStyle.FULL,
                                    Locale.GERMAN),
                            day -> day));

    public static final Map<DayOfWeek, String> germanDaysOfWeekReversed =
            Arrays.stream(DayOfWeek.values())
                    .collect(Collectors.toMap(
                            day -> day,
                            day -> day.getDisplayName(
                                    TextStyle.FULL,
                                    Locale.GERMAN)));

    public LocalDateTime calculateDateFromDeadline(Deadline deadline) {

        LocalDateTime date = deadline.getDatum();
        LocalTime currentTime = date.toLocalTime();
        LocalTime targetTime = deadline.getTime().toLocalTime();

        DayOfWeek targetDay =
                germanDaysOfWeek.get(deadline.getWeekday());

        if (targetDay.getValue()
                == date.getDayOfWeek().getValue()) {

            if (currentTime.isBefore(targetTime)) {
                return LocalDateTime.of(
                        date.toLocalDate(),
                        targetTime);
            }

            return LocalDateTime.of(
                    date.toLocalDate().plusDays(7),
                    targetTime);
        }

        return LocalDateTime.of(
                date.with(
                                TemporalAdjusters.next(targetDay))
                        .toLocalDate(),
                targetTime);
    }
}