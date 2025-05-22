
// src/main/java/com/example/scheduleagreement/dto/DeliveryPlanLineDto.java
package com.example.scheduleagreement.dto;

import com.example.scheduleagreement.model.DeliveryStatus;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

@Data
public class DeliveryPlanLineDto {
    private Long id;
    @NotNull(message = "Planned delivery date cannot be null")
    private LocalDate plannedDeliveryDate;
    @NotNull(message = "Planned quantity cannot be null")
    @Positive(message = "Planned quantity must be positive")
    private Long plannedQuantity;
    private LocalDate submissionDate;
    private DeliveryStatus status;
    private LocalDate actualShippedDate;
}