package com.shrishailacademy.repository;

import com.shrishailacademy.model.TeacherApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherApplicationRepository extends JpaRepository<TeacherApplication, Long> {

    List<TeacherApplication> findAllByOrderByCreatedAtDesc();

    List<TeacherApplication> findByStatusOrderByCreatedAtDesc(TeacherApplication.Status status);

    long countByStatus(TeacherApplication.Status status);
}
