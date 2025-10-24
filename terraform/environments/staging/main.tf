# Staging Environment Configuration

terraform {
  backend "gcs" {
    bucket = "west-bethel-motel-terraform-state"
    prefix = "terraform/state/staging"
  }
}

module "infrastructure" {
  source = "../../"

  project_id   = var.project_id
  project_name = "west-bethel-motel"
  region       = "us-east1"
  environment  = "staging"

  # Database - Production-like for staging
  database_tier    = "db-custom-2-4096"
  database_version = "POSTGRES_15"
  backup_enabled   = true
  ha_enabled       = false

  # Redis - Basic tier with more memory
  redis_tier = "BASIC"
  redis_size = 2

  # Compute - Moderate scaling for staging
  container_image = "gcr.io/${var.project_id}/motel-booking:staging"
  min_instances   = 1
  max_instances   = 5
  cpu_limit       = "2000m"
  memory_limit    = "1Gi"

  # Monitoring
  notification_channels = var.notification_channels
}

output "staging_url" {
  value = module.infrastructure.application_url
}
