package com.example.scheduleagreement.repository;

import com.example.scheduleagreement.model.ScheduleAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleAgreementRepository extends JpaRepository<ScheduleAgreement, Long> {
    // 可以根据需要添加自定义查询方法
}