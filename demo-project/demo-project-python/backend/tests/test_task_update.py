import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.database import Base
from app.dependencies import get_current_user_id, get_task_service
from app.main import create_app
from app.tasks import models as _task_models  # noqa: F401  (register tables)
from app.tasks.repository import TaskRepository
from app.tasks.service import TaskService
from app.users import models as _user_models  # noqa: F401  (register tables)


@pytest.fixture
def db():
    engine = create_engine(
        "sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool
    )
    Base.metadata.create_all(engine)
    with sessionmaker(bind=engine, expire_on_commit=False)() as session:
        yield session


@pytest.fixture
def service(db):
    return TaskService(TaskRepository(db))


def test_update_changes_title_and_description(service):
    task = service.create(1, "old", "old desc")

    updated = service.update(1, task.id, "new", "new desc")

    assert updated.id == task.id
    assert updated.title == "new"
    assert updated.description == "new desc"
    assert updated.completed is False


def test_update_with_null_description_clears_it(service):
    task = service.create(1, "title", "desc")

    assert service.update(1, task.id, "title", None).description is None


@pytest.mark.parametrize("title", ["", "   "])
def test_update_rejects_blank_title(service, title):
    task = service.create(1, "title", None)

    with pytest.raises(ValueError):
        service.update(1, task.id, title, None)


def test_update_missing_task_raises_lookup_error(service):
    with pytest.raises(LookupError):
        service.update(1, 999, "title", None)


def test_update_other_users_task_is_not_found_and_unchanged(service):
    task = service.create(1, "mine", "desc")

    with pytest.raises(LookupError):
        service.update(2, task.id, "hijacked", None)

    assert service.list_for_owner(1)[0].title == "mine"


@pytest.fixture
def client(service):
    app = create_app()
    app.dependency_overrides[get_task_service] = lambda: service
    app.dependency_overrides[get_current_user_id] = lambda: 1
    return TestClient(app)


def test_put_updates_task(client, service):
    task = service.create(1, "old", None)

    response = client.put(
        f"/api/tasks/{task.id}", json={"title": "new", "description": "d"}
    )

    assert response.status_code == 200
    body = response.json()
    assert body["title"] == "new"
    assert body["description"] == "d"
    assert body["ownerId"] == 1


def test_put_blank_title_is_400(client, service):
    task = service.create(1, "old", None)

    response = client.put(f"/api/tasks/{task.id}", json={"title": " "})

    assert response.status_code == 400
    assert "error" in response.json()


def test_put_unknown_task_is_404(client):
    response = client.put("/api/tasks/999", json={"title": "x"})

    assert response.status_code == 404
    assert "error" in response.json()


def test_put_requires_authentication():
    response = TestClient(create_app()).put("/api/tasks/1", json={"title": "x"})

    assert response.status_code == 401
