package com.restaurant.app.core.security.internal;

import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.common.repository.BranchRepository;
import com.restaurant.app.core.entitlements.api.TenantEntitlementService;
import com.restaurant.app.core.security.AppUser;
import com.restaurant.app.core.security.AppUserRepository;
import com.restaurant.app.core.security.RolePermissionRepository;
import com.restaurant.app.core.security.RoleRepository;
import com.restaurant.app.core.security.UserBranchRepository;
import com.restaurant.app.core.security.api.SessionPublicApi;
import com.restaurant.app.core.security.api.UserSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementAdminResetTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;
    @Mock
    private UserBranchRepository userBranchRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TenantEntitlementService tenantEntitlementService;
    @Mock
    private SessionPublicApi sessionPublicApi;

    private UserManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserManagementServiceImpl(
            appUserRepository,
            branchRepository,
            roleRepository,
            rolePermissionRepository,
            userBranchRepository,
            passwordEncoder,
            tenantEntitlementService,
            sessionPublicApi
        );
    }

    @Test
    @DisplayName("adminResetPassword actualiza hash, exige cambio obligatorio y revoca sesiones")
    void shouldResetPasswordForceChangeAndRevokeSessions() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser(tenantId, null, "admin", "admin@test.com", "$old", "ADMIN_TENANT");
        ReflectionTestUtils.setField(user, "id", userId);

        when(appUserRepository.findByTenantIdAndId(tenantId, userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NuevaClave2026")).thenReturn("$encoded");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userBranchRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        UserSummaryDto dto = service.adminResetPassword(tenantId, userId, "NuevaClave2026");

        assertThat(dto.mustChangePassword()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("$encoded");
        verify(sessionPublicApi).revokeAllUserSessions(userId);
    }

    @Test
    @DisplayName("adminResetPassword rechaza contraseñas vacías o menores a 6 caracteres")
    void shouldRejectShortPassword() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.adminResetPassword(tenantId, userId, "12345"))
            .isInstanceOf(ValidationException.class);
    }
}
