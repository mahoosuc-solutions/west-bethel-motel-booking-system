# Networking Module Outputs

output "network_id" {
  description = "VPC network ID"
  value       = google_compute_network.vpc.id
}

output "network_name" {
  description = "VPC network name"
  value       = google_compute_network.vpc.name
}

output "private_network_id" {
  description = "VPC network self link for private service connection"
  value       = google_compute_network.vpc.self_link
}

output "subnet_id" {
  description = "Subnet ID"
  value       = google_compute_subnetwork.subnet.id
}

output "subnet_name" {
  description = "Subnet name"
  value       = google_compute_subnetwork.subnet.name
}

output "load_balancer_ip" {
  description = "Load balancer external IP"
  value       = google_compute_global_address.lb_ip.address
}

output "nat_ip" {
  description = "NAT external IP"
  value       = google_compute_router_nat.nat.id
}

output "security_policy_id" {
  description = "Cloud Armor security policy ID"
  value       = google_compute_security_policy.policy.id
}
