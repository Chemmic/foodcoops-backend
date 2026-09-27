package de.dhbw.foodcoop.warehouse.plugins.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import de.dhbw.foodcoop.warehouse.domain.entities.PreisHistorieEntity;

public interface SpringDataPreisHistorieRepository
        extends JpaRepository<PreisHistorieEntity, String> {

    Optional<PreisHistorieEntity>
    findByBestand_IdAndDeadline_Id(
            String bestandId,
            String deadlineId
    );

    List<PreisHistorieEntity>
    findAllByBestand_IdOrderByDeadline_DatumAsc(
            String bestandId
    );

    List<PreisHistorieEntity>
    findAllByDeadline_Id(
            String deadlineId
    );


    // =========================================================================
    // Schlanke Abfragen ohne Entity-Laden (Historie / Statistik)
    // =========================================================================

    /** Zeilen: [bestandId, preis] */
    @Query("""
            select p.bestand.id, p.preis
            from PreisHistorieEntity p
            where p.deadline.id = :deadlineId
            """)
    List<Object[]> findePreiseVonDeadline(
            @Param("deadlineId")
            String deadlineId
    );

    /** Zeilen: [deadlineDatum, preis], aufsteigend */
    @Query("""
            select d.datum, p.preis
            from PreisHistorieEntity p
            join p.deadline d
            where p.bestand.id = :bestandId
            order by d.datum
            """)
    List<Object[]> findePreisverlaufVonBestand(
            @Param("bestandId")
            String bestandId
    );


    // =========================================================================
    // Kompletter Preis-Snapshot einer Bestellrunde
    // =========================================================================

    @Modifying
    @Transactional
    @Query(
            value = """
                    INSERT INTO preis_historie (
                        id,
                        bestand_id,
                        deadline_id,
                        preis
                    )
                    SELECT
                        UUID(),
                        b.id,
                        :deadlineId,
                        b.preis
                    FROM bestand b
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM preis_historie ph
                        WHERE ph.bestand_id = b.id
                          AND ph.deadline_id = :deadlineId
                    )
                    """,
            nativeQuery = true
    )
    int erstelleSnapshotFuerDeadline(
            @Param("deadlineId")
            String deadlineId
    );
}