# Production Environment Configuration

terraform {
  backend "gcs" {
    bucket = "west-bethel-motel-terraform-state"
    prefix = "terraform/state/production"
  }
}

module "infrastructure" {
  source = "../../"

  project_id   = var.project_id
  project_name = "west-bethel-motel"
  region       = "us-east1"
  environment  = "production"

  # Database - Full production specs
  database_tier    = "db-custom-4-16384"
  database_version = "POSTGRES_15"
  backup_enabled   = true
  ha_enabled       = true

  # Redis - High availability
  redis_tier = "STANDARD_HA"
  redis_size = 5

  # Compute - Full auto-scaling
  container_image = "gcr.io/${var.project_id}/motel-booking:latest"
  min_instances   = 2
  max_instances   = 10
  cpu_limit       = "2000m"
  memory_limit    = "2Gi"

  # Monitoring
  notification_channels = var.notification_channels
}

output "production_url" {
  value = module.infrastructure.application_url
}

output "production_summary" {
  value = module.infrastructure.deployment_summary
}
