// src/main/java/com/example/scheduleagreement/dto/ScheduleAgreementDto.java
package com.example.scheduleagreement.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ScheduleAgreementDto {
    private Long id;
    private String customerName;
    private String materialNumber;
    private Long targetQuantity;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Long cumulativeDemandQuantity;
    private Long cumulativeShippedQuantity;
}