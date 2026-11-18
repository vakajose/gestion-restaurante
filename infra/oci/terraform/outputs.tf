# ==============================================================================
# Outputs — Valores listos para .env, DuckDNS y GitHub Secrets (TASK-609)
# ==============================================================================

output "compartment_id" {
  description = "OCID del compartment cmp-erp."
  value       = local.compartment_id
}

output "object_storage_namespace" {
  description = "Namespace único de OCI Object Storage para S3_ENDPOINT."
  value       = data.oci_objectstorage_namespace.ns.namespace
}

output "s3_endpoint" {
  description = "Valor exacto para S3_ENDPOINT en .env y backups."
  value       = "https://${data.oci_objectstorage_namespace.ns.namespace}.compat.objectstorage.${var.region}.oraclecloud.com"
}

output "s3_media_prod_public_base_url" {
  description = "Valor exacto para S3_PUBLIC_BASE_URL en Producción."
  value       = "https://${data.oci_objectstorage_namespace.ns.namespace}.compat.objectstorage.${var.region}.oraclecloud.com/${oci_objectstorage_bucket.media_prod.name}"
}

output "s3_media_staging_public_base_url" {
  description = "Valor exacto para STAGING_S3_PUBLIC_BASE_URL en Staging."
  value       = "https://${data.oci_objectstorage_namespace.ns.namespace}.compat.objectstorage.${var.region}.oraclecloud.com/${oci_objectstorage_bucket.media_staging.name}"
}

output "backup_bucket_name" {
  description = "Valor exacto para BACKUP_S3_BUCKET."
  value       = oci_objectstorage_bucket.db_backups.name
}

output "vm1_public_ip" {
  description = "IP pública reservada de VM 1 (usar en DuckDNS y GitHub Secret OCI_VM1_HOST)."
  value       = var.create_compute ? oci_core_public_ip.vm1_reserved_ip[0].ip_address : "pendiente-crear-compute"
}

output "vm1_private_ip" {
  description = "IP privada fija de VM 1 (usar en pg_hba.conf de VM 2)."
  value       = var.create_compute ? oci_core_instance.vm1_apps[0].private_ip : var.vm1_private_ip
}

output "vm2_private_ip" {
  description = "IP privada fija de VM 2 (usar en DB_PRIVATE_HOST del .env de VM 1)."
  value       = var.create_compute ? oci_core_instance.vm2_db[0].private_ip : var.vm2_private_ip
}

output "ol9_aarch64_image_used" {
  description = "Imagen Oracle Linux 9 ARM64 seleccionada."
  value       = data.oci_core_images.ol9_aarch64.images[0].display_name
}
