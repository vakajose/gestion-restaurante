# **Perfil de Proyecto: Sistema Integral de Gestión para Restaurante (POS \+ ERP)**

## **1\. Definición del Problema y Justificación**

Actualmente, el restaurante opera con un manejo manual y al día de sus finanzas y procesos operativos. Esta forma de trabajo genera una pérdida de visibilidad en los costos reales de los insumos (altamente variables en el mercado), falta de control exacto en el flujo de tickets de atención al cliente y un desconocimiento del stock real y mermas. Como resultado, es difícil determinar la utilidad neta real al final del día y tomar decisiones estratégicas de precios.  
El desarrollo de un sistema web integral permitirá digitalizar, unificar y automatizar la atención al cliente, el control del inventario y la contabilidad, brindando visibilidad financiera total y optimizando el flujo de trabajo en el restaurante.

## **2\. Objetivos del Proyecto**

> * **Objetivo General:** Desarrollar e implementar un sistema web monolítico centralizado para la gestión operativa (pedidos y stock) y financiera de un restaurante minorista.  
> * **Objetivos Específicos:**  
  * Implementar un Punto de Venta (POS) para la gestión del ciclo de vida de los pedidos (tickets) en tiempo real.  
  * Automatizar el control de inventario (Kardex) mediante el descuento exacto de insumos basados en recetas predefinidas.  
  * Digitalizar el registro de compras, egresos operativos diarios y costos fijos mensuales.  
  * Proveer un panel de control (Dashboard) analítico para visualizar la rentabilidad diaria, el punto de equilibrio y el rendimiento de ventas.

## **3\. Alcance del Sistema (Módulos Principales)**

El sistema estará compuesto por 5 módulos altamente integrados:

> 1. **Módulo de Catálogo y Recetas (BOM \- Bill of Materials):** Gestión de platos, categorías, precios de venta y asignación de ingredientes (receta) con cantidades exactas.  
> 2. **Módulo de Punto de Venta (POS / Atención):** Interfaz ágil para la apertura, seguimiento (En espera, Preparación, Entregado) y cobro de tickets.  
> 3. **Módulo de Inventario (Kardex):** Registro de entradas (compras), salidas automáticas (por ventas) y ajustes de mermas, con cálculo de costo promedio.  
> 4. **Módulo de Egresos y Finanzas:** Registro de gastos fijos mensuales (prorrateados por día) y gastos operativos diarios extraordinarios.  
> 5. **Módulo de Analítica (Dashboard):** Reportes de ingresos vs. costos reales, platos más vendidos y alertas de rentabilidad.

## **4\. Especificación de Requisitos Funcionales (Épicas y Casos de Uso)**

### **Épica 1: Punto de Venta y Flujo de Atención**

> * **Como** cajero/mesero, **quiero** abrir un ticket **para** registrar el pedido exacto del cliente.  
> * **Como** cocinero, **quiero** visualizar los tickets en estado "En Preparación" **para** despachar los platos en orden.  
> * **Como** cajero, **quiero** cambiar el estado de un ticket a "Pagado" o "Anulado" **para** cerrar la venta y registrar el ingreso.

### **Épica 2: Control de Inventario**

> * **Como** administrador, **quiero** registrar la compra de insumos de mercado **para** aumentar mi stock disponible y registrar el gasto.  
> * **Como** sistema, **quiero** descontar automáticamente los ingredientes del Kardex al vender un plato **para** mantener el inventario en tiempo real sin intervención humana.  
> * **Como** administrador, **quiero** registrar ajustes manuales o mermas **para** cuadrar el sistema con el inventario físico.

### **Épica 3: Catálogo y Parametrización**

> * **Como** administrador, **quiero** crear un plato y asignarle su receta exacta (ingredientes y gramajes) **para** que el sistema calcule el costo real de producción por plato.  
> * **Como** administrador, **quiero** actualizar los precios de venta **para** ajustar mis márgenes frente a la subida de precios del mercado.

### **Épica 4: Control Financiero**

> * **Como** administrador, **quiero** registrar mis gastos fijos (alquiler, sueldos base) y gastos diarios extra **para** que el sistema calcule mi punto de equilibrio diario.  
> * **Como** administrador, **quiero** ver un dashboard con la "Utilidad Neta Real" (Ingresos \- Costo de Recetas \- Gastos Fijos/Diarios) **para** saber exactamente cuánto dinero gané hoy.

## **5\. Arquitectura y Tecnología Sugerida**

> * **Patrón Arquitectónico:** Monolito Modular Web (escalable y de fácil despliegue inicial).  
> * **Base de Datos:** Motor Relacional (SQL) estricto. Requisito indispensable para asegurar transacciones ACID (evitar fallos si se cobran dos tickets al mismo tiempo o evitar descuadres de inventario).  
> * **Accesibilidad:** Interfaz web *Responsive* (Adaptable a tablets/pantallas táctiles para el área de caja/cocina y celulares para revisión administrativa).

## **6\. Límites y Exclusiones (Fuera del alcance V 1.0)**

Para asegurar el éxito y lanzamiento de la primera versión, el sistema *NO* incluirá en esta fase:

> * Integración con Impuestos Nacionales para facturación electrónica oficial.  
> * Módulo de RRHH, control de asistencia biométrica o planillas complejas.  
> * Aplicación móvil nativa (iOS/Android) para clientes finales (Delivery integrado).  
> * Mapa visual y gestión avanzada de distribución de mesas físicas.