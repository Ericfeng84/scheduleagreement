package com.example.scheduleagreement.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
public class DeliveryPlanLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_agreement_id", nullable = false)
    private ScheduleAgreement scheduleAgreement;

    private LocalDate plannedDeliveryDate;
    private Long plannedQuantity;
    private LocalDate submissionDate;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status = DeliveryStatus.PLANNED;

    private LocalDate actualShippedDate; // 实际发货日期 (当 status = SHIPPED)

    public DeliveryPlanLine(ScheduleAgreement scheduleAgreement, LocalDate plannedDeliveryDate, Long plannedQuantity) {
        this.scheduleAgreement = scheduleAgreement;
        this.plannedDeliveryDate = plannedDeliveryDate;
        this.plannedQuantity = plannedQuantity;
        this.submissionDate = LocalDate.now();
    }
}