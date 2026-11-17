package com.restaurant.app.modules.configuration.internal;

import com.restaurant.app.core.security.api.PasswordChangePolicyPort;
import com.restaurant.app.modules.configuration.api.ConfigurationPublicApi;
import com.restaurant.app.modules.configuration.api.SystemParameter;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class SecurityPasswordPolicyAdapter implements PasswordChangePolicyPort {

    private final ConfigurationPublicApi configurationPublicApi;

    SecurityPasswordPolicyAdapter(ConfigurationPublicApi configurationPublicApi) {
        this.configurationPublicApi = configurationPublicApi;
    }

    @Override
    public boolean isPasswordChangeEnforcedOnReset(UUID tenantId) {
        return configurationPublicApi.getBoolean(
            SystemParameter.SECURITY_ENFORCE_PASSWORD_CHANGE_ON_RESET,
            null,
            tenantId
        );
    }
}
