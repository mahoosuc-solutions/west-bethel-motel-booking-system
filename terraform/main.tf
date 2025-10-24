terraform {
  required_version = ">= 1.5.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }
}

# Main configuration for West Bethel Motel Booking System
# This orchestrates all infrastructure modules

module "networking" {
  source = "./modules/networking"

  project_id   = var.project_id
  region       = var.region
  environment  = var.environment
  network_name = "${var.project_name}-${var.environment}"
}

module "database" {
  source = "./modules/database"

  project_id         = var.project_id
  region             = var.region
  environment        = var.environment
  network_id         = module.networking.network_id
  private_network_id = module.networking.private_network_id
  database_tier      = var.database_tier
  database_version   = var.database_version
  backup_enabled     = var.backup_enabled
  ha_enabled         = var.ha_enabled
}

module "redis" {
  source = "./modules/redis"

  project_id  = var.project_id
  region      = var.region
  environment = var.environment
  network_id  = module.networking.network_id
  redis_tier  = var.redis_tier
  redis_size  = var.redis_size
}

module "compute" {
  source = "./modules/compute"

  project_id        = var.project_id
  region            = var.region
  environment       = var.environment
  network_id        = module.networking.network_id
  subnet_id         = module.networking.subnet_id
  database_host     = module.database.connection_name
  redis_host        = module.redis.host
  redis_port        = module.redis.port
  container_image   = var.container_image
  min_instances     = var.min_instances
  max_instances     = var.max_instances
  cpu_limit         = var.cpu_limit
  memory_limit      = var.memory_limit
  db_secret_id      = module.database.db_secret_id
}

module "monitoring" {
  source = "./modules/monitoring"

  project_id      = var.project_id
  region          = var.region
  environment     = var.environment
  service_name    = module.compute.service_name
  notification_channels = var.notification_channels
}

# Output important values
output "application_url" {
  description = "URL of the deployed application"
  value       = module.compute.service_url
}

output "database_connection" {
  description = "Database connection name"
  value       = module.database.connection_name
  sensitive   = true
}

output "redis_host" {
  description = "Redis host address"
  value       = module.redis.host
}

output "load_balancer_ip" {
  description = "Load balancer IP address"
  value       = module.networking.load_balancer_ip
}
