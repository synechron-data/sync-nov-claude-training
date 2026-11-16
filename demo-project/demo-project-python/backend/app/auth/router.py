from fastapi import APIRouter

from app.auth.schemas import LoginRequest, RegisterRequest, TokenResponse
from app.dependencies import AuthServiceDep

router = APIRouter(prefix="/api/auth", tags=["auth"])


@router.post("/register")
def register(request: RegisterRequest, auth_service: AuthServiceDep) -> TokenResponse:
    token = auth_service.register(request.email, request.password, request.display_name)
    return TokenResponse(token=token)


@router.post("/login")
def login(request: LoginRequest, auth_service: AuthServiceDep) -> TokenResponse:
    token = auth_service.login(request.email, request.password)
    return TokenResponse(token=token)
