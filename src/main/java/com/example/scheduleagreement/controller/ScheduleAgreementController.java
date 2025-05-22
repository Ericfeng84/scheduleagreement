// src/main/java/com/example/scheduleagreement/controller/ScheduleAgreementController.java
package com.example.scheduleagreement.controller;

import com.example.scheduleagreement.dto.DeliveryPlanLineDto;
import com.example.scheduleagreement.dto.DemandLineDto;
import com.example.scheduleagreement.dto.ScheduleAgreementDto;
import com.example.scheduleagreement.service.ScheduleAgreementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List; // Import List

@RestController
@RequestMapping("/api/scheduleagreements")
public class ScheduleAgreementController {

    @Autowired
    private ScheduleAgreementService scheduleAgreementService;

    @PostMapping
    public ResponseEntity<ScheduleAgreementDto> createScheduleAgreement(@Valid @RequestBody ScheduleAgreementDto dto) {
        return new ResponseEntity<>(scheduleAgreementService.createScheduleAgreement(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleAgreementDto> getScheduleAgreementById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleAgreementService.getScheduleAgreementById(id));
    }

    @GetMapping
    public ResponseEntity<List<ScheduleAgreementDto>> getAllScheduleAgreements() {

        return ResponseEntity.ok(scheduleAgreementService.getAllScheduleAgreements());
    }

    // --- Demand Lines ---
    @PostMapping("/{agreementId}/demands")
    public ResponseEntity<DemandLineDto> addDemand(
            @PathVariable Long agreementId,
            @Valid @RequestBody DemandLineDto demandDto) {
        return new ResponseEntity<>(scheduleAgreementService.addDemandToScheduleAgreement(agreementId, demandDto), HttpStatus.CREATED);
    }

    @GetMapping("/{agreementId}/demands")
    public ResponseEntity<List<DemandLineDto>> getDemandLines(@PathVariable Long agreementId) {
        return ResponseEntity.ok(scheduleAgreementService.getDemandLinesForAgreement(agreementId));
    }

    // --- Delivery Plan Lines ---
    @PostMapping("/{agreementId}/delivery-plans")
    public ResponseEntity<List<DeliveryPlanLineDto>> addDeliveryPlan( // Return type changed to List
            @PathVariable Long agreementId,
            @Valid @RequestBody DeliveryPlanLineDto deliveryPlanDto) {
        // HttpStatus.OK or HttpStatus.CREATED can be debated. If any part is created, CREATED is fine.
        // If it might return an empty list (e.g., 0 quantity input), OK might be better.
        // Let's stick with CREATED if the list is non-empty, otherwise OK.
        // Or simply always return CREATED if the operation itself was successful.
        List<DeliveryPlanLineDto> createdLines = scheduleAgreementService.addDeliveryPlanToScheduleAgreement(agreementId, deliveryPlanDto);
        if (createdLines.isEmpty() && deliveryPlanDto.getPlannedQuantity() > 0) {
            // This case implies 0 ATP for all future months, which is unlikely unless demand is 0.
            // Or could mean the input quantity was 0.
            // Consider what status is appropriate if nothing could be scheduled.
            // For now, let's assume something is usually scheduled or the input DTO was valid.
            return new ResponseEntity<>(createdLines, HttpStatus.OK); // Or perhaps a specific error/message
        }
        return new ResponseEntity<>(createdLines, HttpStatus.CREATED);
    }

    @GetMapping("/{agreementId}/delivery-plans")
    public ResponseEntity<List<DeliveryPlanLineDto>> getDeliveryPlans(@PathVariable Long agreementId) {
        return ResponseEntity.ok(scheduleAgreementService.getDeliveryPlanLinesForAgreement(agreementId));
    }

    @PostMapping("/{agreementId}/delivery-plans/{deliveryPlanLineId}/ship")
    public ResponseEntity<DeliveryPlanLineDto> shipDelivery(
            @PathVariable Long agreementId,
            @PathVariable Long deliveryPlanLineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate actualShippedDate) {
        return ResponseEntity.ok(scheduleAgreementService.shipDelivery(agreementId, deliveryPlanLineId, actualShippedDate));
    }

    
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World!";
    }
}

    


