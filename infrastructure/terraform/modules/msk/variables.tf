variable "cluster_name" {
  type    = string
  default = "hospital-platform-kafka"
}

variable "kafka_version" {
  type    = string
  default = "3.6.0"
}

variable "number_of_broker_nodes" {
  description = "Must be a multiple of the number of subnets (AZs) provided."
  type        = number
  default     = 3
}

variable "broker_instance_type" {
  type    = string
  default = "kafka.m5.large"
}

variable "ebs_volume_size" {
  description = "Per-broker EBS volume size in GiB."
  type        = number
  default     = 100
}

variable "subnet_ids" {
  description = "Private subnet IDs for the broker ENIs."
  type        = list(string)
}

variable "security_group_ids" {
  type = list(string)
}

variable "tags" {
  type    = map(string)
  default = {}
}
