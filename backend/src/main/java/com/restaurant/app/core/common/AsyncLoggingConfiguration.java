package com.restaurant.app.core.common;

import com.restaurant.app.core.common.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.Method;

/**
 * Configuración global de captura y logging de excepciones asíncronas.
 * Evita que errores de validación de negocio (DomainException) impriman stack traces
 * exhaustivos e innecesarios en la consola del servidor.
 */
@Configuration
@EnableAsync
public class AsyncLoggingConfiguration implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncLoggingConfiguration.class);

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable throwable, Method method, Object... params) -> {
            Throwable rootCause = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(throwable);
            if (rootCause instanceof DomainException domainEx) {
                log.warn("Regla de negocio en ejecución asíncrona [{}]: {}", method.getName(), domainEx.getMessage());
            } else if (rootCause instanceof IllegalArgumentException || rootCause instanceof IllegalStateException) {
                log.warn("Inconsistencia en ejecución asíncrona [{}]: {}", method.getName(), rootCause.getMessage());
            } else {
                log.error("Excepción inesperada en ejecución asíncrona [{}]: {}",
                    method.getName(), rootCause.getMessage(), rootCause);
            }
        };
    }
}
