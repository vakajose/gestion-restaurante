package com.restaurant.app.modules.company.internal;

import com.restaurant.app.modules.company.api.OnboardingPublicApi;
import com.restaurant.app.modules.company.api.OnboardingPublicApi.CheckAvailabilityResultDto;
import com.restaurant.app.modules.company.api.OnboardingPublicApi.OnboardingRequest;
import com.restaurant.app.modules.company.api.OnboardingPublicApi.OnboardingResultDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class OnboardingController {

    private final OnboardingPublicApi onboardingPublicApi;

    OnboardingController(OnboardingPublicApi onboardingPublicApi) {
        this.onboardingPublicApi = onboardingPublicApi;
    }

    @PostMapping("/api/v1/auth/onboarding")
    public ResponseEntity<OnboardingResultDto> registerTenantOnboarding(@RequestBody OnboardingRequest request) {
        OnboardingResultDto result = onboardingPublicApi.executeOnboarding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/api/v1/auth/check-slug")
    public ResponseEntity<CheckAvailabilityResultDto> checkSlug(@RequestParam("slug") String slug) {
        return ResponseEntity.ok(onboardingPublicApi.checkSlugAvailability(slug));
    }

    @GetMapping("/api/v1/auth/onboarding/check-availability")
    public ResponseEntity<CheckAvailabilityResultDto> checkAvailability(
        @RequestParam(value = "slug", required = false) String slug,
        @RequestParam(value = "nit", required = false) String nit
    ) {
        if (slug != null && !slug.isBlank() && (nit == null || nit.isBlank())) {
            return ResponseEntity.ok(onboardingPublicApi.checkSlugAvailability(slug));
        }
        if (nit != null && !nit.isBlank() && (slug == null || slug.isBlank())) {
            return ResponseEntity.ok(onboardingPublicApi.checkNitAvailability(nit));
        }
        if (slug != null && !slug.isBlank() && nit != null && !nit.isBlank()) {
            CheckAvailabilityResultDto slugRes = onboardingPublicApi.checkSlugAvailability(slug);
            CheckAvailabilityResultDto nitRes = onboardingPublicApi.checkNitAvailability(nit);
            boolean bothAvailable = slugRes.available() && nitRes.available();
            return ResponseEntity.ok(new CheckAvailabilityResultDto(
                bothAvailable,
                slugRes.suggestedAlternative(),
                slugRes.suggestedSlug(),
                slugRes.available(),
                nitRes.available()
            ));
        }
        return ResponseEntity.ok(new CheckAvailabilityResultDto(true, null, null, true, true));
    }

    @PostMapping("/api/v1/backoffice/tenants")
    @PreAuthorize("hasAnyAuthority('company:write', 'ROLE_SUPERADMIN', 'ROLE_ADMIN_TENANT')")
    public ResponseEntity<OnboardingResultDto> createTenantBackoffice(@RequestBody OnboardingRequest request) {
        OnboardingResultDto result = onboardingPublicApi.executeOnboarding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
