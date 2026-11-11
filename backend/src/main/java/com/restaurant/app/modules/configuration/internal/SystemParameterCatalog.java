package com.restaurant.app.modules.configuration.internal;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.modules.configuration.api.ParameterCategory;
import com.restaurant.app.modules.configuration.api.ParameterCriticality;
import com.restaurant.app.modules.configuration.api.ParameterHelpDto;
import com.restaurant.app.modules.configuration.api.ParameterOptionDto;
import com.restaurant.app.modules.configuration.api.ParameterScope;
import com.restaurant.app.modules.configuration.api.ParameterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class SystemParameterCatalog {

    private static final Logger log = LoggerFactory.getLogger(SystemParameterCatalog.class);
    private static final String CATALOG_RESOURCE_PATH = "/config/parameters-catalog.json";

    record ParameterDefinition(
        String key,
        String name,
        String description,
        ParameterCategory category,
        ParameterType type,
        String defaultValue,
        ParameterScope allowedScope,
        ParameterCriticality criticality,
        boolean requiresRestart,
        ParameterHelpDto help,
        List<ParameterOptionDto> options
    ) {
        public ParameterDefinition {
            if (options == null) {
                options = Collections.emptyList();
            }
        }
    }

    private static final Map<String, ParameterDefinition> CATALOG = new LinkedHashMap<>();

    static {
        loadCatalog();
    }

    static synchronized void loadCatalog() {
        CATALOG.clear();
        try (InputStream is = SystemParameterCatalog.class.getResourceAsStream(CATALOG_RESOURCE_PATH)) {
            if (is == null) {
                throw new IllegalStateException("No se encontró el recurso del catálogo de parámetros: " + CATALOG_RESOURCE_PATH);
            }
            ObjectMapper mapper = new ObjectMapper();
            List<ParameterDefinition> list = mapper.readValue(is, new TypeReference<List<ParameterDefinition>>() {});
            for (ParameterDefinition def : list) {
                CATALOG.put(def.key().toLowerCase(), def);
            }
            log.info("Catálogo de parámetros cargado exitosamente desde {} ({} parámetros)", CATALOG_RESOURCE_PATH, CATALOG.size());
        } catch (Exception e) {
            log.error("Error al cargar el catálogo de parámetros desde {}", CATALOG_RESOURCE_PATH, e);
            throw new IllegalStateException("Fallo al inicializar SystemParameterCatalog", e);
        }
    }

    public static Optional<ParameterDefinition> getDefinition(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(CATALOG.get(key.trim().toLowerCase()));
    }

    public static List<ParameterDefinition> getAllDefinitions() {
        return List.copyOf(CATALOG.values());
    }

    public static List<ParameterDefinition> getDefinitionsByCategory(ParameterCategory category) {
        if (category == null) {
            return getAllDefinitions();
        }
        return CATALOG.values().stream()
            .filter(def -> def.category() == category)
            .toList();
    }

    public static void validate(ParameterDefinition def, ParameterScope targetScope, String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("El valor del parámetro no puede estar vacío");
        }

        // Validación de jerarquía de ámbito
        if (targetScope == ParameterScope.BRANCH && def.allowedScope() == ParameterScope.TENANT) {
            throw new BusinessRuleException("El parámetro '" + def.key() + "' no permite sobreescrituras a nivel de sucursal (exclusivo de empresa)");
        }
        if (targetScope == ParameterScope.BRANCH && def.allowedScope() == ParameterScope.GLOBAL) {
            throw new BusinessRuleException("El parámetro '" + def.key() + "' solo puede configurarse a nivel global");
        }
        if (targetScope == ParameterScope.TENANT && def.allowedScope() == ParameterScope.GLOBAL) {
            throw new BusinessRuleException("El parámetro '" + def.key() + "' solo puede configurarse a nivel global");
        }

        // Validación de tipo de dato
        String trimmed = value.trim();
        switch (def.type()) {
            case BOOLEAN -> {
                if (!trimmed.equalsIgnoreCase("true") && !trimmed.equalsIgnoreCase("false")) {
                    throw new ValidationException("El valor para '" + def.key() + "' debe ser booleano ('true' o 'false')");
                }
            }
            case INTEGER -> {
                try {
                    Integer.parseInt(trimmed);
                } catch (NumberFormatException e) {
                    throw new ValidationException("El valor para '" + def.key() + "' debe ser un número entero válido");
                }
            }
            case DOUBLE -> {
                try {
                    Double.parseDouble(trimmed);
                } catch (NumberFormatException e) {
                    throw new ValidationException("El valor para '" + def.key() + "' debe ser un número decimal válido");
                }
            }
            case ENUM -> {
                if (def.options() != null && !def.options().isEmpty()) {
                    boolean matches = def.options().stream()
                        .anyMatch(opt -> opt.value().equalsIgnoreCase(trimmed));
                    if (!matches) {
                        List<String> validValues = def.options().stream().map(ParameterOptionDto::value).toList();
                        throw new ValidationException("El valor '" + trimmed + "' no es válido para '" + def.key() +
                            "'. Opciones permitidas: " + validValues);
                    }
                }
            }
            case STRING, JSON -> {
                // Validación básica de no vacío
            }
        }
    }
}
