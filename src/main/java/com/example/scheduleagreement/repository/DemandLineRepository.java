// src/main/java/com/example/scheduleagreement/repository/DemandLineRepository.java
package com.example.scheduleagreement.repository;

import com.example.scheduleagreement.model.DemandLine;
import com.example.scheduleagreement.model.ScheduleAgreement; // Import ScheduleAgreement
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate; // Import LocalDate

@Repository
public interface DemandLineRepository extends JpaRepository<DemandLine, Long> {
    // New method to get cumulative demand up to a certain date
    @Query("SELECT COALESCE(SUM(dl.requestedQuantity), 0) FROM DemandLine dl " +
           "WHERE dl.scheduleAgreement = :sa AND dl.requestedDate <= :endDate")
    Long sumRequestedQuantityUpToDate(@Param("sa") ScheduleAgreement sa, @Param("endDate") LocalDate endDate);
}


