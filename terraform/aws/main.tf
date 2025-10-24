# Main AWS Infrastructure Configuration for West Bethel Motel Booking System
# This creates a complete production-ready infrastructure on AWS

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "West Bethel Motel Booking System"
      Environment = var.environment
      ManagedBy   = "Terraform"
      Owner       = var.owner
    }
  }
}

# VPC and Networking
module "vpc" {
  source = "./modules/vpc"

  project_name       = var.project_name
  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
  enable_nat_gateway = var.enable_nat_gateway
  enable_vpn_gateway = var.enable_vpn_gateway
}

# Security Groups
module "security_groups" {
  source = "./modules/security-groups"

  project_name = var.project_name
  environment  = var.environment
  vpc_id       = module.vpc.vpc_id
}

# RDS PostgreSQL Database
module "rds" {
  source = "./modules/rds"

  project_name          = var.project_name
  environment           = var.environment
  vpc_id                = module.vpc.vpc_id
  database_subnet_ids   = module.vpc.database_subnet_ids
  security_group_id     = module.security_groups.database_security_group_id
  instance_class        = var.db_instance_class
  allocated_storage     = var.db_allocated_storage
  engine_version        = var.db_engine_version
  multi_az              = var.db_multi_az
  backup_retention_days = var.db_backup_retention_days
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
}

# ElastiCache Redis
module "elasticache" {
  source = "./modules/elasticache"

  project_name      = var.project_name
  environment       = var.environment
  vpc_id            = module.vpc.vpc_id
  cache_subnet_ids  = module.vpc.cache_subnet_ids
  security_group_id = module.security_groups.cache_security_group_id
  node_type         = var.redis_node_type
  num_cache_nodes   = var.redis_num_nodes
  engine_version    = var.redis_engine_version
  automatic_failover_enabled = var.redis_automatic_failover
}

# Application Load Balancer
module "alb" {
  source = "./modules/alb"

  project_name      = var.project_name
  environment       = var.environment
  vpc_id            = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnet_ids
  security_group_id = module.security_groups.alb_security_group_id
  certificate_arn   = var.ssl_certificate_arn
  enable_waf        = var.enable_waf
}

# ECS Cluster
module "ecs" {
  source = "./modules/ecs"

  project_name               = var.project_name
  environment                = var.environment
  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
  security_group_id          = module.security_groups.ecs_security_group_id
  alb_target_group_arn       = module.alb.target_group_arn
  container_image            = var.container_image
  container_cpu              = var.container_cpu
  container_memory           = var.container_memory
  desired_count              = var.ecs_desired_count
  min_capacity               = var.ecs_min_capacity
  max_capacity               = var.ecs_max_capacity
  db_host                    = module.rds.endpoint
  db_name                    = module.rds.database_name
  db_secret_arn              = module.rds.secret_arn
  redis_host                 = module.elasticache.endpoint
  redis_port                 = module.elasticache.port
  log_retention_days         = var.log_retention_days
}

# IAM Roles and Policies
module "iam" {
  source = "./modules/iam"

  project_name = var.project_name
  environment  = var.environment
  secret_arns  = [module.rds.secret_arn]
}

# CloudWatch Monitoring and Alarms
module "monitoring" {
  source = "./modules/monitoring"

  project_name        = var.project_name
  environment         = var.environment
  ecs_cluster_name    = module.ecs.cluster_name
  ecs_service_name    = module.ecs.service_name
  alb_arn_suffix      = module.alb.alb_arn_suffix
  target_group_suffix = module.alb.target_group_arn_suffix
  rds_instance_id     = module.rds.instance_id
  redis_cluster_id    = module.elasticache.cluster_id
  sns_topic_arn       = var.sns_alert_topic_arn
}

# S3 Bucket for Application Assets and Backups
module "s3" {
  source = "./modules/s3"

  project_name = var.project_name
  environment  = var.environment
  enable_versioning = true
  enable_encryption = true
  lifecycle_rules_enabled = true
}

# Secrets Manager for Application Secrets
resource "aws_secretsmanager_secret" "jwt_secret" {
  name_prefix             = "${var.project_name}-${var.environment}-jwt-"
  description             = "JWT secret for authentication"
  recovery_window_in_days = 7

  tags = {
    Name = "${var.project_name}-${var.environment}-jwt-secret"
  }
}

resource "random_password" "jwt_secret" {
  length  = 64
  special = true
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = random_password.jwt_secret.result
}

# Route53 DNS (optional)
module "route53" {
  source = "./modules/route53"
  count  = var.enable_route53 ? 1 : 0

  project_name    = var.project_name
  environment     = var.environment
  domain_name     = var.domain_name
  alb_dns_name    = module.alb.dns_name
  alb_zone_id     = module.alb.zone_id
}

# WAF (Web Application Firewall)
module "waf" {
  source = "./modules/waf"
  count  = var.enable_waf ? 1 : 0

  project_name = var.project_name
  environment  = var.environment
  alb_arn      = module.alb.alb_arn
}
