---
name: spring-modulith-patterns
description: >-
  Patrones arquitectónicos y reglas inviolables para el desarrollo de módulos en Spring Modulith con Java 25.
  Usar al crear o modificar código en el backend.
---

# Patrones para Spring Modulith en este Proyecto

## 1. Reglas de Paquetes
- Cada módulo se ubica en `com.restaurant.app.modules.<modulo>/`.
- **Paquete `.api`:** Único paquete público expuesto a otros módulos. Contiene interfaces públicas y records/DTOs inmutables.
- **Paquete `.internal`:** Todo el código restante (entidades JPA, repositorios, servicios internos, controladores) es package-private (`class Foo { ... }`).
- **Prohibido:**
  - Importar repositorios de otro módulo.
  - Dependencias circulares entre módulos.
  - Modificar datos de otro módulo de forma síncrona sin pasar por eventos o su API pública.

## 2. Eventos Asíncronos Desacoplados
- Los eventos son records inmutables en el paquete `.api` del emisor (ej. `OrderPaidEvent` en `modules.pos.api`).
- Los consumidores usan `@ApplicationModuleListener` (transaccional y respaldado por el Event Publication Registry de Spring Modulith).

## 3. Verificación de Arquitectura
Toda suite de pruebas unitarias debe incluir y pasar:
```java
@Test
void verifyModularity() {
    ApplicationModules.of(RestaurantApplication.class).verify();
}
```
