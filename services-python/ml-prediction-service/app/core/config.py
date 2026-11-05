from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    service_name: str = "ml-prediction-service"
    database_url: str = "postgresql+asyncpg://hospital:hospital@localhost:5432/hospital_ml"
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic: str = "hospital.events"
    kafka_consumer_enabled: bool = False
    kafka_group_id: str = "ml-prediction-service"
    jwt_secret: str = "hospital-development-secret-key-change-me-please-0123456789"
    otlp_endpoint: str | None = None
    log_level: str = "INFO"
    model_dir: str = "app/models"
    auto_train_on_startup: bool = True
    no_show_model_file: str = "no_show_model.joblib"
    length_of_stay_model_file: str = "length_of_stay_model.joblib"


settings = Settings()
