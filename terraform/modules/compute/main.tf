# Compute Module - Cloud Run Deployment
# Deploys containerized application with auto-scaling

resource "google_cloud_run_service" "motel_booking" {
  name     = "motel-booking-${var.environment}"
  location = var.region

  template {
    spec {
      containers {
        image = var.container_image

        resources {
          limits = {
            cpu    = var.cpu_limit
            memory = var.memory_limit
          }
        }

        env {
          name  = "SPRING_PROFILES_ACTIVE"
          value = var.environment
        }

        env {
          name  = "SPRING_DATASOURCE_URL"
          value = "jdbc:postgresql:///${var.database_name}?cloudSqlInstance=${var.database_host}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
        }

        env {
          name = "SPRING_DATASOURCE_USERNAME"
          value_from {
            secret_key_ref {
              name = google_secret_manager_secret.db_username.secret_id
              key  = "latest"
            }
          }
        }

        env {
          name = "SPRING_DATASOURCE_PASSWORD"
          value_from {
            secret_key_ref {
              name = google_secret_manager_secret.db_password.secret_id
              key  = "latest"
            }
          }
        }

        env {
          name  = "SPRING_REDIS_HOST"
          value = var.redis_host
        }

        env {
          name  = "SPRING_REDIS_PORT"
          value = tostring(var.redis_port)
        }

        env {
          name  = "MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE"
          value = "health,info,metrics,prometheus"
        }

        # Liveness probe
        liveness_probe {
          http_get {
            path = "/actuator/health/liveness"
            port = 8080
          }
          initial_delay_seconds = 30
          timeout_seconds       = 5
          period_seconds        = 10
          failure_threshold     = 3
        }

        # Readiness probe
        startup_probe {
          http_get {
            path = "/actuator/health/readiness"
            port = 8080
          }
          initial_delay_seconds = 10
          timeout_seconds       = 5
          period_seconds        = 5
          failure_threshold     = 10
        }
      }

      container_concurrency = 80
      timeout_seconds       = 300

      service_account_name = google_service_account.cloud_run_sa.email
    }

    metadata {
      annotations = {
        "autoscaling.knative.dev/minScale"      = tostring(var.min_instances)
        "autoscaling.knative.dev/maxScale"      = tostring(var.max_instances)
        "run.googleapis.com/cloudsql-instances" = var.database_host
        "run.googleapis.com/vpc-access-connector" = google_vpc_access_connector.connector.id
        "run.googleapis.com/vpc-access-egress"    = "private-ranges-only"
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }

  autogenerate_revision_name = true

  depends_on = [
    google_project_service.run_api,
    google_vpc_access_connector.connector
  ]
}

# VPC Access Connector for Cloud Run
resource "google_vpc_access_connector" "connector" {
  name          = "motel-booking-connector-${var.environment}"
  region        = var.region
  network       = var.network_id
  ip_cidr_range = "10.8.0.0/28"
  min_instances = 2
  max_instances = 10
}

# Service Account for Cloud Run
resource "google_service_account" "cloud_run_sa" {
  account_id   = "motel-booking-${var.environment}"
  display_name = "Service Account for Motel Booking ${var.environment}"
  description  = "Used by Cloud Run service for database and secrets access"
}

# IAM bindings for Cloud Run service account
resource "google_project_iam_member" "cloudsql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.cloud_run_sa.email}"
}

resource "google_project_iam_member" "secret_accessor" {
  project = var.project_id
  role    = "roles/secretmanager.secretAccessor"
  member  = "serviceAccount:${google_service_account.cloud_run_sa.email}"
}

# Secret Manager secrets for database credentials
resource "google_secret_manager_secret" "db_username" {
  secret_id = "db-username-${var.environment}"

  replication {
    automatic = true
  }
}

resource "google_secret_manager_secret_version" "db_username" {
  secret      = google_secret_manager_secret.db_username.id
  secret_data = var.db_username
}

resource "google_secret_manager_secret" "db_password" {
  secret_id = "db-password-${var.environment}"

  replication {
    automatic = true
  }
}

resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = var.db_password
}

# IAM policy for public access (adjust as needed)
data "google_iam_policy" "noauth" {
  binding {
    role = "roles/run.invoker"
    members = [
      "allUsers",
    ]
  }
}

resource "google_cloud_run_service_iam_policy" "noauth" {
  location    = google_cloud_run_service.motel_booking.location
  project     = google_cloud_run_service.motel_booking.project
  service     = google_cloud_run_service.motel_booking.name
  policy_data = data.google_iam_policy.noauth.policy_data
}

# Enable required APIs
resource "google_project_service" "run_api" {
  service            = "run.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "vpcaccess_api" {
  service            = "vpcaccess.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "secretmanager_api" {
  service            = "secretmanager.googleapis.com"
  disable_on_destroy = false
}
