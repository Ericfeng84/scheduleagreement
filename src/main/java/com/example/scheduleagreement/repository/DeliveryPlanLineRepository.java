// src/main/java/com/example/scheduleagreement/repository/DeliveryPlanLineRepository.java
package com.example.scheduleagreement.repository;

import com.example.scheduleagreement.model.DeliveryPlanLine;
import com.example.scheduleagreement.model.ScheduleAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate; // Import LocalDate
import java.util.List;

@Repository
public interface DeliveryPlanLineRepository extends JpaRepository<DeliveryPlanLine, Long> {
    List<DeliveryPlanLine> findByScheduleAgreementId(Long scheduleAgreementId);

    // This query was for the old logic, we might not need it or might adapt it.
    // For now, let's keep it commented or remove if truly unused by other parts.
    /*
    @Query("SELECT COALESCE(SUM(dpl.plannedQuantity), 0) FROM DeliveryPlanLine dpl " +
           "WHERE dpl.scheduleAgreement = :sa AND dpl.status <> com.example.scheduleagreement.model.DeliveryStatus.CANCELLED")
    Long sumPlannedQuantityForAgreement(@Param("sa") ScheduleAgreement sa);
    */

    // New method to get cumulative PLANNED or DEFERRED quantity up to a certain date
    @Query("SELECT COALESCE(SUM(dpl.plannedQuantity), 0) FROM DeliveryPlanLine dpl " +
           "WHERE dpl.scheduleAgreement = :sa " +
           "AND dpl.plannedDeliveryDate <= :endDate " +
           "AND (dpl.status = com.example.scheduleagreement.model.DeliveryStatus.PLANNED OR dpl.status = com.example.scheduleagreement.model.DeliveryStatus.DEFERRED)")
    Long sumPlannedOrDeferredQuantityUpToDate(@Param("sa") ScheduleAgreement sa, @Param("endDate") LocalDate endDate);
}
