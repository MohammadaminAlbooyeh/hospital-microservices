output "bootstrap_brokers_tls" {
  description = "TLS bootstrap broker connection string, for KAFKA_BOOTSTRAP_SERVERS."
  value       = aws_msk_cluster.this.bootstrap_brokers_tls
}

output "zookeeper_connect_string" {
  value = aws_msk_cluster.this.zookeeper_connect_string
}

output "arn" {
  value = aws_msk_cluster.this.arn
}
