package com.myapp.kinesis.modules.appointment.repository;

import com.myapp.kinesis.modules.appointment.entity.AppointmentResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AppointmentResourceRepository extends JpaRepository<AppointmentResourceEntity, UUID> {
    // We don't need any custom methods for Phase 2
}