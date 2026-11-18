# ==============================================================================
# Gobernanza, Compartment, Políticas IAM, Budgets y Quotas (Home Region)
# TASK-609 / Always Free Guardrails
# ==============================================================================

resource "oci_identity_compartment" "main" {
  count          = var.create_compartment ? 1 : 0
  provider       = oci.home
  compartment_id = var.tenancy_ocid
  name           = "cmp-${var.project_prefix}"
  description    = "Compartment aislado para plataforma ERP (VCN, Ampere A1, Object Storage)"
  enable_delete  = true
}

data "oci_identity_compartment" "existing" {
  count    = var.create_compartment ? 0 : 1
  provider = oci.home
  id       = var.existing_compartment_ocid
}

locals {
  compartment_id   = var.create_compartment ? oci_identity_compartment.main[0].id : var.existing_compartment_ocid
  compartment_name = var.create_compartment ? oci_identity_compartment.main[0].name : data.oci_identity_compartment.existing[0].name
}

# Política IAM de mínimo privilegio para usuario/grupo S3 (Identity Domain 'Default')
# Activar create_iam_s3_policy = true una vez creado 'grp-erp-s3' en Consola OCI.
resource "oci_identity_policy" "s3_storage_least_privilege" {
  count          = var.create_iam_s3_policy ? 1 : 0
  provider       = oci.home
  compartment_id = var.tenancy_ocid
  name           = "pol-${var.project_prefix}-s3-storage"
  description    = "Acceso de mínimo privilegio exclusivo a objetos de Object Storage dentro de cmp-${var.project_prefix}"

  statements = [
    "Allow group 'Default'/'${var.s3_service_group_name}' to read buckets in compartment id ${local.compartment_id}",
    "Allow group 'Default'/'${var.s3_service_group_name}' to manage objects in compartment id ${local.compartment_id}"
  ]
}

# Budget mensual de seguridad ($1.00 USD por defecto) con alertas al 50% y 100%
resource "oci_budget_budget" "always_free_guard" {
  count                 = var.create_budget ? 1 : 0
  provider              = oci.home
  compartment_id        = var.tenancy_ocid
  target_type           = "COMPARTMENT"
  targets               = [local.compartment_id]
  display_name          = "bdg-${var.project_prefix}-always-free"
  description           = "Alerta temprana ante cualquier consumo fuera de Always Free en cmp-${var.project_prefix}"
  amount                = var.budget_amount_usd
  reset_period          = "MONTHLY"
  budget_processing_period_start_offset = 1
}

resource "oci_budget_alert_rule" "alert_50_pct" {
  count          = var.create_budget ? 1 : 0
  provider       = oci.home
  budget_id      = oci_budget_budget.always_free_guard[0].id
  display_name   = "alert-50-pct"
  type           = "FORECAST"
  threshold      = 50
  threshold_type = "PERCENTAGE"
  recipients     = var.budget_alert_email
  message        = "ALERTA OCI [${var.project_prefix}]: El gasto proyectado superó el 50% del presupuesto ($${var.budget_amount_usd} USD). Revisa instancias/volúmenes."
}

resource "oci_budget_alert_rule" "alert_100_pct" {
  count          = var.create_budget ? 1 : 0
  provider       = oci.home
  budget_id      = oci_budget_budget.always_free_guard[0].id
  display_name   = "alert-100-pct"
  type           = "ACTUAL"
  threshold      = 100
  threshold_type = "PERCENTAGE"
  recipients     = var.budget_alert_email
  message        = "ALERTA CRITICA OCI [${var.project_prefix}]: Se alcanzó el 100% del presupuesto ($${var.budget_amount_usd} USD) en cmp-${var.project_prefix}."
}

# Opcional (recomendado al activar PAYG): bloquear creación accidental de más de 2 OCPUs A1
resource "oci_limits_quota" "ampere_a1_guard" {
  count          = var.create_compartment_quota ? 1 : 0
  provider       = oci.home
  compartment_id = var.tenancy_ocid
  name           = "qta-${var.project_prefix}-always-free"
  description    = "Tope estricto Always Free (máx 2 OCPUs Ampere A1) para evitar facturación accidental en PAYG"

  statements = [
    "set compute-core quota standard-a1-core-count to 2 in compartment ${local.compartment_name}"
  ]
}
