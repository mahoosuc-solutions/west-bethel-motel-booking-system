# Database Module Variables

variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "region" {
  description = "GCP region"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "network_id" {
  description = "VPC network ID"
  type        = string
}

variable "private_network_id" {
  description = "Private VPC network ID for service networking"
  type        = string
}

variable "database_tier" {
  description = "Database instance tier"
  type        = string
  default     = "db-f1-micro"
}

variable "database_version" {
  description = "PostgreSQL version"
  type        = string
  default     = "POSTGRES_15"
}

variable "database_name" {
  description = "Database name"
  type        = string
  default     = "motel_booking"
}

variable "db_username" {
  description = "Database username"
  type        = string
  default     = "motel_app"
}

variable "disk_size" {
  description = "Disk size in GB"
  type        = number
  default     = 10
}

variable "backup_enabled" {
  description = "Enable automated backups"
  type        = bool
  default     = true
}

variable "ha_enabled" {
  description = "Enable high availability"
  type        = bool
  default     = false
}
