# Redis Module Outputs

output "instance_id" {
  description = "Redis instance ID"
  value       = google_redis_instance.cache.id
}

output "host" {
  description = "Redis host address"
  value       = google_redis_instance.cache.host
}

output "port" {
  description = "Redis port"
  value       = google_redis_instance.cache.port
}

output "current_location_id" {
  description = "Current location ID"
  value       = google_redis_instance.cache.current_location_id
}

output "connection_string" {
  description = "Redis connection string"
  value       = "${google_redis_instance.cache.host}:${google_redis_instance.cache.port}"
}
