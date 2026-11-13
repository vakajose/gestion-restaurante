package com.restaurant.app.core.storage.internal;

import com.restaurant.app.core.storage.api.StorageProviderType;

import java.util.UUID;

interface StorageStrategy {

    StorageProviderType supports();

    String store(UUID tenantId, UUID dishId, byte[] content, String extension, String contentType);

    void delete(String storedUrl);
}
