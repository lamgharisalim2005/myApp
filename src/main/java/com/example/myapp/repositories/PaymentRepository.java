package com.example.myapp.repositories;

import com.example.myapp.entitys.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByReservationId(UUID reservationId);

    Optional<Payment> findByTransactionId(String transactionId); // ← nouveau
}