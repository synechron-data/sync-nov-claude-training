from collections.abc import Sequence

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.tasks.models import Task


class TaskRepository:
    def __init__(self, db: Session):
        self._db = db

    def find_by_owner_id(self, owner_id: int) -> Sequence[Task]:
        return self._db.scalars(select(Task).where(Task.owner_id == owner_id)).all()

    def find_by_id(self, task_id: int) -> Task | None:
        return self._db.get(Task, task_id)

    def save(self, task: Task) -> Task:
        self._db.add(task)
        self._db.commit()
        return task

    def delete_by_id(self, task_id: int) -> None:
        # Like Spring Data's deleteById, deleting a missing id is a no-op.
        self._db.execute(delete(Task).where(Task.id == task_id))
        self._db.commit()
