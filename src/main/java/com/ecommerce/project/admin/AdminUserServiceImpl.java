package com.ecommerce.project.admin;

import com.ecommerce.project.admin.dto.AdminUserSummaryDTO;
import com.ecommerce.project.auth.AppRole;
import com.ecommerce.project.auth.Role;
import com.ecommerce.project.auth.RoleRepository;
import com.ecommerce.project.auth.User;
import com.ecommerce.project.auth.UserRepository;
import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public Page<AdminUserSummaryDTO> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toDTO);
    }

    @Override
    @Transactional
    public AdminUserSummaryDTO grantRole(Long userId, AppRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user", "userId", userId));
        Role roleEntity = roleRepository.findByRoleName(role)
                .orElseThrow(() -> new IllegalStateException(role + " is not configured"));

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        user.getRoles().add(roleEntity);
        userRepository.save(user);
        return toDTO(user);
    }

    @Override
    @Transactional
    public AdminUserSummaryDTO revokeRole(Long userId, AppRole role, Long callingAdminId) {
        if (role == AppRole.ROLE_ADMIN && userId.equals(callingAdminId)) {
            throw new APIException("You cannot revoke your own admin role");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user", "userId", userId));
        if (user.getRoles() != null) {
            user.getRoles().removeIf(r -> r.getRoleName() == role);
            userRepository.save(user);
        }
        return toDTO(user);
    }

    private AdminUserSummaryDTO toDTO(User user) {
        Set<String> roleNames = user.getRoles() == null ? Set.of() : user.getRoles().stream()
                .map(r -> r.getRoleName().name())
                .collect(Collectors.toSet());

        return AdminUserSummaryDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .enabled(user.isEnabled())
                .roles(roleNames)
                .build();
    }
}
