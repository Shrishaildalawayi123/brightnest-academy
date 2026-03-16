package com.shrishailacademy.repository;

import com.shrishailacademy.model.WhatsappLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhatsappLeadRepository extends JpaRepository<WhatsappLead, Long> {
}
