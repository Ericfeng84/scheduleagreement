// src/main/java/com/example/scheduleagreement/service/ScheduleAgreementService.java
package com.example.scheduleagreement.service;

import com.example.scheduleagreement.dto.DeliveryPlanLineDto;
import com.example.scheduleagreement.dto.DemandLineDto;
import com.example.scheduleagreement.dto.ScheduleAgreementDto;
import com.example.scheduleagreement.exception.ResourceNotFoundException;
import com.example.scheduleagreement.model.*;
import com.example.scheduleagreement.repository.DeliveryPlanLineRepository;
import com.example.scheduleagreement.repository.DemandLineRepository;
import com.example.scheduleagreement.repository.ScheduleAgreementRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList; // Added
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduleAgreementService {

    @Autowired
    private ScheduleAgreementRepository scheduleAgreementRepository;

    @Autowired
    private DemandLineRepository demandLineRepository;

    @Autowired
    private DeliveryPlanLineRepository deliveryPlanLineRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public ScheduleAgreementDto createScheduleAgreement(ScheduleAgreementDto dto) {
        ScheduleAgreement sa = modelMapper.map(dto, ScheduleAgreement.class);
        sa.setCumulativeDemandQuantity(0L); // Initialize
        sa.setCumulativeShippedQuantity(0L); // Initialize
        ScheduleAgreement savedSa = scheduleAgreementRepository.save(sa);
        return modelMapper.map(savedSa, ScheduleAgreementDto.class);
    }

    public ScheduleAgreementDto getScheduleAgreementById(Long id) {
        ScheduleAgreement sa = scheduleAgreementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduleAgreement not found with id: " + id));
        return modelMapper.map(sa, ScheduleAgreementDto.class);
    }

    public List<ScheduleAgreementDto> getAllScheduleAgreements() {
        return scheduleAgreementRepository.findAll().stream()
                .map(sa -> modelMapper.map(sa, ScheduleAgreementDto.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public DemandLineDto addDemandToScheduleAgreement(Long agreementId, DemandLineDto demandDto) {
        ScheduleAgreement sa = scheduleAgreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduleAgreement not found with id: " + agreementId));

        DemandLine demandLine = new DemandLine(sa, demandDto.getRequestedDate(), demandDto.getRequestedQuantity());
        demandLine = demandLineRepository.save(demandLine);

        // Update OVERALL cumulative demand on ScheduleAgreement.
        // This field might be used for other purposes, so let's keep it.
        // The monthly ATP logic uses its own cumulative calculation.
        sa.setCumulativeDemandQuantity(sa.getCumulativeDemandQuantity() + demandLine.getRequestedQuantity());
        scheduleAgreementRepository.save(sa);

        return modelMapper.map(demandLine, DemandLineDto.class);
    }

    @Transactional
    public List<DeliveryPlanLineDto> addDeliveryPlanToScheduleAgreement(Long agreementId, DeliveryPlanLineDto deliveryPlanDto) {
        ScheduleAgreement sa = scheduleAgreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduleAgreement not found with id: " + agreementId));

        List<DeliveryPlanLine> createdPlanLines = new ArrayList<>();
        long remainingQuantityToSchedule = deliveryPlanDto.getPlannedQuantity();
        LocalDate currentProcessingDate = deliveryPlanDto.getPlannedDeliveryDate(); // Start with the requested date
        LocalDate originalRequestedDate = deliveryPlanDto.getPlannedDeliveryDate(); // Keep original for first part

        boolean firstIteration = true;

        while (remainingQuantityToSchedule > 0) {
            YearMonth currentYearMonth = YearMonth.from(currentProcessingDate);
            LocalDate endOfMonth = currentYearMonth.atEndOfMonth();
            LocalDate startOfMonth = currentYearMonth.atDay(1);

            
            long monthlyDemand = demandLineRepository.sumRequestedQuantityBetweenDates(
                sa, startOfMonth, endOfMonth);
            
            if (monthlyDemand == 0) { // 修改此处，判断是否等于0
                // 当前月份没有需求，取消订单
                // 您可以根据需要决定是否真的抛出异常，或者只是清空已创建的计划行并返回
                // 例如，如果希望静默处理，可以这样：
                // createdPlanLines.clear(); // 清空已创建的计划行
                // break; // 跳出循环，不再尝试后续月份
                throw new ResourceNotFoundException("当前月份(" + currentYearMonth + ")没有需求，订单无法继续处理");
            }
            
            
            
            
            
            // 1. Cumulative Demand up to end of current processing month
            long cumulativeDemandByMonthEnd = demandLineRepository.sumRequestedQuantityUpToDate(sa, endOfMonth);

            // 2. Cumulative Planned/Deferred Deliveries up to end of current processing month
            // IMPORTANT: This query fetches existing lines. If we save new lines within this loop,
            // they will be picked up by subsequent calls to this query in the *same transaction* if the
            // persistence context is flushed or if the query forces a flush.
            // For ATP, we want existing commitments *before* this new plan.
            // However, as we split and plan for a month, that newly planned quantity *does* count against
            // the ATP of *that same month* if we were to re-evaluate it for another part of the *same original request*.
            // A simpler approach for this loop: calculate ATP based on what's *already committed* (before this whole operation).
            // Then, as we allocate, the *remaining* ATP for *this specific request's splitting* reduces.
            // The current query `sumPlannedOrDeferredQuantityUpToDate` sums up *all* existing lines.
            // This is correct for determining how much is *already* planned by *other* requests.
            long cumulativePlannedByMonthEnd = deliveryPlanLineRepository.sumPlannedOrDeferredQuantityUpToDate(sa, endOfMonth);

            // 3. 当月可承诺量 (Available to Promise for this month)
            long monthlyATP = cumulativeDemandByMonthEnd - cumulativePlannedByMonthEnd;
            long actualCanScheduleThisMonth = Math.max(0, monthlyATP); // Cannot be negative

            // 4. Determine how much to schedule in the current processing month
            long quantityForThisIteration = Math.min(remainingQuantityToSchedule, actualCanScheduleThisMonth);

            if (quantityForThisIteration > 0) {
                DeliveryPlanLine newPlanLine = new DeliveryPlanLine();
                newPlanLine.setScheduleAgreement(sa);
                newPlanLine.setPlannedQuantity(quantityForThisIteration);

                // Set date: If it's the first part and fits in the original month, use original date.
                // Otherwise, use the 1st of the current processing month.
                if (firstIteration) {
                    newPlanLine.setPlannedDeliveryDate(originalRequestedDate);
                } else {
                    newPlanLine.setPlannedDeliveryDate(currentProcessingDate.withDayOfMonth(1));
                }
                newPlanLine.setStatus(DeliveryStatus.PLANNED); // Parts scheduled are PLANNED
                newPlanLine.setSubmissionDate(LocalDate.now());

                DeliveryPlanLine savedLine = deliveryPlanLineRepository.save(newPlanLine);
                createdPlanLines.add(savedLine);

                remainingQuantityToSchedule -= quantityForThisIteration;
            }

            firstIteration = false; // After the first attempt (even if 0 quantity scheduled), it's no longer the first iteration

            if (remainingQuantityToSchedule > 0) {
                // Still quantity left, move to the 1st of the next month
                currentProcessingDate = currentProcessingDate.with(TemporalAdjusters.firstDayOfNextMonth());
                // If the original date was, e.g., Jan 15th, and we couldn't schedule all,
                // the next attempt is for Feb 1st.
            }
        } // End while loop

        return createdPlanLines.stream()
                .map(line -> modelMapper.map(line, DeliveryPlanLineDto.class))
                .collect(Collectors.toList());
    }


    @Transactional
    public DeliveryPlanLineDto shipDelivery(Long agreementId, Long deliveryPlanLineId, LocalDate actualShippedDate) {
        ScheduleAgreement sa = scheduleAgreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduleAgreement not found with id: " + agreementId));

        DeliveryPlanLine deliveryPlanLine = deliveryPlanLineRepository.findById(deliveryPlanLineId)
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryPlanLine not found with id: " + deliveryPlanLineId));

        if (!deliveryPlanLine.getScheduleAgreement().getId().equals(agreementId)) {
            throw new IllegalArgumentException("DeliveryPlanLine does not belong to the specified ScheduleAgreement.");
        }

        if (deliveryPlanLine.getStatus() == DeliveryStatus.SHIPPED) {
            throw new IllegalStateException("This delivery plan line has already been shipped.");
        }
        if (deliveryPlanLine.getStatus() == DeliveryStatus.CANCELLED) {
            throw new IllegalStateException("Cannot ship a cancelled delivery plan line.");
        }

        deliveryPlanLine.setStatus(DeliveryStatus.SHIPPED);
        deliveryPlanLine.setActualShippedDate(actualShippedDate != null ? actualShippedDate : LocalDate.now());

        // Update cumulative shipped quantity on ScheduleAgreement
        sa.setCumulativeShippedQuantity(sa.getCumulativeShippedQuantity() + deliveryPlanLine.getPlannedQuantity());
        scheduleAgreementRepository.save(sa);

        deliveryPlanLine = deliveryPlanLineRepository.save(deliveryPlanLine);
        return modelMapper.map(deliveryPlanLine, DeliveryPlanLineDto.class);
    }


    public List<DemandLineDto> getDemandLinesForAgreement(Long agreementId) {
        ScheduleAgreement sa = scheduleAgreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduleAgreement not found with id: " + agreementId));
        return sa.getDemandLines().stream() // Assuming DemandLines are eagerly fetched or fetched in transaction
                .map(dl -> modelMapper.map(dl, DemandLineDto.class))
                .collect(Collectors.toList());
    }

    public List<DeliveryPlanLineDto> getDeliveryPlanLinesForAgreement(Long agreementId) {
        // No need to fetch SA first if we just want its delivery plan lines
         return deliveryPlanLineRepository.findByScheduleAgreementId(agreementId).stream()
                .map(dpl -> modelMapper.map(dpl, DeliveryPlanLineDto.class))
                .collect(Collectors.toList());
    }
}
