"""Equivalent of the Java GlobalExceptionHandler: every error is returned as {"error": "..."}."""

import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

logger = logging.getLogger("app")


def _error(
    status_code: int, message: str, headers: dict[str, str] | None = None
) -> JSONResponse:
    return JSONResponse(
        status_code=status_code, content={"error": message}, headers=headers
    )


async def _bad_request(request: Request, exc: ValueError) -> JSONResponse:
    return _error(400, str(exc))


async def _not_found(request: Request, exc: LookupError) -> JSONResponse:
    return _error(404, str(exc))


async def _validation_error(
    request: Request, exc: RequestValidationError
) -> JSONResponse:
    messages = []
    for err in exc.errors():
        field = ".".join(str(part) for part in err["loc"] if part != "body")
        message = err["msg"].removeprefix("Value error, ")
        messages.append(f"{field}: {message}" if field else message)
    return _error(400, "; ".join(messages))


async def _http_error(request: Request, exc: StarletteHTTPException) -> JSONResponse:
    return _error(exc.status_code, str(exc.detail), exc.headers)


async def _catch_unexpected(request: Request, call_next):
    try:
        return await call_next(request)
    except Exception:
        logger.exception("Unhandled error on %s %s", request.method, request.url.path)
        # Deliberately generic response to callers; do not leak internal details.
        return _error(500, "Unexpected server error")


def register_exception_handlers(app: FastAPI) -> None:
    app.add_exception_handler(ValueError, _bad_request)
    app.add_exception_handler(LookupError, _not_found)
    app.add_exception_handler(RequestValidationError, _validation_error)
    app.add_exception_handler(StarletteHTTPException, _http_error)
    # A middleware rather than an Exception handler, so that it runs inside the CORS
    # middleware and 500 responses still carry CORS headers. Register it before CORS.
    app.middleware("http")(_catch_unexpected)
