---
title: "Topología Física Cloud OCI y Red Zero-Trust (TASK-609)"
category: "architecture"
level: "C4-Deployment"
module: "global"
tags:
  - architecture
  - oci
  - deployment
  - mermaid
  - sdd
  - zero-trust
last_updated: "2026-11-17"
status: "verified"
---

# ☁️ Topología Física Cloud OCI y Red Zero-Trust

Este artefacto vivo complementa al [Diagrama C4 Nivel 2 de Contenedores](./c4_level2_containers.md) y materializa la decisión arquitectónica [ADR-0026](../../../adr/0026_cloud_infrastructure_and_deployment_stack.md) sobre **Oracle Cloud Infrastructure (`us-ashburn-1`)** usando **Oracle Linux 9 ARM64 (`aarch64`)** dentro del presupuesto **Always Free** (`2 OCPUs`, `12 GB RAM`, `120 GB / 200 GB Block`, `Object Storage S3`).

---

## 1. Diagrama de Despliegue OCI (VCN Zero-Trust + Staging Efímero)

```mermaid
flowchart TB
    subgraph External["🌐 Internet, DNS y CI/CD"]
        CLIENT["Cajeros POS / Gerentes / Superadmin<br/>HTTPS (443 TCP/UDP)"]
        DNS["DNS Temporal DuckDNS / Futuro Dominio<br/>app / admin / api / logs / staging-*"]
        GH["GitHub Actions + GHCR<br/>(Imágenes linux/arm64 + Trivy)"]
        ADMIN["PC Mantenedor<br/>SSH Ed25519"]
    end

    subgraph OCI["☁️ OCI Tenancy (us-ashburn-1) — Compartment: cmp-erp"]
        GOV["Gobernanza CIS / Mínimo Privilegio (Identity Domain Default)<br/>1. Admin Raíz (Break-Glass + MFA) | 2. ops-erp (solo cmp-erp)<br/>3. svc-erp-s3 (solo Customer Secret Keys) + Budget $1.00 USD"]

        subgraph VCN["VCN: vcn-erp (10.0.0.0/16)"]
            IGW["Internet Gateway ($0)"]
            NAT["NAT Gateway ($0)<br/>Solo egreso controlado Subred Privada"]
            SGW["Service Gateway ($0)<br/>Backbone privado OCI OSN"]

            subgraph PUB["Subred Pública: 10.0.0.0/24 — NSG: nsg-erp-apps"]
                VM1["VM 1: Ampere A1 (1 OCPU / 8 GB RAM / 60 GB)<br/>Oracle Linux 9 aarch64 + IP Privada 10.0.0.10<br/>IP Pública Reservada + firewalld + fail2ban<br/>Ingress: 80/tcp, 443/tcp+udp, 22/tcp"]
                subgraph STACK1["Docker CE (/opt/gestion-restaurante)"]
                    CADDY["Caddy 2.9 (TLS Let's Encrypt automático)<br/>Sin acceso a docker.sock"]
                    PROD["Prod Stack (24/7):<br/>Backend + ERP Nginx + Backoffice Nginx + Dozzle"]
                    STG["Staging Stack (--profile staging):<br/>Apagado por defecto (0 MB RAM)"]
                end
            end

            subgraph PRIV["Subred Privada: 10.0.1.0/24 — NSG: nsg-erp-db"]
                VM2["VM 2: Ampere A1 (1 OCPU / 4 GB RAM / 60 GB)<br/>Oracle Linux 9 aarch64 + IP Privada 10.0.1.20<br/>SIN IP Pública | firewalld estricto<br/>Ingress: 5432 y 22 SOLO desde nsg-erp-apps"]
                PG16["PostgreSQL 16 (Docker)<br/>restaurant_prod (app_prod)<br/>restaurant_staging (app_staging)<br/>pg_hba.conf: 10.0.0.10/32"]
                BAK["Cron 03:00 UTC + AWS CLI v2 + GPG<br/>(scripts/db/backup-postgres-s3.sh)"]
            end
        end

        subgraph OS["OCI Object Storage (S3 Compatible)"]
            B_MEDIA_P["erp-media-prod<br/>(ObjectReadWithoutList)"]
            B_MEDIA_S["erp-media-staging<br/>(ObjectReadWithoutList)"]
            B_BAK["erp-db-backups<br/>(NoPublicAccess + retención 30d)"]
        end
    end

    CLIENT -->|"TLS 1.3 (443)"| VM1
    DNS -.->|"Resuelve IP Pública Reservada"| VM1
    GH -->|"SSH 22 + docker pull ghcr.io"| VM1
    ADMIN -->|"SSH 22 / ProxyJump a VM 2"| VM1

    VM1 --- IGW
    VM1 -->|"JDBC 5432 (10.0.1.20)"| VM2
    VM1 -->|"S3 API (core.storage)"| B_MEDIA_P
    VM1 -->|"S3 API (Staging)"| B_MEDIA_S
    VM2 -->|"Backups .sql.gz.gpg"| SGW --> B_BAK
    VM2 -->|"Pull postgres:16-alpine / dnf"| NAT
```

---

## 2. Matriz de Seguridad y Tráfico Zero-Trust

| Capa | Regla Aplicada | Propósito |
| :--- | :--- | :--- |
| **OCI Subnet `private-db`** | `prohibit_public_ip_on_vnic = true` + Security List vacía | Impide por diseño asignar IP pública a `VM 2`. |
| **OCI `nsg-erp-db`** | Ingress `5432/tcp` y `22/tcp` únicamente desde `nsg-erp-apps` | Ningún host externo o de otra subred alcanza PostgreSQL ni SSH de `VM 2`. |
| **OS `firewalld` (OL9)** | En `VM 1`: `80`, `443/tcp`, `443/udp`, `22`. En `VM 2`: solo `10.0.0.0/24` hacia `5432` y `22` | Defensa en profundidad a nivel de kernel Linux. |
| **PostgreSQL `pg_hba.conf`** | `host restaurant_prod app_prod 10.0.0.10/32 scram-sha-256` | Aislamiento estricto por base, rol e IP exacta de `VM 1`. |
| **Object Storage Buckets** | `db-backups`: `NoPublicAccess`; `media-*`: `ObjectReadWithoutList` | Las imágenes de platos se leen por URL exacta sin exponer listado de objetos; los backups cifrados son 100% privados. |

---

## 3. Trazabilidad IaC

- Código declarativo OpenTofu/Terraform y `cloud-init`: [`infra/oci/`](../../../../infra/oci/README.md)
- Guía operativa paso a paso: [`infra/oci/RUNBOOK_OCI_PASO_A_PASO.md`](../../../../infra/oci/RUNBOOK_OCI_PASO_A_PASO.md)
