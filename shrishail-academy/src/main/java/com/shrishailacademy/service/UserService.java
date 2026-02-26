package com.shrishailacademy.service;

import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> getAllStudents() {
        return userRepository.findAllStudents();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
