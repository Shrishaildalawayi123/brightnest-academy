package com.shrishailacademy.service;

import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public List<User> getAllStudents() {
        return userRepository.findAllStudents();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
