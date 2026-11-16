import bcrypt


class BCryptPasswordEncoder:
    """Equivalent of Spring Security's BCryptPasswordEncoder."""

    def encode(self, raw_password: str) -> str:
        return bcrypt.hashpw(raw_password.encode(), bcrypt.gensalt()).decode()

    def matches(self, raw_password: str, encoded_password: str) -> bool:
        return bcrypt.checkpw(raw_password.encode(), encoded_password.encode())
