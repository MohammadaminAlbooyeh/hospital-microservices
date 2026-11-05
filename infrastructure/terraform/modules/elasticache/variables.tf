variable "cluster_id" {
  type    = string
  default = "hospital-platform-redis"
}

variable "engine_version" {
  type    = string
  default = "7.1"
}

variable "node_type" {
  type    = string
  default = "cache.t3.small"
}

variable "num_cache_nodes" {
  description = "Number of nodes. Use 1 for a single-node dev cache; use a replication group instead for prod HA."
  type        = number
  default     = 1
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_ids" {
  type = list(string)
}

variable "tags" {
  type    = map(string)
  default = {}
}
