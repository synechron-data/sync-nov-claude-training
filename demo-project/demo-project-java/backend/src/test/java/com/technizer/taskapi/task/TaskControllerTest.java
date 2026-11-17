package com.technizer.taskapi.task;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        when(taskService.update(1L, 5L, "New title", "New description")).thenReturn(updated);

        Task result = controller.update(1L, 5L,
                new TaskController.UpdateTaskRequest("New title", "New description"));

        assertThat(result).isSameAs(updated);
        verify(taskService).update(1L, 5L, "New title", "New description");
    }
}
