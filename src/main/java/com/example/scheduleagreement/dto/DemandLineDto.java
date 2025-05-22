// src/main/java/com/example/scheduleagreement/dto/DemandLineDto.java
package com.example.scheduleagreement.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

@Data
public class DemandLineDto {
    private Long id;
    @NotNull(message = "Requested date cannot be null")
    private LocalDate requestedDate;
    @NotNull(message = "Requested quantity cannot be null")
    @Positive(message = "Requested quantity must be positive")
    private Long requestedQuantity;
    private LocalDate submissionDate;
}