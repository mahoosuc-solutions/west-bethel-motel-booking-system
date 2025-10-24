# Infrastructure as Code Guide

## Overview

This guide covers the infrastructure setup using Terraform for AWS and GCP deployments of the West Bethel Motel Booking System.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [AWS Infrastructure](#aws-infrastructure)
3. [GCP Infrastructure](#gcp-infrastructure)
4. [Terraform Usage](#terraform-usage)
5. [Resource Management](#resource-management)
6. [Cost Optimization](#cost-optimization)

---

## Architecture Overview

### Infrastructure Components

```
Production Infrastructure Stack:
├── Networking (VPC, Subnets, Security Groups)
├── Compute (ECS/EKS or GKE)
├── Database (RDS PostgreSQL or Cloud SQL)
├── Cache (ElastiCache Redis or Memorystore)
├── Load Balancer (ALB or Cloud Load Balancer)
├── Storage (S3 or Cloud Storage)
├── Monitoring (CloudWatch or Cloud Monitoring)
└── Security (WAF, Secrets Manager, IAM)
```

---

## AWS Infrastructure

### Directory Structure

```
terraform/aws/
├── main.tf              # Main configuration
├── variables.tf         # Input variables
├── outputs.tf           # Output values
├── modules/
│   ├── vpc/            # VPC and networking
│   ├── rds/            # PostgreSQL database
│   ├── elasticache/    # Redis cache
│   ├── ecs/            # Container orchestration
│   ├── alb/            # Load balancer
│   ├── security-groups/# Security rules
│   ├── iam/            # IAM roles and policies
│   ├── monitoring/     # CloudWatch alarms
│   ├── s3/             # Storage buckets
│   ├── route53/        # DNS management
│   └── waf/            # Web Application Firewall
└── environments/
    ├── dev/
    ├── staging/
    └── production/
```

### Quick Start

```bash
# Navigate to AWS Terraform directory
cd terraform/aws

# Initialize Terraform
terraform init

# Create production infrastructure
terraform workspace new production
terraform plan -var-file=environments/production/terraform.tfvars
terraform apply -var-file=environments/production/terraform.tfvars
```

### AWS Resources Created

1. **VPC** (10.0.0.0/16)
   - 3 Public Subnets (ALB)
   - 3 Private Subnets (ECS Tasks)
   - 3 Database Subnets (RDS)
   - 3 Cache Subnets (ElastiCache)
   - NAT Gateways (HA)
   - VPC Endpoints (S3, ECR)

2. **RDS PostgreSQL**
   - Multi-AZ deployment
   - Automated backups (30 days)
   - Encryption at rest
   - Performance Insights enabled

3. **ElastiCache Redis**
   - Redis 7.0
   - Multi-AZ with automatic failover
   - Encryption in transit

4. **ECS Cluster**
   - Fargate launch type
   - Auto-scaling (2-10 tasks)
   - Service discovery
   - CloudWatch Logs

5. **Application Load Balancer**
   - HTTPS/TLS termination
   - Health checks
   - Access logs to S3
   - WAF integration

6. **Security**
   - Security groups (least privilege)
   - IAM roles and policies
   - Secrets Manager
   - AWS WAF

---

## GCP Infrastructure

### Directory Structure

```
terraform/
├── main.tf              # GCP configuration
├── variables.tf
├── outputs.tf
├── modules/
│   ├── networking/     # VPC and subnets
│   ├── database/       # Cloud SQL
│   ├── redis/          # Memorystore
│   ├── compute/        # Cloud Run or GKE
│   └── monitoring/     # Cloud Monitoring
└── environments/
    └── production/
```

### GCP Resources Created

1. **VPC Network**
   - Custom subnets
   - Cloud NAT
   - Firewall rules

2. **Cloud SQL PostgreSQL**
   - High availability
   - Automated backups
   - Private IP

3. **Memorystore Redis**
   - Standard tier
   - HA configuration

4. **GKE Cluster** or **Cloud Run**
   - Auto-scaling
   - Workload Identity
   - Binary Authorization

---

## Terraform Usage

### Installation

```bash
# Install Terraform
brew install terraform  # macOS
# or
wget https://releases.hashicorp.com/terraform/1.6.0/terraform_1.6.0_linux_amd64.zip
unzip terraform_1.6.0_linux_amd64.zip
sudo mv terraform /usr/local/bin/
```

### Basic Commands

```bash
# Initialize
terraform init

# Plan changes
terraform plan -var-file=environments/production/terraform.tfvars

# Apply changes
terraform apply -var-file=environments/production/terraform.tfvars

# Destroy infrastructure
terraform destroy -var-file=environments/production/terraform.tfvars

# Show state
terraform show

# List resources
terraform state list
```

### Configuration Files

**environments/production/terraform.tfvars:**
```hcl
project_name = "west-bethel-motel"
environment  = "production"
aws_region   = "us-east-1"

# VPC
vpc_cidr = "10.0.0.0/16"
availability_zones = ["us-east-1a", "us-east-1b", "us-east-1c"]

# Database
db_instance_class = "db.t3.medium"
db_allocated_storage = 100
db_multi_az = true
db_backup_retention_days = 30

# Cache
redis_node_type = "cache.t3.medium"
redis_num_nodes = 2

# ECS
container_image = "ghcr.io/westbethel/motel-booking-system:latest"
ecs_desired_count = 3
ecs_min_capacity = 2
ecs_max_capacity = 10

# Security
enable_waf = true
ssl_certificate_arn = "arn:aws:acm:us-east-1:ACCOUNT:certificate/CERT_ID"
```

---

## Resource Management

### State Management

```bash
# Use S3 backend for state (recommended)
terraform {
  backend "s3" {
    bucket         = "motel-booking-terraform-state"
    key            = "production/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "terraform-state-lock"
  }
}
```

### Workspaces

```bash
# Create workspace
terraform workspace new production

# List workspaces
terraform workspace list

# Select workspace
terraform workspace select production

# Delete workspace
terraform workspace delete staging
```

### Import Existing Resources

```bash
# Import RDS instance
terraform import module.rds.aws_db_instance.main motel-booking-db

# Import ECS cluster
terraform import module.ecs.aws_ecs_cluster.main motel-booking-cluster
```

---

## Cost Optimization

### Estimated Monthly Costs

**Production (AWS):**
```
RDS (db.t3.medium, Multi-AZ): $150
ElastiCache (cache.t3.medium x2): $100
ECS Fargate (3 tasks): $100
ALB: $25
NAT Gateways (3): $100
CloudWatch Logs: $10
Total: ~$485/month
```

**Optimization Tips:**

1. **Use Reserved Instances** for predictable workloads
2. **Auto-scaling** to match demand
3. **S3 Lifecycle Policies** for old backups
4. **CloudWatch Log Retention** limits
5. **Spot Instances** for non-critical workloads

### Resource Tagging

```hcl
default_tags {
  tags = {
    Project     = "West Bethel Motel"
    Environment = var.environment
    ManagedBy   = "Terraform"
    CostCenter  = "Engineering"
  }
}
```

---

## Security Best Practices

1. **Never commit secrets** to version control
2. **Use Secrets Manager** for credentials
3. **Enable encryption** at rest and in transit
4. **Implement least privilege** IAM policies
5. **Use private subnets** for compute resources
6. **Enable VPC Flow Logs** for audit
7. **Rotate credentials** regularly
8. **Enable MFA** for AWS console access

---

## Disaster Recovery

### Backup Strategy

1. **Database:** Automated daily backups (30-day retention)
2. **State Files:** Versioned in S3
3. **Application Data:** S3 cross-region replication

### Recovery Procedures

```bash
# Restore RDS from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier motel-booking-db-restored \
  --db-snapshot-identifier motel-booking-backup-20241023

# Restore Terraform state
terraform state pull > backup.tfstate
```

---

**Last Updated:** 2024-10-23
**Version:** 1.0.0
