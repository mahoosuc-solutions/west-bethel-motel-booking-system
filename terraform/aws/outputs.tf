# AWS Infrastructure Outputs

# VPC Outputs
output "vpc_id" {
  description = "ID of the VPC"
  value       = module.vpc.vpc_id
}

output "vpc_cidr" {
  description = "CIDR block of the VPC"
  value       = module.vpc.vpc_cidr
}

output "public_subnet_ids" {
  description = "IDs of public subnets"
  value       = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  description = "IDs of private subnets"
  value       = module.vpc.private_subnet_ids
}

# Database Outputs
output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = module.rds.endpoint
  sensitive   = true
}

output "rds_database_name" {
  description = "Name of the RDS database"
  value       = module.rds.database_name
}

output "rds_secret_arn" {
  description = "ARN of the RDS secret in Secrets Manager"
  value       = module.rds.secret_arn
  sensitive   = true
}

# Redis Outputs
output "redis_endpoint" {
  description = "ElastiCache Redis endpoint"
  value       = module.elasticache.endpoint
  sensitive   = true
}

output "redis_port" {
  description = "ElastiCache Redis port"
  value       = module.elasticache.port
}

# Load Balancer Outputs
output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = module.alb.dns_name
}

output "alb_url" {
  description = "URL of the Application Load Balancer"
  value       = "https://${module.alb.dns_name}"
}

output "alb_zone_id" {
  description = "Route53 zone ID of the ALB"
  value       = module.alb.zone_id
}

# ECS Outputs
output "ecs_cluster_name" {
  description = "Name of the ECS cluster"
  value       = module.ecs.cluster_name
}

output "ecs_service_name" {
  description = "Name of the ECS service"
  value       = module.ecs.service_name
}

output "ecs_task_definition_arn" {
  description = "ARN of the ECS task definition"
  value       = module.ecs.task_definition_arn
}

# S3 Outputs
output "s3_bucket_name" {
  description = "Name of the S3 bucket"
  value       = module.s3.bucket_name
}

output "s3_bucket_arn" {
  description = "ARN of the S3 bucket"
  value       = module.s3.bucket_arn
}

# Security Outputs
output "jwt_secret_arn" {
  description = "ARN of the JWT secret"
  value       = aws_secretsmanager_secret.jwt_secret.arn
  sensitive   = true
}

# DNS Outputs
output "application_domain" {
  description = "Application domain name"
  value       = var.enable_route53 ? var.domain_name : module.alb.dns_name
}

output "application_url" {
  description = "Full application URL"
  value       = var.enable_route53 ? "https://${var.domain_name}" : "https://${module.alb.dns_name}"
}

# Monitoring Outputs
output "cloudwatch_log_group" {
  description = "CloudWatch log group name"
  value       = module.ecs.log_group_name
}

# Connection Information (for deployment scripts)
output "deployment_info" {
  description = "Deployment connection information"
  value = {
    region              = var.aws_region
    ecs_cluster         = module.ecs.cluster_name
    ecs_service         = module.ecs.service_name
    alb_dns             = module.alb.dns_name
    environment         = var.environment
  }
  sensitive = false
}
