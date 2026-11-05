from app.core.config import settings
from app.ml.registry import ModelRegistry

registry = ModelRegistry(settings.model_dir, auto_train=settings.auto_train_on_startup)

__all__ = ["ModelRegistry", "registry"]
