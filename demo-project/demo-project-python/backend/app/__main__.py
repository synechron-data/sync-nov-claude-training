"""Lets the API be started with `python -m app` (http://localhost:8080)."""

import uvicorn

from app import config

if __name__ == "__main__":
    uvicorn.run("app.main:app", host=config.HOST, port=config.PORT)
