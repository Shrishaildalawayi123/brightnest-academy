package com.shrishailacademy.repository;

import com.shrishailacademy.model.ChatbotMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, Long> {
}
