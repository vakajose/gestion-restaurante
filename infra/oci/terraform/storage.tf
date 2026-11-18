# ==============================================================================
# OCI Object Storage (S3 Compatible) — Medios + Backups Cifrados
# TASK-609 / ADR-0026 / ADR-0028
# ==============================================================================

data "oci_objectstorage_namespace" "ns" {
  compartment_id = local.compartment_id
}

# Bucket privado para respaldos cifrados GPG AES-256 de PostgreSQL (scripts/db/backup-postgres-s3.sh)
resource "oci_objectstorage_bucket" "db_backups" {
  compartment_id = local.compartment_id
  namespace      = data.oci_objectstorage_namespace.ns.namespace
  name           = "${var.project_prefix}-db-backups"
  access_type    = "NoPublicAccess"
  storage_tier   = "Standard"
  versioning     = "Disabled"

  freeform_tags = {
    "Project"     = var.project_prefix
    "Purpose"     = "postgresql-gpg-backups"
    "Environment" = "shared"
  }
}

# Retención automática en Object Storage de backups antiguos (> 30 días como respaldo adicional al script de 14 días)
resource "oci_objectstorage_object_lifecycle_policy" "db_backups_retention" {
  count     = var.enable_bucket_lifecycle ? 1 : 0
  namespace = data.oci_objectstorage_namespace.ns.namespace
  bucket    = oci_objectstorage_bucket.db_backups.name

  rules {
    action      = "DELETE"
    is_enabled  = true
    name        = "delete-backups-older-than-30d"
    target      = "objects"
    time_amount = 30
    time_unit   = "DAYS"
  }
}

# Bucket de medios para Producción (lectura directa de imágenes por URL exacta sin permitir listar el bucket)
resource "oci_objectstorage_bucket" "media_prod" {
  compartment_id = local.compartment_id
  namespace      = data.oci_objectstorage_namespace.ns.namespace
  name           = "${var.project_prefix}-media-prod"
  access_type    = "ObjectReadWithoutList"
  storage_tier   = "Standard"
  versioning     = "Disabled"

  freeform_tags = {
    "Project"     = var.project_prefix
    "Purpose"     = "catalog-media"
    "Environment" = "prod"
  }
}

# Bucket de medios aislado para Staging (evita mezclar fotos de prueba con producción)
resource "oci_objectstorage_bucket" "media_staging" {
  compartment_id = local.compartment_id
  namespace      = data.oci_objectstorage_namespace.ns.namespace
  name           = "${var.project_prefix}-media-staging"
  access_type    = "ObjectReadWithoutList"
  storage_tier   = "Standard"
  versioning     = "Disabled"

  freeform_tags = {
    "Project"     = var.project_prefix
    "Purpose"     = "catalog-media"
    "Environment" = "staging"
  }
}
