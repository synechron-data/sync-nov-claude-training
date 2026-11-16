from app.auth.jwt_util import JwtUtil
from app.auth.password_encoder import BCryptPasswordEncoder
from app.users.models import User
from app.users.repository import UserRepository


class AuthService:
    def __init__(
        self,
        user_repository: UserRepository,
        password_encoder: BCryptPasswordEncoder,
        jwt_util: JwtUtil,
    ):
        self._user_repository = user_repository
        self._password_encoder = password_encoder
        self._jwt_util = jwt_util

    def register(self, email: str, password: str, display_name: str) -> str:
        if self._user_repository.exists_by_email(email):
            raise ValueError(f"Email already registered: {email}")
        user = User(
            email=email,
            password_hash=self._password_encoder.encode(password),
            display_name=display_name,
        )
        saved = self._user_repository.save(user)
        return self._jwt_util.generate_token(saved.id, saved.email)

    def login(self, email: str, password: str) -> str:
        user = self._user_repository.find_by_email(email)
        if user is None:
            raise ValueError("Invalid credentials")

        if not self._password_encoder.matches(password, user.password_hash):
            raise ValueError("Invalid credentials")
        return self._jwt_util.generate_token(user.id, user.email)
