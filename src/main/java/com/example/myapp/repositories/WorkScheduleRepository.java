package com.example.myapp.repositories;

import com.example.myapp.entitys.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, UUID> {
    // Récupère tous les créneaux d'un coiffeur
    List<WorkSchedule> findByCoiffeurId(UUID coiffeurId);

    // Récupère les créneaux d'un coiffeur pour un jour précis
    List<WorkSchedule> findByCoiffeurIdAndDayOfWeek(UUID coiffeurId, String dayOfWeek);
}
