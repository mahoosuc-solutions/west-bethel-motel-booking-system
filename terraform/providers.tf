# Provider Configuration

provider "google" {
  project = var.project_id
  region  = var.region
}

provider "google-beta" {
  project = var.project_id
  region  = var.region
}

# Kubernetes provider for GKE cluster management
provider "kubernetes" {
  host                   = "https://${module.compute.cluster_endpoint}"
  token                  = data.google_client_config.default.access_token
  cluster_ca_certificate = base64decode(module.compute.cluster_ca_certificate)
}

# Data source for getting access token
data "google_client_config" "default" {}

# Alternative: AWS Provider Configuration
# Uncomment if using AWS instead of GCP
#
# provider "aws" {
#   region = var.region
#
#   default_tags {
#     tags = {
#       Environment = var.environment
#       Project     = var.project_name
#       ManagedBy   = "Terraform"
#     }
#   }
# }
#
# provider "kubernetes" {
#   host                   = module.compute.cluster_endpoint
#   cluster_ca_certificate = base64decode(module.compute.cluster_ca_certificate)
#   token                  = data.aws_eks_cluster_auth.cluster.token
# }
#
# data "aws_eks_cluster_auth" "cluster" {
#   name = module.compute.cluster_name
# }
