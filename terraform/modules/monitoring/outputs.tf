# Monitoring Module Outputs

output "dashboard_url" {
  description = "Monitoring dashboard URL"
  value       = "https://console.cloud.google.com/monitoring/dashboards/custom/${google_monitoring_dashboard.dashboard.id}?project=${var.project_id}"
}

output "uptime_check_id" {
  description = "Uptime check ID"
  value       = google_monitoring_uptime_check_config.https_check.id
}

output "alert_policy_ids" {
  description = "List of alert policy IDs"
  value = [
    google_monitoring_alert_policy.high_error_rate.id,
    google_monitoring_alert_policy.high_latency.id,
    google_monitoring_alert_policy.instance_count.id,
    google_monitoring_alert_policy.cpu_utilization.id,
    google_monitoring_alert_policy.memory_utilization.id,
  ]
}
