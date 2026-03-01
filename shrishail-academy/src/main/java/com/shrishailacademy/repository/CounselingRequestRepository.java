package com.shrishailacademy.repository;

import com.shrishailacademy.model.CounselingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CounselingRequestRepository extends JpaRepository<CounselingRequest, Long> {

    List<CounselingRequest> findAllByOrderByCreatedAtDesc();

    List<CounselingRequest> findByStatusOrderByCreatedAtDesc(CounselingRequest.Status status);

    long countByStatus(CounselingRequest.Status status);
}
