from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    service_name: str = "audit-service"
    database_url: str = "postgresql+asyncpg://hospital:hospital@localhost:5432/hospital_audit"
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic: str = "hospital.events"
    kafka_consumer_enabled: bool = False
    otlp_endpoint: str | None = None
    log_level: str = "INFO"


settings = Settings()
