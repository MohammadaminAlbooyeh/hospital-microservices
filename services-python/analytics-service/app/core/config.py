from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    service_name: str = "analytics-service"
    database_url: str = "postgresql+asyncpg://hospital:hospital@localhost:5432/hospital_analytics"
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic: str = "hospital.events"
    kafka_consumer_enabled: bool = False
    kafka_group_id: str = "analytics-service"
    jwt_secret: str = "hospital-development-secret-key-change-me-please-0123456789"
    otlp_endpoint: str | None = None
    log_level: str = "INFO"
    bed_capacity: int = Field(default=200, ge=1)
    analytics_seed_enabled: bool = False


settings = Settings()
