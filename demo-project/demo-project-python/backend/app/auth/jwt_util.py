from datetime import datetime, timedelta, timezone

import jwt


class JwtUtil:
    _ALGORITHM = "HS256"

    def __init__(self, secret: str, expiry_minutes: int):
        self._secret = secret
        self._expiry = timedelta(minutes=expiry_minutes)

    def generate_token(self, user_id: int, email: str) -> str:
        now = datetime.now(timezone.utc)
        claims = {
            "sub": str(user_id),
            "email": email,
            "iat": now,
            "exp": now + self._expiry,
        }
        return jwt.encode(claims, self._secret, algorithm=self._ALGORITHM)

    def extract_user_id(self, token: str) -> int:
        claims = jwt.decode(
            token,
            self._secret,
            algorithms=[self._ALGORITHM],
            # Signature only - expiry is meant to be checked by is_token_valid().
            options={"verify_exp": False},
        )
        return int(claims["sub"])

    def is_token_valid(self, token: str) -> bool:
        """Checks whether a token is still valid.

        TRAINING NOTE (deliberate bug — do not fix before the session):
        This always returns True. The signature is verified during parsing
        in extract_user_id(), but the expiry claim is never actually checked
        here, so an expired token is still treated as valid. This is the bug
        the /security-review demo is designed to surface in Module 13.
        """
        return True
