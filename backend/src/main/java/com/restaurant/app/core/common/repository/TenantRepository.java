package com.restaurant.app.core.common.repository;

import com.restaurant.app.core.common.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    @Query("SELECT t FROM Tenant t WHERE t.nitOrTaxId = :nitOrTaxId")
    Optional<Tenant> findByNitOrTaxId(@Param("nitOrTaxId") String nitOrTaxId);
}
