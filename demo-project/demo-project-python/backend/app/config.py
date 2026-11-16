"""Application settings - the equivalent of application.properties / appsettings.json."""

# SQLite in-memory database - no external DB needed for the training session.
# Equivalent of the H2 in-memory DB used by the Java backend.
DATABASE_URL = "sqlite://"

HOST = "localhost"
PORT = 8080

CORS_ALLOWED_ORIGINS = ["http://localhost:4200"]
CORS_ALLOWED_METHODS = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]

# NOTE: this is a demo-only secret. Never commit real secrets like this.
# Participants will see this flagged during the /security-review demo too.
JWT_SECRET = "training-demo-secret-key-not-for-production-use-only"
JWT_EXPIRY_MINUTES = 60
