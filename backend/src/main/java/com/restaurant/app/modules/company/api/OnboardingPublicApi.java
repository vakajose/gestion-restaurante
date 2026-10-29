package com.restaurant.app.modules.company.api;

import com.restaurant.app.core.security.UserDto;

import java.util.UUID;

public interface OnboardingPublicApi {

    record OnboardingCompanyDto(
        String legalName,
        String slug,
        String nitOrTaxId,
        String logoUrl
    ) {}

    record OnboardingBranchDto(
        String name,
        String address,
        Double latitude,
        Double longitude,
        String phone,
        String timezone,
        String currency
    ) {
        public OnboardingBranchDto(
            String name,
            String address,
            Double latitude,
            Double longitude,
            String phone
        ) {
            this(name, address, latitude, longitude, phone, "America/La_Paz", "BOB");
        }
    }

    record OnboardingAdminDto(
        String fullName,
        String username,
        String email,
        String password
    ) {}

    record OnboardingRequest(
        OnboardingCompanyDto company,
        OnboardingBranchDto branch,
        String businessTemplate, // FAST_FOOD, TRADITIONAL, CAFE, BAR, EMPTY
        Boolean includeDemoCatalog,
        OnboardingAdminDto adminUser
    ) {
        public OnboardingRequest(
            OnboardingCompanyDto company,
            OnboardingBranchDto branch,
            String businessTemplate,
            boolean includeDemoCatalog,
            OnboardingAdminDto adminUser
        ) {
            this(company, branch, businessTemplate, Boolean.valueOf(includeDemoCatalog), adminUser);
        }

        public boolean shouldIncludeDemoCatalog() {
            return includeDemoCatalog == null ? true : includeDemoCatalog;
        }
    }

    record OnboardingResultDto(
        UUID tenantId,
        UUID branchId,
        UUID adminUserId,
        String slug,
        String accessToken,
        String token,
        String refreshToken,
        UserDto user
    ) {
        public OnboardingResultDto(
            UUID tenantId,
            UUID branchId,
            UUID adminUserId,
            String slug,
            String accessToken,
            String refreshToken
        ) {
            this(tenantId, branchId, adminUserId, slug, accessToken, accessToken, refreshToken, null);
        }
    }

    record CheckAvailabilityResultDto(
        boolean available,
        String suggestedAlternative,
        String suggestedSlug,
        Boolean slugAvailable,
        Boolean nitAvailable
    ) {
        public CheckAvailabilityResultDto(boolean available, String suggestedAlternative) {
            this(available, suggestedAlternative, suggestedAlternative, available, true);
        }
    }

    CheckAvailabilityResultDto checkSlugAvailability(String slug);

    CheckAvailabilityResultDto checkNitAvailability(String nit);

    OnboardingResultDto executeOnboarding(OnboardingRequest request);
}
