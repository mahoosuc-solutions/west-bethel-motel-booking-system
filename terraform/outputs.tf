# Terraform Outputs

output "project_id" {
  description = "GCP Project ID"
  value       = var.project_id
}

output "region" {
  description = "Deployment region"
  value       = var.region
}

output "environment" {
  description = "Environment name"
  value       = var.environment
}

output "application_url" {
  description = "URL of the deployed application"
  value       = module.compute.service_url
}

output "database_instance_name" {
  description = "Database instance name"
  value       = module.database.instance_name
}

output "database_connection_name" {
  description = "Database connection name for Cloud SQL Proxy"
  value       = module.database.connection_name
  sensitive   = true
}

output "database_ip" {
  description = "Database private IP address"
  value       = module.database.private_ip
  sensitive   = true
}

output "redis_host" {
  description = "Redis host address"
  value       = module.redis.host
}

output "redis_port" {
  description = "Redis port"
  value       = module.redis.port
}

output "redis_instance_id" {
  description = "Redis instance ID"
  value       = module.redis.instance_id
}

output "network_name" {
  description = "VPC network name"
  value       = module.networking.network_name
}

output "subnet_name" {
  description = "Subnet name"
  value       = module.networking.subnet_name
}

output "load_balancer_ip" {
  description = "Load balancer external IP address"
  value       = module.networking.load_balancer_ip
}

output "service_name" {
  description = "Cloud Run service name"
  value       = module.compute.service_name
}

output "monitoring_dashboard_url" {
  description = "URL to monitoring dashboard"
  value       = module.monitoring.dashboard_url
}

output "deployment_summary" {
  description = "Summary of deployed resources"
  value = {
    environment        = var.environment
    application_url    = module.compute.service_url
    database_version   = var.database_version
    redis_tier         = var.redis_tier
    min_instances      = var.min_instances
    max_instances      = var.max_instances
    high_availability  = var.ha_enabled
    backup_enabled     = var.backup_enabled
  }
}
