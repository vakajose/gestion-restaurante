# ==============================================================================
# Variables de Entrada — OCI Sovereign Stack (TASK-609)
# ==============================================================================

variable "tenancy_ocid" {
  description = "OCID del Tenancy raíz de OCI (ej. ocid1.tenancy.oc1..xxxx)."
  type        = string
}

variable "home_region" {
  description = "Home Region del Tenancy OCI (donde viven IAM y Budgets). Ej: us-ashburn-1."
  type        = string
  default     = "us-ashburn-1"
}

variable "region" {
  description = "Región de despliegue para VCN, Compute y Object Storage. Ej: us-ashburn-1."
  type        = string
  default     = "us-ashburn-1"
}

variable "project_prefix" {
  description = "Prefijo para nombrar recursos en OCI."
  type        = string
  default     = "erp"
}

# --- Gobernanza IAM & Presupuesto ---

variable "create_compartment" {
  description = "Recomendado false (buena práctica OCI CIS): el usuario Root crea cmp-erp en Paso A y el operador diario despliega dentro usando existing_compartment_ocid."
  type        = bool
  default     = false
}

variable "existing_compartment_ocid" {
  description = "OCID del compartment cmp-erp creado en Paso A (usado cuando create_compartment = false)."
  type        = string
  default     = ""
}

variable "create_iam_s3_policy" {
  description = "Mantener false si la política pol-erp-s3 ya fue creada en Paso A por el administrador raíz."
  type        = bool
  default     = false
}

variable "s3_service_group_name" {
  description = "Nombre del grupo en Identity Domain 'Default' con acceso mínimo a los buckets S3 (usado si create_iam_s3_policy = true)."
  type        = string
  default     = "grp-erp-s3"
}

variable "create_budget" {
  description = "Mantener false si el Budget fue creado en Paso A por el administrador raíz (los Budgets requieren permisos en Tenancy raíz)."
  type        = bool
  default     = false
}

variable "budget_amount_usd" {
  description = "Monto mensual del presupuesto de alerta en USD (usado si create_budget = true)."
  type        = number
  default     = 1
}

variable "budget_alert_email" {
  description = "Correo electrónico para recibir alertas de presupuesto OCI (requerido solo si create_budget = true)."
  type        = string
  default     = ""
}

variable "create_compartment_quota" {
  description = "Opcional: aplica Compartment Quota estricta (máx. 2 OCPUs A1). Recomendado en cuentas PAYG."
  type        = bool
  default     = false
}

variable "enable_bucket_lifecycle" {
  description = "Opcional: borrado automático en OCI Object Storage >30d (requiere política previa para service objectstorage-us-ashburn-1 en root)."
  type        = bool
  default     = false
}

# --- Red VCN Zero-Trust ---

variable "vcn_cidr" {
  description = "Bloque CIDR de la VCN."
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR de la subred pública (VM 1 Apps + Caddy)."
  type        = string
  default     = "10.0.0.0/24"
}

variable "private_subnet_cidr" {
  description = "CIDR de la subred privada (VM 2 PostgreSQL 16)."
  type        = string
  default     = "10.0.1.0/24"
}

variable "vm1_private_ip" {
  description = "IP privada fija para VM 1 dentro de la subred pública."
  type        = string
  default     = "10.0.0.10"
}

variable "vm2_private_ip" {
  description = "IP privada fija para VM 2 dentro de la subred privada (DB_PRIVATE_HOST)."
  type        = string
  default     = "10.0.1.20"
}

variable "allowed_ssh_cidrs" {
  description = "CIDRs autorizados para SSH (puerto 22) hacia VM 1. Usar ['0.0.0.0/0'] mientras GitHub Actions hosted runners desplieguen por SSH (protegido con Ed25519 + fail2ban)."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

# --- Compute Ampere A1 ARM64 (Always Free 2 OCPUs / 12 GB RAM / 200 GB Block totales) ---

variable "create_compute" {
  description = "Si es true aprovisiona VM 1 y VM 2. Pon false si primero quieres crear Red/Storage y lanzar las VMs desde Consola OCI para reintentar Availability Domains."
  type        = bool
  default     = true
}

variable "availability_domain_index" {
  description = "Índice del Availability Domain en us-ashburn-1 (0 = AD-1, 1 = AD-2, 2 = AD-3). Cambiar si hay Out of host capacity."
  type        = number
  default     = 0
}

variable "ssh_public_key" {
  description = "Llave pública SSH (Ed25519 recomendada) para acceso administrativo a VM 1 y VM 2 (usuario opc)."
  type        = string
}

variable "vm1_ocpus" {
  description = "OCPUs para VM 1 (Apps)."
  type        = number
  default     = 1
}

variable "vm1_memory_gb" {
  description = "RAM en GB para VM 1 (Apps)."
  type        = number
  default     = 8
}

variable "vm1_boot_volume_gb" {
  description = "Boot Volume en GB para VM 1 (mínimo OCI 50 GB; 60 GB deja 80 GB libres de los 200 GB Always Free)."
  type        = number
  default     = 60
}

variable "vm2_ocpus" {
  description = "OCPUs para VM 2 (PostgreSQL 16)."
  type        = number
  default     = 1
}

variable "vm2_memory_gb" {
  description = "RAM en GB para VM 2 (PostgreSQL 16)."
  type        = number
  default     = 4
}

variable "vm2_boot_volume_gb" {
  description = "Boot Volume en GB para VM 2 (mínimo OCI 50 GB; 60 GB + 60 GB = 120 GB, reserva flexible de 80 GB)."
  type        = number
  default     = 60
}
