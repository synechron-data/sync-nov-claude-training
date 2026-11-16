"""Dependency wiring - the equivalent of Spring's beans / the .NET DI registrations.

Tests can swap any of these with `app.dependency_overrides[...]`.
"""

from typing import Annotated

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app import config
from app.auth.jwt_util import JwtUtil
from app.auth.password_encoder import BCryptPasswordEncoder
from app.auth.service import AuthService
from app.database import get_db
from app.tasks.repository import TaskRepository
from app.tasks.service import TaskService
from app.users.repository import UserRepository

_jwt_util = JwtUtil(config.JWT_SECRET, config.JWT_EXPIRY_MINUTES)
_password_encoder = BCryptPasswordEncoder()


def get_jwt_util() -> JwtUtil:
    return _jwt_util


def get_password_encoder() -> BCryptPasswordEncoder:
    return _password_encoder


def get_auth_service(
    db: Annotated[Session, Depends(get_db)],
    password_encoder: Annotated[BCryptPasswordEncoder, Depends(get_password_encoder)],
    jwt_util: Annotated[JwtUtil, Depends(get_jwt_util)],
) -> AuthService:
    return AuthService(UserRepository(db), password_encoder, jwt_util)


def get_task_service(db: Annotated[Session, Depends(get_db)]) -> TaskService:
    return TaskService(TaskRepository(db))


_bearer = HTTPBearer(auto_error=False)


def _unauthorized(detail: str) -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail=detail,
        headers={"WWW-Authenticate": "Bearer"},
    )


def get_current_user_id(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(_bearer)],
    jwt_util: Annotated[JwtUtil, Depends(get_jwt_util)],
) -> int:
    """Equivalent of the Java JwtAuthFilter + @AuthenticationPrincipal Long ownerId."""
    if credentials is None:
        raise _unauthorized("Authentication required")

    token = credentials.credentials

    if not jwt_util.is_token_valid(token):
        raise _unauthorized("Invalid token")

    try:
        return jwt_util.extract_user_id(token)
    except (jwt.InvalidTokenError, KeyError, ValueError):
        raise _unauthorized("Invalid token")


AuthServiceDep = Annotated[AuthService, Depends(get_auth_service)]
TaskServiceDep = Annotated[TaskService, Depends(get_task_service)]
CurrentUserId = Annotated[int, Depends(get_current_user_id)]
