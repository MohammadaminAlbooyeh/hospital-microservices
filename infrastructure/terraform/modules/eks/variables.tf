variable "cluster_name" {
  description = "Name of the EKS cluster."
  type        = string
  default     = "hospital-platform"
}

variable "kubernetes_version" {
  description = "Kubernetes version for the control plane."
  type        = string
  default     = "1.29"
}

variable "subnet_ids" {
  description = "Subnet IDs (private + public) for the cluster control plane ENIs and node groups."
  type        = list(string)
}

variable "cluster_role_arn" {
  description = "IAM role ARN assumed by the EKS control plane."
  type        = string
}

variable "node_role_arn" {
  description = "IAM role ARN assumed by the managed node group's EC2 instances."
  type        = string
}

variable "node_instance_types" {
  description = "Instance types for the default managed node group."
  type        = list(string)
  default     = ["t3.large"]
}

variable "node_desired_size" {
  type    = number
  default = 3
}

variable "node_min_size" {
  type    = number
  default = 2
}

variable "node_max_size" {
  type    = number
  default = 6
}

variable "endpoint_public_access" {
  description = "Whether the EKS API server endpoint is reachable from the public internet."
  type        = bool
  default     = false
}

variable "tags" {
  type    = map(string)
  default = {}
}
