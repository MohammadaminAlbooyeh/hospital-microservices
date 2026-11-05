output "endpoint" {
  value = aws_db_instance.this.endpoint
}

output "address" {
  value = aws_db_instance.this.address
}

output "db_instance_id" {
  value = aws_db_instance.this.id
}

output "db_name" {
  value = aws_db_instance.this.db_name
}
