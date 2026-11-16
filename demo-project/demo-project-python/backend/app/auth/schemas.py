from pydantic import EmailStr

from app.common import CamelModel, NotBlankStr


class RegisterRequest(CamelModel):
    email: EmailStr
    password: NotBlankStr
    display_name: NotBlankStr


class LoginRequest(CamelModel):
    email: EmailStr
    password: NotBlankStr


class TokenResponse(CamelModel):
    token: str
