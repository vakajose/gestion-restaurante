package com.restaurant.app.modules.configuration.internal;

import com.restaurant.app.modules.configuration.api.ParameterScope;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
class ParameterCacheService {

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    private String buildKey(UUID tenantId, UUID branchId, String key) {
        String t = tenantId != null ? tenantId.toString() : "GLOBAL";
        String b = branchId != null ? branchId.toString() : "NONE";
        return t + ":" + b + ":" + key.toLowerCase();
    }

    public Optional<String> get(UUID tenantId, UUID branchId, String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get(buildKey(tenantId, branchId, key)));
    }

    public void put(UUID tenantId, UUID branchId, String key, String value) {
        if (key != null && value != null) {
            cache.put(buildKey(tenantId, branchId, key), value);
        }
    }

    public void evict(UUID tenantId, UUID branchId, String key) {
        if (key != null) {
            cache.remove(buildKey(tenantId, branchId, key));
        }
    }

    public void evictForParameter(String key, ParameterScope scope, UUID tenantId, UUID branchId) {
        if (key == null) {
            return;
        }
        String normalizedKey = key.toLowerCase();

        if (scope == ParameterScope.BRANCH && branchId != null) {
            cache.remove(buildKey(tenantId, branchId, key));
        } else if (scope == ParameterScope.TENANT && tenantId != null) {
            String tenantPrefix = tenantId.toString() + ":";
            String keySuffix = ":" + normalizedKey;
            cache.keySet().removeIf(k -> k.startsWith(tenantPrefix) && k.endsWith(keySuffix));
        } else {
            // GLOBAL o invalidación amplia
            String keySuffix = ":" + normalizedKey;
            cache.keySet().removeIf(k -> k.endsWith(keySuffix));
        }
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }
}
