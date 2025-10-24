# Compute Module Outputs

output "service_name" {
  description = "Cloud Run service name"
  value       = google_cloud_run_service.motel_booking.name
}

output "service_url" {
  description = "Cloud Run service URL"
  value       = google_cloud_run_service.motel_booking.status[0].url
}

output "service_id" {
  description = "Cloud Run service ID"
  value       = google_cloud_run_service.motel_booking.id
}

output "service_account_email" {
  description = "Service account email"
  value       = google_service_account.cloud_run_sa.email
}

output "vpc_connector_id" {
  description = "VPC Access Connector ID"
  value       = google_vpc_access_connector.connector.id
}

output "cluster_endpoint" {
  description = "Cluster endpoint (placeholder for GKE)"
  value       = ""
}

output "cluster_ca_certificate" {
  description = "Cluster CA certificate (placeholder for GKE)"
  value       = ""
}
