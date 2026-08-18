package de.dhbw.foodcoop.warehouse.application.deadline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.dhbw.foodcoop.warehouse.domain.entities.Deadline;
import de.dhbw.foodcoop.warehouse.domain.exceptions.DeadlineNotFoundException;
import de.dhbw.foodcoop.warehouse.domain.repositories.DeadlineRepository;

@ExtendWith(MockitoExtension.class)
class DeadlineServiceTest {

    @Mock
    private DeadlineRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeadlineService service;

    @Test
    void saveSpeichertDeadlineUndPubliziertEvent() {
        Deadline deadline = new Deadline(
                "deadline-1",
                "Montag",
                Time.valueOf("12:00:00"),
                LocalDateTime.of(2026, 8, 17, 10, 0));

        when(repository.speichern(deadline)).thenReturn(deadline);

        Deadline result = service.save(deadline);

        assertSame(deadline, result);

        verify(repository).speichern(deadline);

        ArgumentCaptor<DeadlineSavedEvent> eventCaptor =
                ArgumentCaptor.forClass(DeadlineSavedEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertSame(
                deadline,
                eventCaptor.getValue().deadline());
    }

    @Test
    void coldStartSpeichertOhneEvent() {
        Deadline deadline = new Deadline(
                "deadline-1",
                "Montag",
                Time.valueOf("12:00:00"),
                LocalDateTime.of(2026, 8, 17, 10, 0));

        when(repository.speichern(deadline)).thenReturn(deadline);

        Deadline result = service.coldStart(deadline);

        assertSame(deadline, result);

        verify(repository).speichern(deadline);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void lastWirftExceptionWennKeineDeadlineExistiert() {
        when(repository.letzte())
                .thenReturn(Optional.empty());

        assertThrows(
                DeadlineNotFoundException.class,
                service::last);
    }

    @Test
    void deadlineAmGleichenTagVorDeadlineBleibtAmGleichenTag() {
        Deadline deadline = new Deadline(
                "deadline-1",
                "Montag",
                Time.valueOf("12:00:00"),
                LocalDateTime.of(2026, 8, 17, 10, 0));

        LocalDateTime result =
                service.calculateDateFromDeadline(deadline);

        assertEquals(
                LocalDateTime.of(2026, 8, 17, 12, 0),
                result);
    }

    @Test
    void deadlineAmGleichenTagNachDeadlineWirdEineWocheVerschoben() {
        Deadline deadline = new Deadline(
                "deadline-1",
                "Montag",
                Time.valueOf("12:00:00"),
                LocalDateTime.of(2026, 8, 17, 13, 0));

        LocalDateTime result =
                service.calculateDateFromDeadline(deadline);

        assertEquals(
                LocalDateTime.of(2026, 8, 24, 12, 0),
                result);
    }

    @Test
    void deadlineAnAnderemWochentagWirdAufNaechstenPassendenTagBerechnet() {
        Deadline deadline = new Deadline(
                "deadline-1",
                "Mittwoch",
                Time.valueOf("18:30:00"),
                LocalDateTime.of(2026, 8, 17, 10, 0));

        LocalDateTime result =
                service.calculateDateFromDeadline(deadline);

        assertEquals(
                LocalDateTime.of(2026, 8, 19, 18, 30),
                result);
    }
}