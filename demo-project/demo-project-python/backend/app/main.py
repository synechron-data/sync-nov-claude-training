from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app import config
from app.auth.router import router as auth_router
from app.database import create_tables
from app.exceptions import register_exception_handlers
from app.tasks.router import router as tasks_router


def create_app() -> FastAPI:
    create_tables()

    app = FastAPI(
        title="Task API",
        version="0.1.0",
        description="Task Management API - Claude Code training demo backend (Python / FastAPI port)",
    )

    # Order matters: middleware added later wraps middleware added earlier, so the
    # error-catching middleware registered here ends up inside CORS.
    register_exception_handlers(app)
    app.add_middleware(
        CORSMiddleware,
        allow_origins=config.CORS_ALLOWED_ORIGINS,
        allow_methods=config.CORS_ALLOWED_METHODS,
        allow_headers=["*"],
    )

    app.include_router(auth_router)
    app.include_router(tasks_router)
    return app


app = create_app()
