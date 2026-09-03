package de.dhbw.foodcoop.warehouse.plugins.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestellung;

public interface SpringDataBrotBestellungRepository extends JpaRepository<BrotBestellung, String>{
    @Deprecated
    @Query("SELECT b FROM BrotBestellung b WHERE b.datum > :date AND b.personId = :person_id")
    List<BrotBestellung> findByDateAfterAndPerson(@Param("date") LocalDateTime date, @Param("person_id") String person_id);
    @Deprecated
    @Query("SELECT b FROM BrotBestellung b WHERE b.datum <= :date1 AND b.datum > :date2 AND b.personId = :person_id")
    List<BrotBestellung> findByDateBetween(@Param("date1") LocalDateTime date1, @Param("date2") LocalDateTime date2, @Param("person_id") String person_id);
    @Deprecated
    @Query("SELECT f FROM BrotBestellung f WHERE f.datum <= :date1 AND f.datum > :date2")
    List<BrotBestellung> findByDateBetween(@Param("date1") LocalDateTime date1, @Param("date2") LocalDateTime date2);

    @Deprecated
    @Query("SELECT new BrotBestellung(b.id, b.personId, b.brotbestand,  SUM(b.bestellmenge)) " +
            "FROM BrotBestellung b " +
            "WHERE b.datum > :date " +
            "GROUP BY b.brotbestand")
    List<BrotBestellung> findByDateAfterAndSum(@Param("date") LocalDateTime date);
    
    @Query("SELECT b FROM BrotBestellung b WHERE b.personId = :person_id")
    List<BrotBestellung> findAllFromPerson(@Param("person_id") String person_id);
    
    @Query("SELECT f FROM BrotBestellung f WHERE f.datum > :date")
    List<BrotBestellung> findAllAfter(@Param("date") LocalDateTime date);

    List<BrotBestellung> findAllByPersonIdAndDeadline_Id(
            String personId,
            String deadlineId
    );

    List<BrotBestellung> findAllByDeadline_Id(
            String deadlineId
    );
}
