# ==============================================================================
# Compute Ampere A1 ARM64 (Always Free: 2 OCPUs / 12 GB RAM / 120 GB de 200 GB)
# Oracle Linux 9 aarch64 — VM 1 (Apps) + VM 2 (PostgreSQL 16 Privada)
# TASK-609 / ADR-0026
# ==============================================================================

data "oci_identity_availability_domains" "ads" {
  compartment_id = local.compartment_id
}

data "oci_core_images" "ol9_aarch64" {
  compartment_id           = local.compartment_id
  operating_system         = "Oracle Linux"
  operating_system_version = "9"
  shape                    = "VM.Standard.A1.Flex"
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"

  filter {
    name   = "display_name"
    values = ["^Oracle-Linux-9\\.[0-9]+-aarch64-20[0-9]{2}\\..*$"]
    regex  = true
  }
}

# ------------------------------------------------------------------------------
# VM 1 — Servidor de Aplicaciones (Caddy + Backend + ERP + Backoffice + Dozzle)
# ------------------------------------------------------------------------------
resource "oci_core_instance" "vm1_apps" {
  count               = var.create_compute ? 1 : 0
  compartment_id      = local.compartment_id
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[var.availability_domain_index].name
  display_name        = "vm-${var.project_prefix}-apps-01"
  shape               = "VM.Standard.A1.Flex"

  shape_config {
    ocpus         = var.vm1_ocpus
    memory_in_gbs = var.vm1_memory_gb
  }

  source_details {
    source_type             = "image"
    source_id               = data.oci_core_images.ol9_aarch64.images[0].id
    boot_volume_size_in_gbs = var.vm1_boot_volume_gb
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.public_apps.id
    display_name     = "vnic-vm1-apps"
    assign_public_ip = false # Se asigna IP Pública Reservada explícita abajo
    private_ip       = var.vm1_private_ip
    nsg_ids          = [oci_core_network_security_group.nsg_apps.id]
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
    user_data           = filebase64("${path.module}/../cloud-init/vm1-apps-ol9.yaml")
  }

  freeform_tags = {
    "Project" = var.project_prefix
    "Role"    = "apps-edge"
  }
}

data "oci_core_vnic_attachments" "vm1_vnics" {
  count          = var.create_compute ? 1 : 0
  compartment_id = local.compartment_id
  instance_id    = oci_core_instance.vm1_apps[0].id
}

data "oci_core_private_ips" "vm1_private_ips" {
  count   = var.create_compute ? 1 : 0
  vnic_id = data.oci_core_vnic_attachments.vm1_vnics[0].vnic_attachments[0].vnic_id
}

# IP Pública Reservada (permanente aunque recrees VM 1; $0.00 mientras esté adjunta)
resource "oci_core_public_ip" "vm1_reserved_ip" {
  count          = var.create_compute ? 1 : 0
  compartment_id = local.compartment_id
  lifetime       = "RESERVED"
  display_name   = "ip-${var.project_prefix}-vm1-reserved"
  private_ip_id  = data.oci_core_private_ips.vm1_private_ips[0].private_ips[0].id
}

# ------------------------------------------------------------------------------
# VM 2 — Servidor Dedicado de Base de Datos PostgreSQL 16 (Subred Privada)
# ------------------------------------------------------------------------------
resource "oci_core_instance" "vm2_db" {
  count               = var.create_compute ? 1 : 0
  compartment_id      = local.compartment_id
  availability_domain = data.oci_identity_availability_domains.ads.availability_domains[var.availability_domain_index].name
  display_name        = "vm-${var.project_prefix}-db-02"
  shape               = "VM.Standard.A1.Flex"

  shape_config {
    ocpus         = var.vm2_ocpus
    memory_in_gbs = var.vm2_memory_gb
  }

  source_details {
    source_type             = "image"
    source_id               = data.oci_core_images.ol9_aarch64.images[0].id
    boot_volume_size_in_gbs = var.vm2_boot_volume_gb
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.private_db.id
    display_name     = "vnic-vm2-db"
    assign_public_ip = false
    private_ip       = var.vm2_private_ip
    nsg_ids          = [oci_core_network_security_group.nsg_db.id]
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
    user_data           = filebase64("${path.module}/../cloud-init/vm2-db-ol9.yaml")
  }

  freeform_tags = {
    "Project" = var.project_prefix
    "Role"    = "database-postgresql"
  }
}
