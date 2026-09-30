# 05. Sistema de Diseño - Especificación Datta Able

## 1. Identidad de Color y Tokens Tailwind
- **Fondos (Canvas):**
  - Light: `bg-[#f4f7fa]`
  - Dark: `bg-[#1c2128]`
- **Superficie de Tarjetas:**
  - Light: `bg-white border border-gray-100 shadow-sm rounded-lg`
  - Dark: `bg-[#2a2e36] border border-[#373d49] shadow-sm rounded-lg`
- **Sidebar de Navegación:**
  - Light/Default: `bg-[#3f4d67] text-[#a9b7d0]`
  - Item Activo: `text-[#1de9b6] bg-black/10 font-semibold border-l-4 border-[#1de9b6]`
  - Dark: `bg-[#1b1e24] text-gray-400`
- **Botones y Acciones Primarias:**
  - Primario: `bg-[#04a9f5] hover:bg-[#0398dc] text-white font-medium rounded-md px-4 py-2`
  - Aprobación / Listo: `bg-[#1de9b6] hover:bg-[#15c599] text-white text-xs rounded px-2.5 py-1`
  - Rechazo / Anulación: `bg-[#899fd4] hover:bg-[#7287be] text-white text-xs rounded px-2.5 py-1`
- **Auth Views (Login/Register):**
  - Dos círculos decorativos de fondo en gradiente: Púrpura (`#9084d7`) en cuadrante inferior izquierdo y Verde Turquesa (`#1de9b6`) en cuadrante superior derecho.

## 2. Persistencia de Tema
- La clase `.dark` se inyecta en la etiqueta `<html>`.
- El servicio `ThemeService` de Angular expone un Signal `isDarkMode = signal<boolean>(...)` y sincroniza con `localStorage.getItem('app_theme')` y la tabla `user_preferences`.