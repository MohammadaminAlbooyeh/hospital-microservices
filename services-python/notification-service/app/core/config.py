from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    service_name: str = "notification-service"
    database_url: str = "postgresql+asyncpg://hospital:hospital@localhost:5432/hospital_notification"
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic: str = "hospital.events"
    kafka_consumer_enabled: bool = False
    kafka_group_id: str = "notification-service"
    jwt_secret: str = "hospital-development-secret-key-change-me-please-0123456789"
    otlp_endpoint: str | None = None
    log_level: str = "INFO"

    redis_url: str = "redis://localhost:6379/0"
    celery_task_always_eager: bool = False
    celery_broker_connection_timeout: float = 2.0

    smtp_host: str = "localhost"
    smtp_port: int = 1025
    smtp_from: str = "no-reply@hospital.local"
    smtp_username: str | None = None
    smtp_password: str | None = None
    smtp_use_tls: bool = False
    smtp_timeout: float = 10.0

    twilio_account_sid: str | None = None
    twilio_auth_token: str | None = None
    twilio_from_number: str | None = None
    twilio_timeout: float = 10.0


settings = Settings()
