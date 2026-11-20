package com.technizer.taskapi.task;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskControllerTest {

    private final TaskService taskService = mock(TaskService.class);
    private final TaskController controller = new TaskController(taskService);

    @Test
    void listCompletedDelegatesToServiceWithOwnerId() {
        Task done = new Task("Done", "Finished", 1L);
        done.setCompleted(true);
        when(taskService.listCompletedForOwner(1L)).thenReturn(List.of(done));

        List<Task> result = controller.listCompleted(1L);

        assertThat(result).containsExactly(done);
        verify(taskService).listCompletedForOwner(1L);
        verify(taskService, never()).listForOwner(any());
    }

    @Test
    void listCompletedReturnsEmptyListWhenServiceReturnsNone() {
        when(taskService.listCompletedForOwner(1L)).thenReturn(List.of());

        assertThat(controller.listCompleted(1L)).isEmpty();
    }

    @Test
    void listCompletedPassesNullOwnerThroughToService() {
        // No null guard in the controller: an unresolved principal (null) goes straight to the service.
        when(taskService.listCompletedForOwner(null)).thenReturn(List.of());

        assertThat(controller.listCompleted(null)).isEmpty();
        verify(taskService).listCompletedForOwner(null);
    }

    @Test
    void listCompletedIsMappedToGetCompleted() throws NoSuchMethodException {
        GetMapping mapping = TaskController.class
                .getMethod("listCompleted", Long.class)
                .getAnnotation(GetMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/completed");
    }

    @Test
    void updateDelegatesToServiceWithOwnerIdAndRequestFields() {
        Task updated = new Task("New title", "New description", 1L);
        when(taskService.update(1L, 5L, "New title", "New description", null)).thenReturn(updated);

        Task result = controller.update(1L, 5L,
                new TaskController.UpdateTaskRequest("New title", "New description", null));

        assertThat(result).isSameAs(updated);
        verify(taskService).update(1L, 5L, "New title", "New description", null);
    }

    @Test
    void createPassesDueDateThroughToService() {
        LocalDate due = LocalDate.of(2026, 12, 1);
        Task created = new Task("Title", "Desc", 1L);
        created.setDueDate(due);
        when(taskService.create(1L, "Title", "Desc", due)).thenReturn(created);

        Task result = controller.create(1L, new TaskController.CreateTaskRequest("Title", "Desc", due));

        assertThat(result).isSameAs(created);
        assertThat(result.getDueDate()).isEqualTo(due);
        verify(taskService).create(1L, "Title", "Desc", due);
    }

    @Test
    void createPassesNullDueDateThroughToService() {
        Task created = new Task("Title", "Desc", 1L);
        when(taskService.create(1L, "Title", "Desc", null)).thenReturn(created);

        Task result = controller.create(1L, new TaskController.CreateTaskRequest("Title", "Desc", null));

        assertThat(result.getDueDate()).isNull();
        verify(taskService).create(1L, "Title", "Desc", null);
    }

    @Test
    void updatePassesDueDateThroughToService() {
        LocalDate due = LocalDate.of(2027, 1, 15);
        Task updated = new Task("Title", "Desc", 1L);
        updated.setDueDate(due);
        when(taskService.update(1L, 5L, "Title", "Desc", due)).thenReturn(updated);

        Task result = controller.update(1L, 5L, new TaskController.UpdateTaskRequest("Title", "Desc", due));

        assertThat(result.getDueDate()).isEqualTo(due);
        verify(taskService).update(1L, 5L, "Title", "Desc", due);
    }

    @Test
    void updatePassesNullDueDateThroughSoTheServiceCanClearIt() {
        Task updated = new Task("Title", "Desc", 1L);
        when(taskService.update(1L, 5L, "Title", "Desc", null)).thenReturn(updated);

        controller.update(1L, 5L, new TaskController.UpdateTaskRequest("Title", "Desc", null));

        verify(taskService).update(1L, 5L, "Title", "Desc", null);
    }

    @Test
    void deleteDelegatesToServiceWithOwnerIdAndTaskId() {
        controller.delete(1L, 10L);

        verify(taskService).delete(1L, 10L);
    }

    @Test
    void deletePropagatesNotFoundFromServiceSoTheExceptionHandlerCanMapItTo404() {
        doThrow(new NoSuchElementException("Task not found: 10")).when(taskService).delete(2L, 10L);

        assertThatThrownBy(() -> controller.delete(2L, 10L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Task not found: 10");
    }

    @Test
    void deletePassesNullOwnerThroughToService() {
        // No null guard in the controller: an unresolved principal (null) goes straight to the service.
        controller.delete(null, 10L);

        verify(taskService).delete(null, 10L);
    }

    @Test
    void deleteIsMappedToDeleteIdPath() throws NoSuchMethodException {
        DeleteMapping mapping = TaskController.class
                .getMethod("delete", Long.class, Long.class)
                .getAnnotation(DeleteMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/{id}");
    }
}
