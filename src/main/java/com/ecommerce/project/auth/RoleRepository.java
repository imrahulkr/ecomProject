package com.ecommerce.project.auth;

import com.ecommerce.project.auth.AppRole;
import com.ecommerce.project.auth.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import com.ecommerce.project.auth.RoleRepository;

@Repository
public interface RoleRepository extends JpaRepository<Role,Long> {
    Optional<Role> findByRoleName(AppRole appRole);
}
