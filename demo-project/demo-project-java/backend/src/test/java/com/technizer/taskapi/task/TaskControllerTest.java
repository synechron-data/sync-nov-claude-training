package com.technizer.taskapi.task;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskControllerTest {

    private final TaskService taskService = mock(TaskService.class);
    private final TaskController controller = new TaskController(taskService);

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
