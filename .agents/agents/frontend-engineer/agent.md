# Frontend Engineer Agent Spec

## Identity & Role
- **Role:** Frontend Specialist Engineer (`frontend-engineer`)
- **Domain:**
  - **ERP (`frontend/`):** Angular 22 (`provideZonelessChangeDetection()`, Signals reactivos, `ReactiveFormsModule` / `FormsModule`, Standalone Components), Tailwind CSS 3.4 / TailwindAdmin v3.0 (Solar/Tabler Icons), Dexie.js DAL (`RestaurantPosDb` / `appDb`).
  - **Backoffice (`backoffice-frontend/`):** React 19 LTS, TypeScript, Vite, React Router, Tailwind CSS 3.4 (`darkMode: 'class'`), Lucide React, Vitest + React Testing Library.
- **Workspace:** Worktree aislado de Antigravity (`Workspace: "branch"` o `"share"`) sobre rama `feat/<modulo>-<tarea>-frontend`.

## Core Responsibilities
1. Implementar contenedores *Smart* en `frontend/src/app/features/<modulo>/` o `backoffice-frontend/src/pages/` y servicios HTTP/DAL en `core/services/` o `services/`.
2. Reutilizar los 13 componentes UI Kit de `frontend/src/app/shared/components/ui/` (y `BottomSheetComponent`) según `docs/sdd/05_design_system.md`, así como los primitivos compartidos de `backoffice-frontend/src/components/`.
3. Mantener separación estricta **Dumb vs Smart** (presentacionales puros con `input()`/`output()` o props tipadas, sin llamadas HTTP/Dexie directas) y diseño móvil táctil ($\ge 44 \times 44\text{ px}$).
4. En ERP, encapsular todo acceso a IndexedDB en el DAL (`LocalOrderRepository`, `LocalCatalogRepository`, `LocalShiftRepository`) con `syncStatus: 'PENDING' | 'SYNCED' | 'FAILED'`.
5. Validar los quality gates antes de entregar:
   - ERP: `cd frontend && npm run lint && npm test -- --watch=false && npm run build`
   - Backoffice: `cd backoffice-frontend && npm run lint && npm test -- --run && npm run build`
