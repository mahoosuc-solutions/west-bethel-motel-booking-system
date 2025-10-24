# Redis Module - Cloud Memorystore
# Managed Redis for caching and session management

resource "google_redis_instance" "cache" {
  name               = "motel-booking-${var.environment}"
  tier               = var.redis_tier
  memory_size_gb     = var.redis_size
  region             = var.region
  redis_version      = "REDIS_7_0"
  display_name       = "Motel Booking Cache ${var.environment}"
  authorized_network = var.network_id
  connect_mode       = "PRIVATE_SERVICE_ACCESS"

  redis_configs = {
    maxmemory-policy = "allkeys-lru"
    timeout          = "300"
  }

  maintenance_policy {
    weekly_maintenance_window {
      day = "SUNDAY"
      start_time {
        hours   = 2
        minutes = 0
        seconds = 0
        nanos   = 0
      }
    }
  }

  persistence_config {
    persistence_mode    = var.redis_tier == "STANDARD_HA" ? "RDB" : "DISABLED"
    rdb_snapshot_period = var.redis_tier == "STANDARD_HA" ? "ONE_HOUR" : null
  }

  labels = {
    environment = var.environment
    managed_by  = "terraform"
    service     = "motel-booking"
  }

  depends_on = [google_project_service.redis_api]
}

# Enable Redis API
resource "google_project_service" "redis_api" {
  service            = "redis.googleapis.com"
  disable_on_destroy = false
}
