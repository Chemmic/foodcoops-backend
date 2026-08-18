package de.dhbw.foodcoop.warehouse.application.bestellungsliste;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.dhbw.foodcoop.warehouse.application.admin.ConfigurationService;
import de.dhbw.foodcoop.warehouse.application.brot.BrotBestellungService;
import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.application.frischbestellung.FrischBestellungService;
import de.dhbw.foodcoop.warehouse.application.gebindemanagement.GebindemanagementService;
import de.dhbw.foodcoop.warehouse.domain.entities.BestellUebersicht;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.ConfigurationEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.Deadline;
import de.dhbw.foodcoop.warehouse.domain.entities.DiscrepancyEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestand;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.Kategorie;
import de.dhbw.foodcoop.warehouse.domain.repositories.BestellÜbersichtRepository;

@ExtendWith(MockitoExtension.class)
class BestellÜbersichtServiceTest {

    @Mock
    private BestellÜbersichtRepository repo;

    @Mock
    private GebindemanagementService gebindeService;

    @Mock
    private DeadlineService deadlineService;

    @Mock
    private FrischBestellungService frischService;

    @Mock
    private BrotBestellungService brotService;

    @Mock
    private ConfigurationService cfgService;

    private BestellÜbersichtService service;

    @BeforeEach
    void setUp() {
        service = new BestellÜbersichtService(
                repo,
                gebindeService,
                deadlineService,
                frischService,
                brotService,
                cfgService);
    }

    @Test
    void createListLiefertEmptyWennVorherigeDeadlineFehlt() {
        ConfigurationEntity configuration =
                mock(ConfigurationEntity.class);

        when(configuration.getThreshold())
                .thenReturn(80.0);

        when(cfgService.getConfig())
                .thenReturn(Optional.of(configuration));

        Deadline current =
                mock(Deadline.class);

        when(deadlineService.last())
                .thenReturn(current);

        when(deadlineService.getByPosition(0))
                .thenReturn(Optional.of(current));

        when(deadlineService.getByPosition(1))
                .thenReturn(Optional.empty());

        Optional<BestellUebersicht> result =
                service.createList();

        assertTrue(result.isEmpty());

        verify(repo, never())
                .speichern(any());

        verifyNoInteractions(
                frischService,
                brotService,
                gebindeService);
    }

    @Test
    void createListVerarbeitetMixbareUndNichtMixbareKategorien() {
        ConfigurationEntity configuration =
                mock(ConfigurationEntity.class);

        when(configuration.getThreshold())
                .thenReturn(80.0);

        when(cfgService.getConfig())
                .thenReturn(Optional.of(configuration));

        LocalDateTime currentDate =
                LocalDateTime.of(
                        2026,
                        8,
                        16,
                        18,
                        0);

        LocalDateTime previousDate =
                LocalDateTime.of(
                        2026,
                        8,
                        9,
                        18,
                        0);

        Deadline current =
                mock(Deadline.class);

        Deadline previous =
                mock(Deadline.class);

        when(current.getDatum())
                .thenReturn(currentDate);

        when(previous.getDatum())
                .thenReturn(previousDate);

        when(deadlineService.last())
                .thenReturn(current);

        when(deadlineService.getByPosition(0))
                .thenReturn(Optional.of(current));

        when(deadlineService.getByPosition(1))
                .thenReturn(Optional.of(previous));

        Kategorie mixable =
                mock(Kategorie.class);

        Kategorie notMixable =
                mock(Kategorie.class);

        when(mixable.isMixable())
                .thenReturn(true);

        when(notMixable.isMixable())
                .thenReturn(false);

        FrischBestand bestand1 =
                mock(FrischBestand.class);

        FrischBestand bestand2 =
                mock(FrischBestand.class);

        when(bestand1.getKategorie())
                .thenReturn(mixable);

        when(bestand2.getKategorie())
                .thenReturn(notMixable);

        FrischBestellung order1 =
                mock(FrischBestellung.class);

        FrischBestellung order2 =
                mock(FrischBestellung.class);

        when(order1.getFrischbestand())
                .thenReturn(bestand1);

        when(order2.getFrischbestand())
                .thenReturn(bestand2);

        when(frischService.findByDateBetween(
                currentDate,
                previousDate))
                .thenReturn(
                        List.of(
                                order1,
                                order2));

        DiscrepancyEntity discrepancy1 =
                mock(DiscrepancyEntity.class);

        DiscrepancyEntity discrepancy2 =
                mock(DiscrepancyEntity.class);

        when(gebindeService
                .getDiscrepancyListForMixableCategorie(
                        mixable,
                        80.0))
                .thenReturn(
                        List.of(discrepancy1));

        when(gebindeService
                .getDiscrepancyForNotMixableOrder(
                        notMixable,
                        80.0))
                .thenReturn(
                        List.of(discrepancy2));

        BrotBestellung brot =
                mock(BrotBestellung.class);

        when(brotService.findByDateBetween(
                currentDate,
                previousDate))
                .thenReturn(List.of(brot));

        when(repo.speichern(any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        Optional<BestellUebersicht> result =
                service.createList();

        assertTrue(result.isPresent());

        BestellUebersicht uebersicht =
                result.orElseThrow();

        assertSame(
                current,
                uebersicht
                        .getToOrderWithinDeadline());

        assertEquals(
                2,
                uebersicht
                        .getDiscrepancy()
                        .size());

        assertEquals(
                1,
                uebersicht
                        .getBrotBestellung()
                        .size());

        verify(gebindeService)
                .getDiscrepancyListForMixableCategorie(
                        mixable,
                        80.0);

        verify(gebindeService)
                .getDiscrepancyForNotMixableOrder(
                        notMixable,
                        80.0);
    }
}