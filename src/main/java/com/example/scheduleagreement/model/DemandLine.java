package com.example.scheduleagreement.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
public class DemandLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_agreement_id", nullable = false)
    private ScheduleAgreement scheduleAgreement;

    private LocalDate requestedDate;
    private Long requestedQuantity;
    private LocalDate submissionDate;

    public DemandLine(ScheduleAgreement scheduleAgreement, LocalDate requestedDate, Long requestedQuantity) {
        this.scheduleAgreement = scheduleAgreement;
        this.requestedDate = requestedDate;
        this.requestedQuantity = requestedQuantity;
        this.submissionDate = LocalDate.now();
    }
}