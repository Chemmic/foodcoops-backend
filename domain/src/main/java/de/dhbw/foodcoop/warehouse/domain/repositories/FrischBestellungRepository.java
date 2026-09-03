package de.dhbw.foodcoop.warehouse.domain.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestellung;

public interface FrischBestellungRepository {
    List<FrischBestellung> alle();
    
    List<FrischBestellung> findeAlleVonPerson(String person_id);

    @Deprecated
    List<FrischBestellung> findeMitDatumNachUndPerson(LocalDateTime datum, String person_id);


    @Deprecated
    List<FrischBestellung> findeMitDatumZwischen(LocalDateTime datum1, LocalDateTime datum2, String person_id);

    @Deprecated
    List<FrischBestellung> findeMitDatumZwischen(LocalDateTime datum1, LocalDateTime datum2);

    FrischBestellung speichern(FrischBestellung frischBestellung);

    Optional<FrischBestellung> findeMitId(String id);

    @Deprecated
    List<FrischBestellung> findeAlleBestellungenNachDatum(LocalDateTime datum);

    void deleteById(String id);

    List<FrischBestellung> findeVonPersonUndDeadline(
            String personId,
            String deadlineId
    );

    List<FrischBestellung> findeAlleVonDeadline(
            String deadlineId
    );
}
