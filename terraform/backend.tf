# Terraform Backend Configuration
# Store state in GCS bucket for collaboration and state locking

terraform {
  backend "gcs" {
    bucket  = "west-bethel-motel-terraform-state"
    prefix  = "terraform/state"

    # Enable state locking with Cloud Storage
    # Prevents concurrent modifications
  }
}

# Alternative: AWS S3 Backend
# Uncomment and configure if using AWS instead of GCP
#
# terraform {
#   backend "s3" {
#     bucket         = "west-bethel-motel-terraform-state"
#     key            = "terraform/state/terraform.tfstate"
#     region         = "us-east-1"
#     encrypt        = true
#     dynamodb_table = "terraform-state-lock"
#   }
# }

# Note: Backend configuration cannot use variables
# Create the backend storage before running terraform init:
#
# For GCP:
#   gsutil mb -p PROJECT_ID -l us-east1 gs://west-bethel-motel-terraform-state
#   gsutil versioning set on gs://west-bethel-motel-terraform-state
#
# For AWS:
#   aws s3api create-bucket --bucket west-bethel-motel-terraform-state --region us-east-1
#   aws s3api put-bucket-versioning --bucket west-bethel-motel-terraform-state --versioning-configuration Status=Enabled
#   aws dynamodb create-table --table-name terraform-state-lock \
#     --attribute-definitions AttributeName=LockID,AttributeType=S \
#     --key-schema AttributeName=LockID,KeyType=HASH \
#     --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5
