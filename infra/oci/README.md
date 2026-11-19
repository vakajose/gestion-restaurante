# Infraestructura Cloud OCI (`infra/oci/`) — TASK-609

Este directorio contiene la especificación declarativa (**OpenTofu / Terraform OCI**), scripts de arranque (**`cloud-init`** para Oracle Linux 9 ARM64) y la guía operativa completa para desplegar el sistema **Gestión Restaurante (POS + ERP + Backoffice)** en **Oracle Cloud Infrastructure (`us-ashburn-1`)**.

> **Guías operativas paso a paso:**
> - **Provisión inicial y QA/Staging:** [`RUNBOOK_OCI_QA_STAGING.md`](./RUNBOOK_OCI_QA_STAGING.md)
> - **Paso a Producción (Go-Live):** [`RUNBOOK_OCI_PROD.md`](./RUNBOOK_OCI_PROD.md)

---

## 1. Mapa de Archivos

| Ruta | Propósito |
| :--- | :--- |
| [`RUNBOOK_OCI_QA_STAGING.md`](./RUNBOOK_OCI_QA_STAGING.md) | Checklist maestro + guía clic-a-clic para Consola OCI, Resource Manager, DuckDNS, VM 2, VM 1, GitHub Actions y Smoke Test `v0.6.0-rc.1`. |
| [`RUNBOOK_OCI_PROD.md`](./RUNBOOK_OCI_PROD.md) | Guía diferencial de Go-Live a Producción (dominio propio, DNS, `IMAGE_TAG=latest`, `cd-production.yml` y Restore Drill). |
| [`cloud-init/vm1-apps-ol9.yaml`](./cloud-init/vm1-apps-ol9.yaml) | Bootstrap automático de **VM 1** (Oracle Linux 9 `aarch64`): expande disco, quita `podman`, instala `docker-ce`, endurece SSH, activa `fail2ban`, configura `firewalld` (`80`, `443/tcp`, `443/udp`, `22`) y crea `/opt/gestion-restaurante`. |
| [`cloud-init/vm2-db-ol9.yaml`](./cloud-init/vm2-db-ol9.yaml) | Bootstrap automático de **VM 2** (Oracle Linux 9 `aarch64`): expande disco, quita `podman`, instala `docker-ce`, AWS CLI v2 ARM64 + `gpg` + `cronie`, y restringe `firewalld` (`5432` y `22` solo desde `10.0.0.0/24`). |
| [`terraform/versions.tf`](./terraform/versions.tf) | Configuración del provider `oracle/oci` con alias explícito `oci.home` para IAM y Budgets. |
| [`terraform/variables.tf`](./terraform/variables.tf) | Parámetros de región, toggles (`create_compartment`, `create_compute`, `create_budget`, `create_compartment_quota`), CIDRs y tamaños Always Free. |
| [`terraform/iam_governance.tf`](./terraform/iam_governance.tf) | Compartment `cmp-erp`, política IAM S3 de mínimo privilegio, Budget mensual `$1.00 USD` (`50%` / `100%`) y Quota opcional `2 OCPUs A1`. |
| [`terraform/network.tf`](./terraform/network.tf) | VCN `10.0.0.0/16`, Internet Gateway, NAT Gateway, Service Gateway, Subred Pública (`10.0.0.0/24`), Subred Privada (`10.0.1.0/24`) y NSGs Zero-Trust (`nsg-erp-apps` / `nsg-erp-db`). |
| [`terraform/storage.tf`](./terraform/storage.tf) | Buckets S3-compatibles: `erp-db-backups` (`NoPublicAccess` + retención 30d) y `erp-media-prod` / `erp-media-staging` (`ObjectReadWithoutList`). |
| [`terraform/compute.tf`](./terraform/compute.tf) | Instancias `VM.Standard.A1.Flex` (`VM 1`: `1 OCPU / 8 GB / 60 GB`, IP pública reservada; `VM 2`: `1 OCPU / 4 GB / 60 GB`, sin IP pública). |
| [`terraform/outputs.tf`](./terraform/outputs.tf) | Salidas listas para copiar en `.env`, DuckDNS y GitHub Secrets. |

---

## 2. Presupuesto de Recursos Always Free (`us-ashburn-1`)

| Recurso | Asignado | Tope Always Free | Reserva Libre |
| :--- | :--- | :--- | :--- |
| **Ampere A1 (`VM.Standard.A1.Flex`)** | `VM 1` (`1 OCPU / 8 GB`) + `VM 2` (`1 OCPU / 4 GB`) = **2 OCPUs / 12 GB** | 2 OCPUs / 12 GB (1,500 OCPU-h / 9,000 GB-h mes) | `0` (100% aprovechado) |
| **Block Storage (Boot Volumes)** | `VM 1` (`60 GB`) + `VM 2` (`60 GB`) = **120 GB** | 200 GB totales | **`80 GB` libres** (expandible en línea con `oci-growfs` o nuevo Block Volume $\ge 50\text{ GB}$) |
| **Object Storage (Standard)** | 3 buckets (`media-prod`, `media-staging`, `db-backups`) | 10 GB Standard (PAYG/Trial) / 20 GB combinados (Free Tier) + 50,000 req/mes | ~`10–20 GB` |
| **VCN + IGW + NAT GW + Service GW** | 1 VCN + 3 Gateways | 2 VCNs, Gateways `$0.00`, 10 TB egress/mes | 1 VCN libre |

---

## 3. Cómo Empaquetar para OCI Resource Manager

Si vas a ejecutar la infraestructura desde la Consola Web de OCI (**Developer Services $\rightarrow$ Resource Manager $\rightarrow$ Stacks**):

```bash
cd infra/oci
zip -r oci-erp-stack.zip terraform/ cloud-init/
```

*(Nota: al usar `filebase64("${path.module}/../cloud-init/...")`, el `.zip` debe contener ambas carpetas `terraform/` y `cloud-init/` al mismo nivel).*
