# ADR 0001: Adopción de Monolito Modular con Spring Modulith

## Contexto
El sistema POS y ERP de restaurante requiere alta cohesión en transacciones financieras y control de inventario (ACID), pero necesita mantener fronteras de contexto claras entre ventas, catálogo, inventario, finanzas y analítica. Una arquitectura de microservicios añadiría una sobrecarga operacional y de red excesiva e innecesaria para esta fase (latencia en POS, transacciones distribuidas, orquestación de contenedores). Un monolito tradicional sin restricciones, por otro lado, suele degenerar en un "Big Ball of Mud" con dependencias circulares y llamadas cruzadas descontroladas a repositorios.

## Decisión
Se adopta una arquitectura de **Monolito Modular** utilizando **Spring Modulith** en Java 25 / Spring Boot.

### Reglas Técnicas:
1. Cada módulo de negocio tiene encapsulación estricta en su paquete.
2. Se prohíbe inyectar repositorios o entidades internas de un módulo en otro.
3. La comunicación inter-módulo se realiza únicamente a través de contratos expuestos en paquetes públicos `.api` o eventos de dominio asíncronos (`@ApplicationModuleListener`).
4. La modularidad se valida automáticamente en la suite de pruebas unitarias mediante `ApplicationModules.of(RestaurantApplication.class).verify()`.

## Consecuencias
- **Positivas:** Despliegue simple y transacciones locales seguras; fácil evolución modular; garantía de separación de responsabilidades; posibilidad futura de extraer módulos a servicios independientes si la escala lo demanda.
- **Negativas:** Los desarrolladores deben respetar rigurosamente la disciplina de no omitir la capa `.api`.
