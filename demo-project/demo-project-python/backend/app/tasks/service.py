from collections.abc import Sequence

from app.tasks.models import Task
from app.tasks.repository import TaskRepository


class TaskService:
    def __init__(self, task_repository: TaskRepository):
        self._task_repository = task_repository

    def list_for_owner(self, owner_id: int) -> Sequence[Task]:
        return self._task_repository.find_by_owner_id(owner_id)

    def create(self, owner_id: int, title: str, description: str | None) -> Task:
        task = Task(title=title, description=description, owner_id=owner_id)
        return self._task_repository.save(task)

    def mark_completed(self, owner_id: int, task_id: int) -> Task:
        task = self._task_repository.find_by_id(task_id)
        if task is None:
            raise LookupError(f"Task not found: {task_id}")

        # NOTE: no ownership check here yet - flagged as a TODO for the
        # security-review demo (a user can complete another user's task).
        task.completed = True
        return self._task_repository.save(task)

    def delete(self, owner_id: int, task_id: int) -> None:
        self._task_repository.delete_by_id(task_id)
