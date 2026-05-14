package com.example.myapp.entitys;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "reservation_service")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReservationService {

    @Id
    @Column(name = "reservation_id")
    private UUID reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;
}