from fastapi import APIRouter, Response

from app.dependencies import CurrentUserId, TaskServiceDep
from app.tasks.schemas import CreateTaskRequest, TaskResponse, UpdateTaskRequest

router = APIRouter(prefix="/api/tasks", tags=["tasks"])


@router.get("", response_model=list[TaskResponse])
def list_tasks(owner_id: CurrentUserId, task_service: TaskServiceDep):
    return task_service.list_for_owner(owner_id)


@router.post("", response_model=TaskResponse)
def create_task(
    request: CreateTaskRequest, owner_id: CurrentUserId, task_service: TaskServiceDep
):
    return task_service.create(owner_id, request.title, request.description)


@router.put("/{task_id}", response_model=TaskResponse)
def update_task(
    task_id: int,
    request: UpdateTaskRequest,
    owner_id: CurrentUserId,
    task_service: TaskServiceDep,
):
    return task_service.update(owner_id, task_id, request.title, request.description)


@router.patch("/{task_id}/complete", response_model=TaskResponse)
def complete_task(task_id: int, owner_id: CurrentUserId, task_service: TaskServiceDep):
    return task_service.mark_completed(owner_id, task_id)


@router.delete("/{task_id}")
def delete_task(
    task_id: int, owner_id: CurrentUserId, task_service: TaskServiceDep
) -> Response:
    task_service.delete(owner_id, task_id)
    return Response(status_code=200)
