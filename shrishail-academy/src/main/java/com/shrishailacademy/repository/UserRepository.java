package com.shrishailacademy.repository;

import com.shrishailacademy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User Repository - Data Access Layer for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email (for login)
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email already exists (for registration)
     */
    boolean existsByEmail(String email);

    /**
     * Find all users by role
     */
    List<User> findByRole(User.Role role);

    /**
     * Count users by role
     */
    long countByRole(User.Role role);

    /**
     * Find all students
     */
    default List<User> findAllStudents() {
        return findByRole(User.Role.STUDENT);
    }

    /**
     * Find all admins
     */
    default List<User> findAllAdmins() {
        return findByRole(User.Role.ADMIN);
    }
}
