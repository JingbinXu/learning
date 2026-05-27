from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import RedirectResponse, JSONResponse
from starlette.middleware.sessions import SessionMiddleware

from app.config import get_settings
from app.exceptions import BusinessException
from app.routers import health, user, app as app_router, chat_history, workflow


settings = get_settings()


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    print(f"Starting bing-ai-code-backend on port {settings.server_port}")
    yield
    # Shutdown
    print("Shutting down...")


app = FastAPI(
    title="Bing AI Code Backend",
    description="AI-powered frontend code generation platform",
    version="0.1.0",
    lifespan=lifespan,
)


@app.exception_handler(BusinessException)
async def business_exception_handler(request: Request, exc: BusinessException):
    return JSONResponse(
        status_code=200,
        content={"code": exc.code, "data": None, "message": exc.message},
    )

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Session middleware (Redis-backed via itsdangerous signing)
app.add_middleware(
    SessionMiddleware,
    secret_key=settings.session_secret_key,
    max_age=settings.session_max_age,
    same_site="lax",
    https_only=False,
)

# Register routers
prefix = settings.api_prefix
app.include_router(health.router, prefix=prefix)
app.include_router(user.router, prefix=prefix)
app.include_router(app_router.router, prefix=prefix)
app.include_router(chat_history.router, prefix=prefix)
app.include_router(workflow.router, prefix=prefix)


@app.get("/")
async def root():
    return RedirectResponse(url="/docs")


@app.get("/docs-redirect")
async def docs_redirect():
    return {"message": "API docs at /docs"}
