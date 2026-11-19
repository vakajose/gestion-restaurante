# Runbook Guiado Paso a Paso: Oracle Cloud Infrastructure (`TASK-609`)

> **Entorno objetivo:** Tenancy OCI `vakajose` — Región `us-ashburn-1` (`US East Ashburn`) — Sistema Operativo **Oracle Linux 9 ARM64 (`aarch64`)** — Modalidad **P1 (Always Free primero, `$0.00`)** + **DuckDNS** temporal para HTTPS real con Caddy/Let's Encrypt.

Este documento tiene **dos partes**:
1. **Checklist Maestro Rápido** para controlar tu avance.
2. **Guía Ultra-Detallada** explicando **dónde hacer clic**, **qué escribir exactamente** y **por qué**, para que GitHub Actions, VM 1, VM 2, Object Storage y Caddy funcionen a la primera.

---

## 📋 Parte 1: Checklist Maestro de Progreso

### Fase A — Preparación Local y Gobernanza CIS / Mínimo Privilegio (Única vez como Root)
- [ ] **A1.** Generar par de llaves SSH **Ed25519** exclusivas para OCI/GitHub Actions (`~/.ssh/oci_erp_ed25519`).
- [ ] **A2.** Verificar **MFA** en tu usuario principal (*Break-Glass*), anotar **Tenancy OCID** y **Object Storage Namespace**.
- [ ] **A3.** Crear Compartment `cmp-erp` (y anotar su OCID) + Budget de alerta `$1.00 USD` en `root`.
- [ ] **A4.** Dentro del Domain `Default` (incluido gratis), crear 2 usuarios no-admin: operador diario `ops-erp` (`grp-erp-ops`, confinado a `cmp-erp`) y cuenta técnica `svc-erp-s3` (`grp-erp-s3`, con **Customer Secret Keys** para S3).
- [ ] **A5.** Crear las 2 políticas IAM (`pol-erp-ops` y `pol-erp-s3`), **cerrar sesión del usuario Root** e iniciar sesión como **`ops-erp`**.

### Fase B — Provisión de Infraestructura OCI como `ops-erp` (Red, Buckets, VM 1 y VM 2)
- [ ] **B1.** Subir el paquete IaC (`terraform/` + `cloud-init/`) a **OCI Resource Manager** dentro de **`cmp-erp`** (o ejecutar Ruta B2 manual si hay que reintentar Availability Domains por capacidad Always Free).
- [ ] **B2.** Confirmar creación de VCN `10.0.0.0/16`, Subred Pública/Privada, NSGs, 3 Buckets, `VM 1` (`1 OCPU / 8 GB / 60 GB`) y `VM 2` (`1 OCPU / 4 GB / 60 GB`).
- [ ] **B3.** Anotar la **IP Pública Reservada de `VM 1`** y confirmar IPs privadas (`VM 1 = 10.0.0.10`, `VM 2 = 10.0.1.20`).

### Fase C — DNS Temporal (DuckDNS)
- [ ] **C1.** Registrar subdominio gratuito en DuckDNS (ej. `vakajose-erp.duckdns.org`) apuntando a la IP pública reservada de `VM 1`.
- [ ] **C2.** Verificar resolución DNS con `dig` / `nslookup` para `staging-api.<subdominio>.duckdns.org`.

### Fase D — Configuración Manual en `VM 2` (PostgreSQL 16 Zero-Trust)
- [ ] **D1.** Conectar por SSH a `VM 2` usando `ProxyJump` a través de `VM 1`.
- [ ] **D2.** Verificar `docker-ce`, `aws` CLI, `gpg` y `firewalld` (`5432` y `22` restringidos a `10.0.0.0/24`).
- [ ] **D3.** Copiar `docker/postgres-vm2/`, configurar `VM2_BIND_IP=10.0.1.20`, adaptar `pg_hba.conf` con `10.0.0.10/32`, levantar `postgres:16-alpine` y crear bases/roles (`erp_prod`/`app_prod` y `erp_staging`/`app_staging`).
- [ ] **D4.** Configurar archivo `.env.backup` y registrar `cron` diario cifrado GPG AES-256 hacia `erp-db-backups` + probar ejecución manual.

### Fase E — Configuración Manual en `VM 1` (Apps + Caddy + Staging)
- [ ] **E1.** Conectar por SSH a `VM 1` y verificar `docker-ce`, `fail2ban`, `firewalld` (`80`, `443/tcp`, `443/udp`, `22`) y permisos de `/opt/gestion-restaurante`.
- [ ] **E2.** Clonar el repositorio en `/opt/gestion-restaurante` y verificar conectividad TCP a `10.0.1.20:5432`.
- [ ] **E3.** Generar secretos aleatorios (`JWT_SECRET`, `PLATFORM_JWT_SECRET`, hashes `bcrypt` de Caddy escapados con `$$`) y crear `/opt/gestion-restaurante/.env` (con `IMAGE_TAG=staging` temporal hasta el primer release de producción).

### Fase F — Configuración en GitHub (Secrets, Variables, GHCR, Environments)
- [ ] **F1.** Configurar Secrets (`OCI_VM1_SSH_KEY`, `OCI_VM1_HOST`, `OCI_VM1_USER`) y Variables (`OCI_STAGING_DEPLOY_ENABLED=true`, `OCI_PROD_DEPLOY_ENABLED=false`, `APP_DOMAIN`).
- [ ] **F2.** Configurar GitHub Environments (`staging` y `production` con aprobación manual).

### Fase G — Primer Smoke Staging HTTPS (`v0.6.0-rc.1`)
- [ ] **G1.** Emitir y pushear tag `v0.6.0-rc.1` (dispara [cd-staging.yml](../../.github/workflows/cd-staging.yml)).
- [ ] **G2.** Verificar certificados HTTPS reales, `/actuator/health`, login en Backoffice + ERP, subida de imagen a OCI Object Storage (`erp-media-staging`) y logs en Dozzle.
- [ ] **G3.** Apagar perfil `staging` al terminar para dejar `0 MB` RAM extra consumidos.

---

## 🛠️ Parte 2: Guía Ultra-Detallada (Cómo y Dónde Configurar Todo)

---

### PASO A — Preparación Local y Gobernanza CIS en Consola OCI (Separación del Usuario Principal)

> [!IMPORTANT]
> **¿Por qué existe un único Domain (`Default`) y cómo cumplir buenas prácticas OCI sin pagar extra?**
> En cuentas Always Free / estándar, OCI incluye **1 Identity Domain gratuito (`Default`)** — no necesitas crear ni pagar otro Domain. Lo que exige el **CIS OCI Benchmark** es **no usar tu usuario principal (`Administrators`) para operar infraestructura ni claves S3**:
> 1. **Usuario Principal actual (Break-Glass + MFA):** Se usa **solo en este Paso A** (para crear el Compartment, Budget y políticas raíz) y se guarda para emergencias o Facturación/PAYG.
> 2. **Usuario Operador (`ops-erp` en `grp-erp-ops`):** Tu usuario diario de Consola/Resource Manager, confinado **exclusivamente al compartment `cmp-erp`** (sin permisos sobre Billing ni sobre el Tenancy raíz).
> 3. **Usuario Técnico S3 (`svc-erp-s3` en `grp-erp-s3`):** Cuenta de servicio sin acceso a consola, que solo genera **Customer Secret Keys** para `core.storage` y backups PostgreSQL.

#### A1. Generar llave SSH Ed25519 en tu WSL2 local
Esta llave servirá tanto para tu acceso administrativo como para que **GitHub Actions** despliegue por SSH en `VM 1`.
> **Importante:** GitHub Actions requiere que la llave privada usada por el pipeline **no tenga passphrase** (para ejecutarse de forma desatendida). Puedes crear una exclusiva para OCI/Deploy:

```bash
ssh-keygen -t ed25519 -C "oci-erp-deploy" -f ~/.ssh/oci_erp_ed25519 -N ""
chmod 600 ~/.ssh/oci_erp_ed25519
cat ~/.ssh/oci_erp_ed25519.pub
```
- **Guarda la salida del `.pub`** (la pegarás en OCI).
- **Guarda el contenido completo de `~/.ssh/oci_erp_ed25519`** (lo pegarás luego en GitHub Secrets como `OCI_VM1_SSH_KEY`).

#### A2. Verificar MFA en el Root Admin y obtener Tenancy OCID + Namespace
1. Inicia sesión con tu usuario principal en la **Consola Web de OCI** (`https://cloud.oracle.com`). Verifica arriba a la derecha que la región sea **`US East (Ashburn)`**.
2. Confirma que tienes **MFA** activo: icono de **Perfil (arriba derecha) $\rightarrow$ My profile $\rightarrow$ Security** (si aún no activaste *Mobile App* TOTP, actívalo ahora).
3. Vuelve al icono de **Perfil $\rightarrow$ `Tenancy: vakajose`** y en **Tenancy information**, copia:
   - **OCID:** (`ocid1.tenancy.oc1..aaaa...`) $\rightarrow$ Tu `tenancy_ocid`.
   - **Object Storage Namespace:** (cadena corta ej. `idxxxxxxxxxx`) $\rightarrow$ La usarás en `S3_ENDPOINT`.

#### A3. Crear Compartment `cmp-erp` y Budget `$1.00 USD` (desde Root)
1. **Crear Compartment:**
   - Menú hamburguesa $\rightarrow$ **`Identity & Security`** $\rightarrow$ **`Compartments`** $\rightarrow$ **`Create Compartment`**.
   - **Name:** `cmp-erp`
   - **Description:** `Compartment aislado para plataforma ERP (VCN, Ampere A1, Object Storage)`
   - **Parent Compartment:** `vakajose (root)` $\rightarrow$ **`Create Compartment`**.
   - Entra en **`cmp-erp`** y copia su **OCID** (`ocid1.compartment.oc1..aaaa...`) $\rightarrow$ Este será tu `existing_compartment_ocid`.
2. **Crear Budget de Alerta (`$1.00 USD`):**
   - Menú hamburguesa $\rightarrow$ **`Billing & Cost Management`** $\rightarrow$ **`Cost Management`** $\rightarrow$ **`Budgets`** $\rightarrow$ **`Create Budget`**.
   - **Scope:** `Compartment` | **Target compartment:** `cmp-erp` (o `vakajose (root)` para vigilar toda la cuenta).
   - **Name:** `bdg-erp-always-free` | **Schedule:** `Monthly` | **Monthly Budget Amount:** `1` (`$1.00 USD`).
   - En **Budget Alert Rule**: Threshold **`50%` of Forecast** (y opcional otra al `100%` Actual), **Email Recipients:** tu correo real $\rightarrow$ **`Create`**.

#### A4. Crear Grupos y Usuarios No-Admin en `Default domain`
1. Abre menú hamburguesa $\rightarrow$ **`Identity & Security`** $\rightarrow$ **`Domains`** $\rightarrow$ haz clic en **`Default domain`**.
2. **Crear los 2 Grupos (`Groups -> Create group`):**
   - Grupo 1: **Name:** `grp-erp-ops` | **Description:** `Operadores diarios confinados a cmp-erp`
   - Grupo 2: **Name:** `grp-erp-s3` | **Description:** `Acceso S3 mínimo privilegio para medios y backups`
3. **Crear el Usuario Operador (`Users -> Create user`):**
   - **First name / Last name:** `Ops` / `ERP`
   - **Username / Email:** usa un correo secundario tuyo (ej. alias `+ociops@gmail.com` si usas Gmail, ya que OCI exige email único por usuario interactivo; ese correo recibirá el enlace para fijar su contraseña y MFA).
   - En **Groups**, marca **únicamente `grp-erp-ops`** (NO lo agregues a `Administrators`).
   - Clic en **`Create`**.
4. **Crear el Usuario Técnico S3 (`Users -> Create user`):**
   - **First name / Last name:** `Service` / `ERP S3`
   - **Username / Email:** desmarca *Use email address as username* (si aparece) y escribe `svc-erp-s3` (o usa otro alias ej. `+ocis3@gmail.com`).
   - En **Groups**, marca **únicamente `grp-erp-s3`**.
   - Clic en **`Create`**.
   - Entra en la ficha de `svc-erp-s3` $\rightarrow$ barra izquierda **`Customer secret keys`** $\rightarrow$ **`Generate secret key`** (`Name: erp-s3-key-1`).
   - ⚠️ **COPIA INMEDIATAMENTE `Secret Key` y `Access Key`** (la `Secret Key` nunca se vuelve a mostrar). Serán tus `S3_ACCESS_KEY` y `S3_SECRET_KEY`.

#### A5. Crear Políticas IAM de Mínimo Privilegio y Cambiar al Usuario `ops-erp`
1. Menú hamburguesa $\rightarrow$ **`Identity & Security`** $\rightarrow$ **`Policies`**.
2. A la izquierda en **Compartment**, selecciona **`vakajose (root)`** $\rightarrow$ **`Create Policy`**:
   - **Name:** `pol-erp-ops` | **Description:** `Permite a ops-erp gestionar recursos solo dentro de cmp-erp`
   - Activa **`Show manual editor`** y pega estas 4 líneas exactas:
     ```text
     Allow group 'Default'/'grp-erp-ops' to manage all-resources in compartment cmp-erp
     Allow group 'Default'/'grp-erp-ops' to read compartments in tenancy
     Allow group 'Default'/'grp-erp-ops' to read limits in tenancy
     Allow group 'Default'/'grp-erp-ops' to read objectstorage-namespaces in tenancy
     ```
   - Clic en **`Create`**.
3. Crea una segunda política en **`vakajose (root)`** (`Create Policy`):
   - **Name:** `pol-erp-s3` | **Description:** `Acceso exclusivo a objetos S3 en cmp-erp`
   - Activa **`Show manual editor`** y pega:
     ```text
     Allow group 'Default'/'grp-erp-s3' to read buckets in compartment cmp-erp
     Allow group 'Default'/'grp-erp-s3' to manage objects in compartment cmp-erp
     ```
   - Clic en **`Create`**.
4. **Activar `ops-erp` y retirar la sesión Root:**
   - Abre el correo de activación que recibió tu usuario `ops-erp`, define su contraseña y activa su **MFA**.
   - **Cierra sesión del usuario Root (`Profile -> Sign out`)** y vuelve a iniciar sesión en OCI Console como **`ops-erp`**. *(Desde este punto en adelante ya nunca operas como Root en el día a día).*

---

### PASO B — Provisión en OCI como `ops-erp` (Red VCN, Buckets y VMs)

Tienes **dos rutas**: te recomendamos intentar primero la **Ruta B1 (OCI Resource Manager)** porque aplica toda nuestra IaC de `infra/oci/terraform/` desde el navegador con vista previa (`Plan`) y sin instalar nada en tu PC. Si te aparece `Out of host capacity` en Ampere A1, usa la **Ruta B2** para reintentar los *Availability Domains* `AD-1`, `AD-2`, `AD-3`.

#### Ruta B1 — Provisión con OCI Resource Manager (Recomendada)

1. **Empaqueta la IaC en tu WSL2 local:**
   ```bash
   cd /home/jvaca/.gemini/antigravity/worktrees/gestion-restaurante/task609_oci_infrastructure/infra/oci
   zip -r oci-erp-stack.zip terraform/ cloud-init/
   ```
   *(El archivo queda en `infra/oci/oci-erp-stack.zip`; no lo commitees a Git).*

2. **Crea el Stack en Consola OCI (sesión de `ops-erp`):**
   - Menú hamburguesa $\rightarrow$ **`Developer Services`** $\rightarrow$ **`Resource Manager`** $\rightarrow$ **`Stacks`**.
   - A la izquierda en **Compartment**, selecciona **`cmp-erp`** *(muy importante: `ops-erp` tiene permisos aquí, NO en `root`)*.
   - Clic en **`Create stack`**.
   - En **Choose the origin of the Terraform configuration**, elige **`.Zip file`** y sube `oci-erp-stack.zip`.
   - **Name:** `stack-erp-oci`
   - **Create in compartment:** `cmp-erp`
   - **Terraform version:** elige `1.5.x` (la más reciente disponible) $\rightarrow$ **`Next`**.

3. **Completa las variables en el formulario web:**
   - `tenancy_ocid`: pega tu `ocid1.tenancy.oc1..aaaa...`
   - `home_region`: `us-ashburn-1`
   - `region`: `us-ashburn-1`
   - `create_compartment`: **`false`** *(ya creado en Paso A3)*
   - `existing_compartment_ocid`: pega el OCID de **`cmp-erp`** (`ocid1.compartment.oc1..aaaa...`)
   - `create_iam_s3_policy`: **`false`** *(ya creada en Paso A5)*
   - `create_budget`: **`false`** *(ya creado en Paso A3)*
   - `ssh_public_key`: pega el contenido completo de `~/.ssh/oci_erp_ed25519.pub`
   - `create_compute`: **`true`**
   - `availability_domain_index`: `0` *(AD-1 de Ashburn; si falla por capacidad, cambia a `1` o `2` en `Edit Stack -> Configure variables`)*
   - Deja los demás defaults (`1 OCPU / 8 GB / 60 GB` para VM 1, `1 OCPU / 4 GB / 60 GB` para VM 2) $\rightarrow$ **`Next`** $\rightarrow$ **`Create`**.

4. **Ejecuta `Plan` y `Apply`:**
   - Dentro del Stack creado, clic en **`Plan`**. Espera a que termine en verde (`Succeeded`) y revisa que no haya errores.
   - Clic en **`Apply`** $\rightarrow$ **`Apply`**.
   - Al terminar en verde, haz clic en el tab **`Outputs`** (o en el menú izquierdo **Resources $\rightarrow$ Outputs**) y anota:
     - `vm1_public_ip` (IP Pública Reservada de VM 1)
     - `vm1_private_ip` (`10.0.0.10`)
     - `vm2_private_ip` (`10.0.1.20`)
     - `s3_endpoint`
     - `s3_media_staging_public_base_url`
     - `s3_media_prod_public_base_url`

> [!TIP]
> **¿Qué pasa si `Apply` crea Compartment, Budget, VCN y Buckets pero falla creando las VMs por `Out of host capacity`?**
> 1. En `Edit Stack -> Configure variables`, prueba `availability_domain_index = 1` (AD-2) y luego `2` (AD-3) y vuelve a correr `Apply`.
> 2. Si en Free Tier (P1) los 3 ADs de Ashburn están saturados, puedes:
>    - Poner `create_compute = false` para terminar limpiamente Red + Storage, y crear las VMs manualmente reintentando en la Consola (**Ruta B2** abajo), o
>    - Hacer el upgrade a **Pay-As-You-Go (PAYG)** (`Billing & Cost Management -> Upgrade and Manage Payment`, recordando la retención temporal de ~\$100 USD en tarjeta que tu banco devuelve en días), esperar la confirmación del upgrade y re-ejecutar `Apply`.

#### Ruta B2 — Cómo Crear / Reintentar VMs Manualmente en Consola OCI (si usaste `create_compute = false`)

Si necesitas crear las VMs directo en la interfaz web usando la VCN/NSGs que ya creó Resource Manager:
1. Menú hamburguesa $\rightarrow$ **`Compute`** $\rightarrow$ **`Instances`**.
2. En **Compartment** (izquierda), selecciona **`cmp-erp`**.
3. **Crear `VM 1` (Apps):**
   - Clic en **`Create instance`**.
   - **Name:** `vm-erp-apps-01` | **Compartment:** `cmp-erp`.
   - **Placement:** elige `AD-1` (o prueba `AD-2` / `AD-3`).
   - **Image and shape:**
     - **Change image:** selecciona **`Oracle Linux`** $\rightarrow$ **`Oracle Linux 9`** (verifica que diga `aarch64` o compatible con Ampere al elegir shape).
     - **Change shape:** **`Ampere`** $\rightarrow$ **`VM.Standard.A1.Flex`** $\rightarrow$ **`Number of OCPUs: 1`**, **`Amount of memory (GB): 8`**.
   - **Networking:**
     - **VCN:** `vcn-erp`
     - **Subnet:** `snet-erp-public-apps` (`Public`)
     - **Network security groups:** marca **`Use network security groups to control traffic`** y selecciona **`nsg-erp-apps`**.
     - **Public IPv4 address:** marca **`Assign a public IPv4 address`** (y luego en `VNIC -> IPv4 Addresses` puedes promoverla a *Reserved Public IP*).
     - **Private IPv4 address:** `10.0.0.10`.
   - **Add SSH keys:** pega `~/.ssh/oci_erp_ed25519.pub`.
   - **Boot volume:** marca **`Specify a custom boot volume size`** $\rightarrow$ **`60` GB**.
   - Expande **`Show advanced options`** $\rightarrow$ pestaña **`Management`** $\rightarrow$ **`User data`** $\rightarrow$ pega el contenido de [`infra/oci/cloud-init/vm1-apps-ol9.yaml`](./cloud-init/vm1-apps-ol9.yaml).
   - Clic en **`Create`**.

4. **Crear `VM 2` (Base de Datos):**
   - Repite el flujo con:
     - **Name:** `vm-erp-db-02`
     - **Shape:** `VM.Standard.A1.Flex` $\rightarrow$ **`1 OCPU`**, **`4 GB RAM`**
     - **Boot volume:** **`60` GB**
     - **Subnet:** `snet-erp-private-db` (`Private`)
     - **Public IPv4 address:** **deshabilitada / No asignada**
     - **Private IPv4 address:** `10.0.1.20`
     - **NSG:** **`nsg-erp-db`**
     - **User data (`cloud-init`):** contenido de [`infra/oci/cloud-init/vm2-db-ol9.yaml`](./cloud-init/vm2-db-ol9.yaml).

---

### PASO C — Configurar DuckDNS (Dominio Temporal Gratis para HTTPS Real)

1. Entra a `https://www.duckdns.org` e inicia sesión con tu cuenta de GitHub o Google.
2. En **sub domain**, escribe un nombre libre (ej. `vakajose-erp`) y clic en **`add domain`**.
3. En el campo **current ip**, pega la **IP Pública Reservada de `VM 1`** (`vm1_public_ip`) y clic en **`update ip`**.
4. Verifica desde tu WSL2 local que todos los subdominios ya apuntan a `VM 1`:
   ```bash
   getent hosts staging-api.vakajose-erp.duckdns.org
   getent hosts staging-app.vakajose-erp.duckdns.org
   getent hosts app.vakajose-erp.duckdns.org
   ```
   *(Todos deben devolver automáticamente la IP pública de `VM 1`).*

---

### PASO D — Configuración Manual en `VM 2` (PostgreSQL 16 + Backups Cifrados)

`VM 2` **no tiene IP pública**. Entrarás saltando por `VM 1` (`ProxyJump`) con la misma llave SSH.

#### D1. Configurar `~/.ssh/config` y conectar a `VM 2` (`ProxyJump`)
En tu Fedora WSL2 local, registra los alias `oci-vm1` y `oci-vm2`:
```bash
cat <<'EOF' >> ~/.ssh/config

Host oci-vm1
    HostName <VM1_PUBLIC_IP>
    User opc
    IdentityFile ~/.ssh/oci_erp_ed25519
    IdentitiesOnly yes

Host oci-vm2
    HostName 10.0.1.20
    User opc
    IdentityFile ~/.ssh/oci_erp_ed25519
    IdentitiesOnly yes
    ProxyJump oci-vm1
EOF
chmod 600 ~/.ssh/config
ssh oci-vm2
```
Dentro de `VM 2`, verifica que `cloud-init` terminó bien:
```bash
cloud-init status --wait
docker --version && docker compose version
aws --version && gpg --version
sudo firewall-cmd --list-all
```
*(Debes ver las `rich rules` permitiendo `5432/tcp` y `22/tcp` desde `10.0.0.0/24`).*

#### D2. Copiar `postgres-vm2` + `scripts/db` por `scp` y levantar PostgreSQL 16
Desde tu Fedora WSL2 local (sin necesidad de clonar el repo completo en `VM 2`):
```bash
scp -r docker/postgres-vm2 scripts/db oci-vm2:/opt/gestion-restaurante/
```
Luego en `VM 2`:
```bash
cd /opt/gestion-restaurante/postgres-vm2
```

1. **Ajusta `pg_hba.conf` Zero-Trust:**
   ```bash
   cp pg_hba.conf.template pg_hba.conf
   sed -i 's/<VM1_PRIVATE_IP>/10.0.0.10/g' pg_hba.conf
   cat pg_hba.conf
   ```
2. **Configura variables en `.env` de `postgres-vm2`:**
   Crea `/opt/gestion-restaurante/postgres-vm2/.env`:
   ```bash
   cat <<'EOF' > .env
   VM2_BIND_IP=10.0.1.20
   POSTGRES_SUPERUSER_PASSWORD=GENERAR_PASSWORD_SUPERUSER_FUERTE
   EOF
   chmod 600 .env
   ```
3. **Levanta PostgreSQL 16:**
   ```bash
   docker compose -f docker-compose.db.yml --env-file .env up -d
   docker compose -f docker-compose.db.yml ps
   ```
4. **Crea las 2 bases de datos y los 2 roles aislados (`erp_prod` y `erp_staging`):**
   ```bash
   cp init-databases-and-roles.sql.template init-databases-and-roles.sql
   # Edita init-databases-and-roles.sql y reemplaza las 2 contraseñas fuertes:
   nano init-databases-and-roles.sql

   docker exec -i restaurant-vm2-postgres psql -U postgres < init-databases-and-roles.sql
   # Elimina el archivo con contraseñas en texto plano tras ejecutarlo:
   rm -f init-databases-and-roles.sql
   ```
5. **Aplica `pg_hba.conf` dentro del volumen de PostgreSQL y recarga:**
   ```bash
   docker cp pg_hba.conf restaurant-vm2-postgres:/var/lib/postgresql/data/pg_hba.conf
   docker exec -u postgres restaurant-vm2-postgres psql -U postgres -c "SELECT pg_reload_conf();"
   ```
   *(Nota: `-u postgres` es necesario porque `pg_hba.conf` usa autenticación `peer` para conexiones locales).*
   *(Verifica que solo `10.0.0.10/32` esté autorizado para `app_prod` y `app_staging`).*

#### D3. Configurar Backups Diarios Cifrados GPG hacia OCI Object Storage
En `VM 2`, crea el archivo de entorno para respaldos `/opt/gestion-restaurante/.env.backup` (`chmod 600`):
```bash
cat <<'EOF' > /opt/gestion-restaurante/.env.backup
DB_CONTAINER=restaurant-vm2-postgres
PGUSER=postgres
PGDATABASE=erp_prod
BACKUP_ENCRYPTION_PASSPHRASE=REEMPLAZAR_PASSPHRASE_GPG_MUY_LARGA_Y_SEGURA
BACKUP_OUTPUT_DIR=/var/tmp/restaurant-backups
BACKUP_RETENTION_DAYS=14
S3_ENDPOINT=https://<TU_NAMESPACE>.compat.objectstorage.us-ashburn-1.oraclecloud.com
S3_REGION=us-ashburn-1
BACKUP_S3_BUCKET=erp-db-backups
S3_ACCESS_KEY=<TU_S3_ACCESS_KEY_DEL_PASO_A4>
S3_SECRET_KEY=<TU_S3_SECRET_KEY_DEL_PASO_A4>
EOF
chmod 600 /opt/gestion-restaurante/.env.backup
```
*(Guarda `BACKUP_ENCRYPTION_PASSPHRASE` en tu gestor de contraseñas: sin ella es imposible descifrar un backup).*

Prueba el script manualmente ahora:
```bash
set -a && source /opt/gestion-restaurante/.env.backup && set +a
/opt/gestion-restaurante/db/backup-postgres-s3.sh --docker-container restaurant-vm2-postgres
```
Si termina con `Respaldo completado y verificado exitosamente`, programa el `cron` diario a las `03:00 UTC` (`crontab -e`):
```cron
0 3 * * * bash -c 'set -a && source /opt/gestion-restaurante/.env.backup && set +a && /opt/gestion-restaurante/db/backup-postgres-s3.sh --docker-container restaurant-vm2-postgres >> /var/log/restaurant-backups/cron.log 2>&1'
```

---

### PASO E — Configuración Manual en `VM 1` (Apps + Caddy + Staging)

Conéctate desde tu WSL2 a `VM 1`:
```bash
ssh -i ~/.ssh/oci_erp_ed25519 opc@<VM1_PUBLIC_IP>
```

#### E1. Verificar Bootstrap (`cloud-init`), Docker y conectividad con `VM 2`
```bash
cloud-init status --wait
docker --version && docker compose version
sudo firewall-cmd --list-all
sudo fail2ban-client status sshd
# Prueba conectividad TCP hacia VM 2 puerto 5432 (debe responder "succeeded" o "Connected"):
timeout 3 bash -c '</dev/tcp/10.0.1.20/5432' && echo "OK VM2 5432 accesible"
```

#### E2. Copiar `docker/` en `/opt/gestion-restaurante` (por `scp`)
GitHub Actions (`cd-staging.yml` y `cd-production.yml`) hace `cd /opt/gestion-restaurante` y espera que `docker/` viva ahí directamente (no hace `git pull` en `VM 1`, despliega imágenes GHCR). Desde tu Fedora WSL2 local:
```bash
scp -r docker oci-vm1:/opt/gestion-restaurante/
```
*(Alternativamente, si el repo es público, puedes clonar con `git clone <URL> .`).*

#### E3. Generar Secretos y crear `/opt/gestion-restaurante/.env`
Genera 2 secretos JWT independientes y 2 hashes `bcrypt` para Caddy (`DOZZLE` y `STAGING` Basic Auth):
```bash
echo "JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')"
echo "PLATFORM_JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')"

# Genera el hash de password para Dozzle y Staging Basic Auth (duplica cada '$' como '$$' para Docker Compose):
docker run --rm caddy:2.9-alpine caddy hash-password --plaintext 'TU_PASSWORD_DOZZLE' | sed 's/\$/\$\$/g'
docker run --rm caddy:2.9-alpine caddy hash-password --plaintext 'TU_PASSWORD_STAGING_QA' | sed 's/\$/\$\$/g'
```

Crea `/opt/gestion-restaurante/.env` a partir de la plantilla `docker/.env.cloud.example` (`chmod 600`):
```bash
cp docker/.env.cloud.example .env
chmod 600 .env
nano .env
```
**Campos críticos a completar en `/opt/gestion-restaurante/.env`:**
- `GHCR_OWNER`: tu usuario u organización GitHub en **minúsculas** (ej. `vakajose`).
- **`IMAGE_TAG=staging`** *(⚠️ **CRÍTICO para el primer Smoke Staging:** `cd-staging.yml` levanta `docker-compose.prod.yml` junto con `docker-compose.staging.yml`, pero `:latest` aún no existe en GHCR hasta que lances Producción. Usar `IMAGE_TAG=staging` temporalmente evita el error `manifest unknown` durante el Smoke Test. Cuando liberes Producción a `main`, lo cambias a `latest`).*
- `ACME_EMAIL`: tu correo real (Let's Encrypt envía avisos de expiración aquí).
- `APP_DOMAIN`, `ADMIN_DOMAIN`, `API_DOMAIN`, `LOGS_DOMAIN`: `app.<tu-subdominio>.duckdns.org`, `admin.<tu-subdominio>.duckdns.org`, `api.<tu-subdominio>.duckdns.org`, `logs.<tu-subdominio>.duckdns.org`.
- `STAGING_APP_DOMAIN`, `STAGING_ADMIN_DOMAIN`, `STAGING_API_DOMAIN`: `staging-app.<tu-subdominio>.duckdns.org`, `staging-admin.<tu-subdominio>.duckdns.org`, `staging-api.<tu-subdominio>.duckdns.org`.
- `STAGING_BASIC_AUTH_USER` / `STAGING_BASIC_AUTH_HASH`: usuario y hash generado arriba (con `$$`).
- `DOZZLE_ADMIN_USER` / `DOZZLE_ADMIN_PASSWORD_HASH`: usuario y hash generado arriba (con `$$`).
- `DB_PRIVATE_HOST=10.0.1.20`
- `PROD_DB_PASSWORD` y `STAGING_DB_PASSWORD`: las mismas contraseñas que creaste en `VM 2` (`Paso D2`).
- `JWT_SECRET` y `PLATFORM_JWT_SECRET`: los valores generados con `openssl`.
- `PLATFORM_ADMIN_EMAIL` y `PLATFORM_ADMIN_PASSWORD` (mín. 12 caracteres para el bootstrap del SuperAdmin).
- `CORS_ALLOWED_ORIGINS=https://app.<tu-subdominio>.duckdns.org,https://admin.<tu-subdominio>.duckdns.org`
- `STAGING_CORS_ALLOWED_ORIGINS=https://staging-app.<tu-subdominio>.duckdns.org,https://staging-admin.<tu-subdominio>.duckdns.org`
- `APP_STORAGE_TYPE=S3`
- `S3_ENDPOINT=https://<TU_NAMESPACE>.compat.objectstorage.us-ashburn-1.oraclecloud.com`
- `S3_REGION=us-ashburn-1`
- `S3_BUCKET=erp-media-prod`
- `STAGING_S3_BUCKET=erp-media-staging` *(observa que `docker-compose.staging.yml` soporta `STAGING_S3_BUCKET` y `STAGING_S3_PUBLIC_BASE_URL`)*
- `S3_ACCESS_KEY` y `S3_SECRET_KEY`: del Paso A4.
- `S3_PUBLIC_BASE_URL=https://<TU_NAMESPACE>.compat.objectstorage.us-ashburn-1.oraclecloud.com/erp-media-prod`
- `STAGING_S3_PUBLIC_BASE_URL=https://<TU_NAMESPACE>.compat.objectstorage.us-ashburn-1.oraclecloud.com/erp-media-staging`

> [!NOTE]
> **Detalle de SELinux en Oracle Linux 9:** El volumen `./caddy/Caddyfile.cloud:/etc/caddy/Caddyfile:ro` y `./caddy/conf.d:/etc/caddy/conf.d:ro` suelen funcionar sin problema en Docker CE sobre OL9; si en el primer arranque Caddy reportara `permission denied` al leer el `Caddyfile`, ejecuta una vez `sudo chcon -Rt svirt_sandbox_file_t /opt/gestion-restaurante/docker/caddy`.

---

### PASO F — Configuración en GitHub (Secrets, Variables, Environments y GHCR)

Ve a tu repositorio en **GitHub.com** $\rightarrow$ **`Settings`**.

#### F1. Configurar `Secrets` (en Environments) y `Variables` (a nivel de Repositorio)
1. **Secrets (en `Settings -> Environments -> staging` y `production`):**
   - **`OCI_VM1_HOST`**: IP pública reservada de `VM 1` (ej. `129.80.29.124`).
   - **`OCI_VM1_USER`**: `opc`
   - **`OCI_VM1_SSH_KEY`**: contenido completo de `~/.ssh/oci_erp_ed25519` (desde `-----BEGIN OPENSSH PRIVATE KEY-----` hasta `-----END OPENSSH PRIVATE KEY-----`).
2. **Variables de Repositorio (`Settings -> Secrets and variables -> Actions -> pestaña Variables -> New repository variable`):**
   *(⚠️ Importante: deben ir a nivel de **Repositorio**, no solo dentro del Environment, porque el `if: vars.OCI_STAGING_DEPLOY_ENABLED == 'true'` de `cd-staging.yml` se evalúa antes de cargar el Environment).*
   - **`OCI_STAGING_DEPLOY_ENABLED`**: `true`
   - **`OCI_PROD_DEPLOY_ENABLED`**: `false` *(mantener en `false` hasta que decidas promover a Producción)*
   - **`APP_DOMAIN`**: `app.tecton.duckdns.org`

#### F2. Configurar `Environments`
En **`Settings`** $\rightarrow$ **`Environments`**:
1. **`staging`**: Clic en **`New environment`** $\rightarrow$ nombre `staging` (sin aprobación manual requerida para agilidad en QA, o agrégate si deseas).
2. **`production`**: Clic en **`New environment`** $\rightarrow$ nombre `production` $\rightarrow$ marca **`Required reviewers`** y agréga tu usuario de GitHub (obligatorio según `TASK-610` para exigir confirmación manual antes de desplegar a Producción).

#### F3. Nota sobre GHCR (`ghcr.io`)
El workflow `cd-staging.yml` tiene `packages: write` y usa `GITHUB_TOKEN`. Al completar su primera publicación en GHCR, si tu repositorio es **privado** y `VM 1` recibiera error `unauthorized` al hacer `docker pull`:
- Ve a tu perfil/organización en GitHub $\rightarrow$ **`Packages`** $\rightarrow$ selecciona cada imagen (`gestion-restaurante-backend`, `gestion-restaurante-erp`, `gestion-restaurante-backoffice`) $\rightarrow$ **`Package settings`** $\rightarrow$ **`Manage Actions access`** y verifica que el repositorio tenga acceso de **Read/Write**.

---

### PASO G — Primer Smoke Staging HTTPS (`v0.6.0-rc.1`) y Validación

#### G1. Disparar el pipeline de Staging
Una vez que tengas los cambios integrados en tu rama de trabajo y quieras lanzar el primer Smoke Staging (cuando decidas hacer push):
```bash
git tag -a v0.6.0-rc.1 -m "Smoke Staging OCI HTTPS"
git push origin v0.6.0-rc.1
```
*(Recuerda que **pushear a `develop` o `release/*` NO dispara `cd-staging.yml`**; únicamente el tag `v*-rc.*` o `workflow_dispatch`).*

#### G2. Checklist de Verificación en Vivo
1. **GitHub Actions:** Observa el workflow `CD Staging (GHCR + OCI On-Demand)` $\rightarrow$ `Build, Trivy Scan & Push` (3 imágenes ARM64 verdes) $\rightarrow$ `Deploy Ephemeral Staging to OCI VM 1` verde.
2. **Backend Health (HTTPS real):**
   ```bash
   curl -i https://staging-api.<tu-subdominio>.duckdns.org/actuator/health
   ```
   Debe responder `HTTP/2 200` con certificado TLS válido de Let's Encrypt y `"status":"UP"`.
3. **Backoffice React Staging:** Abre `https://staging-admin.<tu-subdominio>.duckdns.org` (te pedirá Basic Auth `STAGING_BASIC_AUTH_USER` / password). Inicia sesión con el `PLATFORM_ADMIN_EMAIL` / `PLATFORM_ADMIN_PASSWORD` configurado en `.env`.
   - Ve a **Storage / Platform Settings** y ejecuta **Probar conexión S3** para certificar comunicación con el bucket `erp-media-staging` en `us-ashburn-1`.
4. **ERP Angular Staging:** Abre `https://staging-app.<tu-subdominio>.duckdns.org`, verifica carga de `env.js` apuntando a `https://staging-api...` sin errores CORS, crea un restaurante de prueba vía onboarding o sube una foto de plato y verifica que carga desde `erp-media-staging`.
5. **Dozzle (Logs en caliente):** Abre `https://logs.<tu-subdominio>.duckdns.org` con las credenciales `DOZZLE_ADMIN_USER`.

#### G3. Apagar Staging Efímero al Finalizar QA (`0 MB` RAM)
En `VM 1`, cuando termines tus pruebas de Staging y quieras liberar recursos:
```bash
cd /opt/gestion-restaurante
rm -f docker/caddy/conf.d/staging.caddy
docker compose -f docker/docker-compose.prod.yml exec caddy caddy reload --config /etc/caddy/Caddyfile
docker compose -f docker/docker-compose.staging.yml --profile staging down
```
*(Y si además quieres apagar temporalmente los contenedores base `prod` hasta el lanzamiento real, `docker compose -f docker/docker-compose.prod.yml down` — recuerda que si estás en Always Free sin PAYG, mantener una carga ligera/uso regular evita la reclamación por inactividad; si pasaste a PAYG no existe Idle Reclamation).*
