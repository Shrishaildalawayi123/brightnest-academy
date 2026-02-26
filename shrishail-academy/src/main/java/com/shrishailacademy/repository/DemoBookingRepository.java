package com.shrishailacademy.repository;

import com.shrishailacademy.model.DemoBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemoBookingRepository extends JpaRepository<DemoBooking, Long> {

    List<DemoBooking> findAllByOrderByCreatedAtDesc();

    List<DemoBooking> findByStatusOrderByCreatedAtDesc(DemoBooking.Status status);

    long countByStatus(DemoBooking.Status status);
}
