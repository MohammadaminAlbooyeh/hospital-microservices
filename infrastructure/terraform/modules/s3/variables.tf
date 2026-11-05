variable "bucket_name" {
  description = "Globally-unique S3 bucket name, e.g. for document-service / audit-service artifacts."
  type        = string
}

variable "versioning_enabled" {
  type    = bool
  default = true
}

variable "force_destroy" {
  description = "Allow bucket deletion even if it still contains objects (dev/test only)."
  type        = bool
  default     = false
}

variable "lifecycle_expiration_days" {
  description = "Days after which noncurrent object versions are expired. 0 disables the rule."
  type        = number
  default     = 90
}

variable "tags" {
  type    = map(string)
  default = {}
}
