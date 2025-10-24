package com.westbethel.motel_booking.security.repository;

import com.westbethel.motel_booking.security.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Find a role by its name.
     *
     * @param name the role name to search for (e.g., "ROLE_USER", "ROLE_ADMIN")
     * @return Optional containing the role if found
     */
    Optional<Role> findByName(String name);

    /**
     * Check if a role with the given name exists.
     *
     * @param name the role name to check
     * @return true if the role exists
     */
    boolean existsByName(String name);
}
