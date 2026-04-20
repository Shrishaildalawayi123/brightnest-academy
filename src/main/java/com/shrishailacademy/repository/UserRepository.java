package com.shrishailacademy.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shrishailacademy.model.User;

/**
 * User Repository - Data Access Layer for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email (for login)
     */
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndTenantId(String email, Long tenantId);

    Optional<User> findByEmailVerificationTokenAndTenantId(String emailVerificationToken, Long tenantId);

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    List<User> findByIdInAndTenantId(Collection<Long> ids, Long tenantId);

    /**
     * Check if email already exists (for registration)
     */
    boolean existsByEmail(String email);

    boolean existsByEmailAndTenantId(String email, Long tenantId);

    /**
     * Check if email already exists for another user.
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByEmailAndTenantIdAndIdNot(String email, Long tenantId, Long id);

    /**
     * Find user by refresh token (for token rotation)
     */
    Optional<User> findByRefreshToken(String refreshToken);

    Optional<User> findByRefreshTokenAndTenantId(String refreshToken, Long tenantId);

    /**
     * Find all users by role
     */
    List<User> findByRole(User.Role role);

    List<User> findByRoleAndTenantId(User.Role role, Long tenantId);

    List<User> findByRoleInAndTenantId(Collection<User.Role> roles, Long tenantId);

    List<User> findAllByTenantId(Long tenantId);

    Page<User> findAllByTenantId(Long tenantId, Pageable pageable);

    long countByTenantId(Long tenantId);

    /**
     * Count users by role
     */
    long countByRole(User.Role role);

    long countByRoleAndTenantId(User.Role role, Long tenantId);

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
