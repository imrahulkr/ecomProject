package com.ecommerce.project.admin;

import com.ecommerce.project.admin.dto.AdminUserSummaryDTO;
import com.ecommerce.project.auth.AppRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<AdminUserSummaryDTO> listUsers(Pageable pageable);

    AdminUserSummaryDTO grantRole(Long userId, AppRole role);

    AdminUserSummaryDTO revokeRole(Long userId, AppRole role, Long callingAdminId);
}
