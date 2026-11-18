# ==============================================================================
# Providers & Version Constraints — OCI Sovereign Stack (TASK-609)
# Compatible con OCI Resource Manager (Resource Principal), Cloud Shell y CLI local.
# ==============================================================================

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    oci = {
      source  = "oracle/oci"
      version = ">= 5.30.0"
    }
  }
}

# Provider principal en la región de despliegue (por defecto us-ashburn-1)
provider "oci" {
  tenancy_ocid = var.tenancy_ocid
  region       = var.region
}

# Provider explícito para Home Region (obligatorio para recursos IAM y Budgets en OCI)
provider "oci" {
  alias        = "home"
  tenancy_ocid = var.tenancy_ocid
  region       = var.home_region
}
