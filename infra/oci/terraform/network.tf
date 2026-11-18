# ==============================================================================
# Red VCN Zero-Trust — Subred Pública (VM 1) + Subred Privada (VM 2)
# TASK-609 / ADR-0026
# ==============================================================================

data "oci_core_services" "all_oci_services" {
  filter {
    name   = "name"
    values = ["All .* Services In Oracle Services Network"]
    regex  = true
  }
}

resource "oci_core_vcn" "main" {
  compartment_id = local.compartment_id
  cidr_blocks    = [var.vcn_cidr]
  display_name   = "vcn-${var.project_prefix}"
  dns_label      = "vcnerp"
}

# Gateways ($0.00 en OCI Always Free & PAYG)
resource "oci_core_internet_gateway" "igw" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "igw-${var.project_prefix}"
  enabled        = true
}

resource "oci_core_nat_gateway" "nat" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "nat-${var.project_prefix}"
  block_traffic  = false
}

resource "oci_core_service_gateway" "sgw" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "sgw-${var.project_prefix}"

  services {
    service_id = data.oci_core_services.all_oci_services.services[0].id
  }
}

# Tablas de Ruteo
resource "oci_core_route_table" "rt_public" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "rt-${var.project_prefix}-public"

  route_rules {
    description       = "Salida a Internet para VM 1 (Caddy ACME, GHCR, respuestas HTTPS)"
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
    network_entity_id = oci_core_internet_gateway.igw.id
  }
}

resource "oci_core_route_table" "rt_private" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "rt-${var.project_prefix}-private"

  route_rules {
    description       = "Tráfico privado por backbone OCI hacia Object Storage (backups GPG) y repos OL9"
    destination       = data.oci_core_services.all_oci_services.services[0].cidr_block
    destination_type  = "SERVICE_CIDR_BLOCK"
    network_entity_id = oci_core_service_gateway.sgw.id
  }

  route_rules {
    description       = "Salida controlada NAT para docker pull postgres:16-alpine y parches SO (sin IP pública)"
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
    network_entity_id = oci_core_nat_gateway.nat.id
  }
}

# Security List vacía para anular el Default Security List permisivo de OCI;
# todo el control de tráfico se aplica vía NSGs Zero-Trust.
resource "oci_core_security_list" "empty_sl" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "sl-${var.project_prefix}-zero-trust-empty"
}

# Subredes
resource "oci_core_subnet" "public_apps" {
  compartment_id             = local.compartment_id
  vcn_id                     = oci_core_vcn.main.id
  cidr_block                 = var.public_subnet_cidr
  display_name               = "snet-${var.project_prefix}-public-apps"
  dns_label                  = "pubapps"
  prohibit_public_ip_on_vnic = false
  route_table_id             = oci_core_route_table.rt_public.id
  security_list_ids          = [oci_core_security_list.empty_sl.id]
}

resource "oci_core_subnet" "private_db" {
  compartment_id             = local.compartment_id
  vcn_id                     = oci_core_vcn.main.id
  cidr_block                 = var.private_subnet_cidr
  display_name               = "snet-${var.project_prefix}-private-db"
  dns_label                  = "privdb"
  prohibit_public_ip_on_vnic = true
  route_table_id             = oci_core_route_table.rt_private.id
  security_list_ids          = [oci_core_security_list.empty_sl.id]
}

# Network Security Group — Apps (VM 1)
resource "oci_core_network_security_group" "nsg_apps" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "nsg-${var.project_prefix}-apps"
}

resource "oci_core_network_security_group_security_rule" "apps_ingress_http" {
  network_security_group_id = oci_core_network_security_group.nsg_apps.id
  direction                 = "INGRESS"
  protocol                  = "6" # TCP
  source                    = "0.0.0.0/0"
  source_type               = "CIDR_BLOCK"
  description               = "HTTP 80 para redirección HTTPS y ACME HTTP-01 de Caddy"

  tcp_options {
    destination_port_range {
      min = 80
      max = 80
    }
  }
}

resource "oci_core_network_security_group_security_rule" "apps_ingress_https_tcp" {
  network_security_group_id = oci_core_network_security_group.nsg_apps.id
  direction                 = "INGRESS"
  protocol                  = "6" # TCP
  source                    = "0.0.0.0/0"
  source_type               = "CIDR_BLOCK"
  description               = "HTTPS 443 TCP (ERP, Backoffice, API, Dozzle)"

  tcp_options {
    destination_port_range {
      min = 443
      max = 443
    }
  }
}

resource "oci_core_network_security_group_security_rule" "apps_ingress_https_udp" {
  network_security_group_id = oci_core_network_security_group.nsg_apps.id
  direction                 = "INGRESS"
  protocol                  = "17" # UDP
  source                    = "0.0.0.0/0"
  source_type               = "CIDR_BLOCK"
  description               = "HTTPS 443 UDP (HTTP/3 QUIC expuesto por Caddy)"

  udp_options {
    destination_port_range {
      min = 443
      max = 443
    }
  }
}

resource "oci_core_network_security_group_security_rule" "apps_ingress_ssh" {
  for_each                  = toset(var.allowed_ssh_cidrs)
  network_security_group_id = oci_core_network_security_group.nsg_apps.id
  direction                 = "INGRESS"
  protocol                  = "6" # TCP
  source                    = each.value
  source_type               = "CIDR_BLOCK"
  description               = "SSH 22 para administración y GitHub Actions CD (Ed25519 + fail2ban)"

  tcp_options {
    destination_port_range {
      min = 22
      max = 22
    }
  }
}

resource "oci_core_network_security_group_security_rule" "apps_egress_all" {
  network_security_group_id = oci_core_network_security_group.nsg_apps.id
  direction                 = "EGRESS"
  protocol                  = "all"
  destination               = "0.0.0.0/0"
  destination_type          = "CIDR_BLOCK"
  description               = "Egreso hacia VM 2 (5432), GHCR, Let's Encrypt y Object Storage"
}

# Network Security Group — Base de Datos (VM 2 Zero-Trust)
resource "oci_core_network_security_group" "nsg_db" {
  compartment_id = local.compartment_id
  vcn_id         = oci_core_vcn.main.id
  display_name   = "nsg-${var.project_prefix}-db"
}

resource "oci_core_network_security_group_security_rule" "db_ingress_postgres_from_apps" {
  network_security_group_id = oci_core_network_security_group.nsg_db.id
  direction                 = "INGRESS"
  protocol                  = "6" # TCP
  source                    = oci_core_network_security_group.nsg_apps.id
  source_type               = "NETWORK_SECURITY_GROUP"
  description               = "PostgreSQL 5432 permitido EXCLUSIVAMENTE desde nsg-apps (VM 1)"

  tcp_options {
    destination_port_range {
      min = 5432
      max = 5432
    }
  }
}

resource "oci_core_network_security_group_security_rule" "db_ingress_ssh_from_apps" {
  network_security_group_id = oci_core_network_security_group.nsg_db.id
  direction                 = "INGRESS"
  protocol                  = "6" # TCP
  source                    = oci_core_network_security_group.nsg_apps.id
  source_type               = "NETWORK_SECURITY_GROUP"
  description               = "SSH 22 ProxyJump permitido EXCLUSIVAMENTE desde nsg-apps (VM 1)"

  tcp_options {
    destination_port_range {
      min = 22
      max = 22
    }
  }
}

resource "oci_core_network_security_group_security_rule" "db_egress_all" {
  network_security_group_id = oci_core_network_security_group.nsg_db.id
  direction                 = "EGRESS"
  protocol                  = "all"
  destination               = "0.0.0.0/0"
  destination_type          = "CIDR_BLOCK"
  description               = "Egreso controlado vía Service GW (Object Storage) y NAT GW (Docker/dnf)"
}
