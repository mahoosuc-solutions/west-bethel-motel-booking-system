# Database Module Outputs

output "instance_name" {
  description = "Database instance name"
  value       = google_sql_database_instance.postgres.name
}

output "connection_name" {
  description = "Database connection name for Cloud SQL Proxy"
  value       = google_sql_database_instance.postgres.connection_name
}

output "database_name" {
  description = "Database name"
  value       = google_sql_database.database.name
}

output "username" {
  description = "Database username"
  value       = google_sql_user.users.name
}

output "password" {
  description = "Database password"
  value       = random_password.db_password.result
  sensitive   = true
}

output "private_ip" {
  description = "Database private IP address"
  value       = google_sql_database_instance.postgres.private_ip_address
}

output "db_secret_id" {
  description = "Secret Manager secret ID for database password"
  value       = google_secret_manager_secret.db_password.secret_id
}

output "read_replica_connection_name" {
  description = "Read replica connection name"
  value       = var.ha_enabled && var.environment == "production" ? google_sql_database_instance.read_replica[0].connection_name : null
}
