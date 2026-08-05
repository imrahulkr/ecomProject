package com.ecommerce.project.admin;

import com.ecommerce.project.admin.dto.AdminUserSummaryDTO;
import com.ecommerce.project.auth.AppRole;
import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AuthUtil authUtil;

    @GetMapping
    public Page<AdminUserSummaryDTO> list(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize
    ) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(AppConstants.SORT_USERS_BY).ascending());
        return adminUserService.listUsers(pageable);
    }

    @PutMapping("/{userId}/roles/{role}")
    public AdminUserSummaryDTO grantRole(@PathVariable Long userId, @PathVariable AppRole role) {
        return adminUserService.grantRole(userId, role);
    }

    @DeleteMapping("/{userId}/roles/{role}")
    public AdminUserSummaryDTO revokeRole(@PathVariable Long userId, @PathVariable AppRole role) {
        return adminUserService.revokeRole(userId, role, authUtil.loggedInUserId());
    }
}
