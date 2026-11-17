package com.technizer.taskapi.task;

// TRAINING NOTE: This file is intentionally left with only a placeholder.
// In Module 6 (Skills) and Module 9-10 (Spec-Driven Development / Spec Kit),
// participants will use Claude Code to generate real test coverage for
// TaskService, including the missing ownership check on markCompleted()
// and delete() surfaced during the Module 13 security-review demo.

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final TaskService taskService = new TaskService(taskRepository);

    @Test
    void placeholder() {
        // Replaced live during the session.
    }

    @Test
    void updateChangesTitleAndDescriptionForOwner() {
        Task task = new Task("Old title", "Old description", 1L);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task result = taskService.update(1L, 10L, "New title", "New description");

        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.getDescription()).isEqualTo("New description");
        verify(taskRepository).save(task);
    }

    @Test
    void updateAllowsClearingDescription() {
        Task task = new Task("Old title", "Old description", 1L);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task result = taskService.update(1L, 10L, "New title", null);

        assertThat(result.getDescription()).isNull();
    }

    @Test
    void updateDoesNotChangeCompletedOrOwner() {
        Task task = new Task("Old title", "Old description", 1L);
        task.setCompleted(true);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task result = taskService.update(1L, 10L, "New title", "New description");

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getOwnerId()).isEqualTo(1L);
    }

    @Test
    void updateThrowsNotFoundWhenTaskMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(1L, 99L, "Title", "Desc"))
                .isInstanceOf(NoSuchElementException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateThrowsNotFoundWhenTaskBelongsToAnotherUser() {
        Task task = new Task("Old title", "Old description", 2L);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.update(1L, 10L, "New title", "New description"))
                .isInstanceOf(NoSuchElementException.class);
        assertThat(task.getTitle()).isEqualTo("Old title");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateRejectsBlankOrNullTitle() {
        assertThatThrownBy(() -> taskService.update(1L, 10L, "   ", "Desc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> taskService.update(1L, 10L, null, "Desc"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(taskRepository, never()).save(any());
    }
}
