variable "identifier" {
  description = "RDS instance identifier."
  type        = string
  default     = "hospital-platform-db"
}

variable "engine" {
  description = "Database engine (postgres for the Spring/FastAPI services' primary store)."
  type        = string
  default     = "postgres"
}

variable "engine_version" {
  type    = string
  default = "16"
}

variable "instance_class" {
  type    = string
  default = "db.t3.medium"
}

variable "allocated_storage" {
  description = "Allocated storage in GiB."
  type        = number
  default     = 50
}

variable "max_allocated_storage" {
  description = "Upper bound for storage autoscaling in GiB."
  type        = number
  default     = 200
}

variable "db_name" {
  type    = string
  default = "hospital"
}

variable "username" {
  type    = string
  default = "hospital"
}

variable "password" {
  description = "Master password. Pass via a secrets manager / tfvars that are never committed."
  type        = string
  sensitive   = true
}

variable "subnet_ids" {
  description = "Private subnet IDs for the DB subnet group."
  type        = list(string)
}

variable "vpc_security_group_ids" {
  type = list(string)
}

variable "multi_az" {
  type    = bool
  default = true
}

variable "backup_retention_period" {
  description = "Days to retain automated backups."
  type        = number
  default     = 7
}

variable "deletion_protection" {
  type    = bool
  default = true
}

variable "tags" {
  type    = map(string)
  default = {}
}
