from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    service_name: str = "triage-service"
    database_url: str = "postgresql+asyncpg://hospital:hospital@localhost:5432/hospital_triage"
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic: str = "hospital.events"
    kafka_consumer_enabled: bool = False
    kafka_group_id: str = "triage-service"
    jwt_secret: str = "hospital-development-secret-key-change-me-please-0123456789"
    model_path: str = "app/ml/model.pkl"
    otlp_endpoint: str | None = None
    log_level: str = "INFO"


settings = Settings()
