from sqlalchemy import exists, select
from sqlalchemy.orm import Session

from app.users.models import User


class UserRepository:
    def __init__(self, db: Session):
        self._db = db

    def find_by_email(self, email: str) -> User | None:
        return self._db.scalar(select(User).where(User.email == email))

    def exists_by_email(self, email: str) -> bool:
        return bool(self._db.scalar(select(exists().where(User.email == email))))

    def save(self, user: User) -> User:
        self._db.add(user)
        self._db.commit()
        return user
