// src/main/java/com/example/scheduleagreement/model/ScheduleAgreement.java
package com.example.scheduleagreement.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class ScheduleAgreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String materialNumber;
    private Long targetQuantity; // 总体承诺数量
    private LocalDate validFrom;
    private LocalDate validTo;

    private Long cumulativeDemandQuantity = 0L; // 累计客户需求量
    private Long cumulativeShippedQuantity = 0L; // 累计已发货量

    @OneToMany(mappedBy = "scheduleAgreement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DemandLine> demandLines = new ArrayList<>();

    @OneToMany(mappedBy = "scheduleAgreement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryPlanLine> deliveryPlanLines = new ArrayList<>();

    public ScheduleAgreement(String customerName, String materialNumber, Long targetQuantity, LocalDate validFrom, LocalDate validTo) {
        this.customerName = customerName;
        this.materialNumber = materialNumber;
        this.targetQuantity = targetQuantity;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }
}
