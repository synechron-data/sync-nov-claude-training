from collections.abc import Iterator

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker
from sqlalchemy.pool import StaticPool

from app import config

# An in-memory SQLite database lives only as long as its connection, so every
# session shares a single connection (StaticPool) for the lifetime of the app.
engine = create_engine(
    config.DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)

SessionLocal = sessionmaker(bind=engine, expire_on_commit=False)


class Base(DeclarativeBase):
    pass


def get_db() -> Iterator[Session]:
    """One session per request, closed when the request finishes."""
    with SessionLocal() as session:
        yield session


def create_tables() -> None:
    # Import the models so they are registered on Base.metadata.
    from app.tasks import models as _task_models  # noqa: F401
    from app.users import models as _user_models  # noqa: F401

    Base.metadata.create_all(engine)
