from datetime import datetime, timezone

from pydantic import ConfigDict, field_serializer

from app.common import CamelModel, NotBlankStr


class CreateTaskRequest(CamelModel):
    title: NotBlankStr
    description: str | None = None


class UpdateTaskRequest(CamelModel):
    title: NotBlankStr
    description: str | None = None


class TaskResponse(CamelModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    description: str | None
    completed: bool
    owner_id: int
    created_at: datetime

    @field_serializer("created_at")
    def _serialize_created_at(self, value: datetime) -> str:
        # SQLite drops the timezone, so stored values come back naive (they are UTC).
        # Emit "2025-01-01T12:00:00.123456Z", like a Java Instant.
        if value.tzinfo is None:
            value = value.replace(tzinfo=timezone.utc)
        return value.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")
