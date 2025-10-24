# Development Environment Configuration

terraform {
  backend "gcs" {
    bucket = "west-bethel-motel-terraform-state"
    prefix = "terraform/state/dev"
  }
}

module "infrastructure" {
  source = "../../"

  project_id   = var.project_id
  project_name = "west-bethel-motel"
  region       = "us-east1"
  environment  = "dev"

  # Database - Minimal resources for dev
  database_tier    = "db-f1-micro"
  database_version = "POSTGRES_15"
  backup_enabled   = false
  ha_enabled       = false

  # Redis - Basic tier for dev
  redis_tier = "BASIC"
  redis_size = 1

  # Compute - Minimal scaling for dev
  container_image = "gcr.io/${var.project_id}/motel-booking:dev"
  min_instances   = 0
  max_instances   = 2
  cpu_limit       = "1000m"
  memory_limit    = "512Mi"

  # Monitoring
  notification_channels = []
}

output "dev_url" {
  value = module.infrastructure.application_url
}
