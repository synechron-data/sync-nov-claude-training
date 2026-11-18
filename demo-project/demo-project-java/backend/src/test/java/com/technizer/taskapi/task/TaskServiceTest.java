package com.technizer.taskapi.task;

// TRAINING NOTE: This file is intentionally left with only a placeholder.
// In Module 6 (Skills) and Module 9-10 (Spec-Driven Development / Spec Kit),
// participants will use Claude Code to generate real test coverage for
// TaskService, including the missing ownership check on markCompleted()
// and delete() surfaced during the Module 13 security-review demo.

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
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
    void listCompletedForOwnerReturnsOnlyCompletedTasksFromRepository() {
        Task done = new Task("Done", "Finished", 1L);
        done.setCompleted(true);
        when(taskRepository.findByOwnerIdAndCompletedTrue(1L)).thenReturn(List.of(done));

        List<Task> result = taskService.listCompletedForOwner(1L);

        assertThat(result).containsExactly(done);
        assertThat(result).allMatch(Task::isCompleted);
        verify(taskRepository).findByOwnerIdAndCompletedTrue(1L);
    }

    @Test
    void listCompletedForOwnerReturnsEmptyListWhenNoneCompleted() {
        when(taskRepository.findByOwnerIdAndCompletedTrue(1L)).thenReturn(List.of());

        assertThat(taskService.listCompletedForOwner(1L)).isEmpty();
    }

    @Test
    void listCompletedForOwnerReturnsAllCompletedTasksInRepositoryOrder() {
        Task first = new Task("First", null, 1L);
        first.setCompleted(true);
        Task second = new Task("Second", "Desc", 1L);
        second.setCompleted(true);
        when(taskRepository.findByOwnerIdAndCompletedTrue(1L)).thenReturn(List.of(first, second));

        assertThat(taskService.listCompletedForOwner(1L)).containsExactly(first, second);
    }

    @Test
    void listCompletedForOwnerQueriesOnlyTheGivenOwner() {
        when(taskRepository.findByOwnerIdAndCompletedTrue(2L)).thenReturn(List.of());

        taskService.listCompletedForOwner(2L);

        verify(taskRepository).findByOwnerIdAndCompletedTrue(2L);
        verify(taskRepository, never()).findByOwnerIdAndCompletedTrue(1L);
        verify(taskRepository, never()).findByOwnerId(any());
    }

    @Test
    void listCompletedForOwnerWithNullOwnerReturnsWhatRepositoryReturns() {
        // No null guard in the service: null is passed straight to the repository.
        when(taskRepository.findByOwnerIdAndCompletedTrue(null)).thenReturn(List.of());

        assertThat(taskService.listCompletedForOwner(null)).isEmpty();
        verify(taskRepository).findByOwnerIdAndCompletedTrue(null);
    }

    @Test
    void updateChangesTitleAndDescriptionForOwner() {
        Task task = new Task("Old title", "Old description", 1L);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task result = taskService.update(1L, 10L, "New title", "New description", null);

        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.getDescription()).isEqualTo("New description");
        verify(taskRepository).save(task);
    }

    @Test
    void updateAllowsClearingDescription() {
        Task task = new Task("Old title", "Old description", 1L);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task result = taskService.update(1L, 10L, "New title", null, null);

        assertThat(result.getDescription()).isNull();
    }

    @Test
    void updateDoesNotChangeCompletedOrOwner() {
        Task task = new Task("Old title", "Old description", 1L);
        task.setCompleted(true);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task result = taskService.update(1L, 10L, "New title", "New description", null);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getOwnerId()).isEqualTo(1L);
    }

    @Test
    void updateThrowsNotFoundWhenTaskMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(1L, 99L, "Title", "Desc", null))
                .isInstanceOf(NoSuchElementException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateThrowsNotFoundWhenTaskBelongsToAnotherUser() {
        Task task = new Task("Old title", "Old description", 2L);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.update(1L, 10L, "New title", "New description", null))
                .isInstanceOf(NoSuchElementException.class);
        assertThat(task.getTitle()).isEqualTo("Old title");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateRejectsBlankOrNullTitle() {
        assertThatThrownBy(() -> taskService.update(1L, 10L, "   ", "Desc", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> taskService.update(1L, 10L, null, "Desc", null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createStoresDueDateOnSavedTask() {
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDate due = LocalDate.of(2026, 12, 1);

        Task result = taskService.create(1L, "Title", "Desc", due);

        assertThat(result.getDueDate()).isEqualTo(due);
        assertThat(result.getTitle()).isEqualTo("Title");
        assertThat(result.getOwnerId()).isEqualTo(1L);
    }

    @Test
    void createWithNullDueDateLeavesDueDateEmpty() {
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.create(1L, "Title", "Desc", null);

        assertThat(result.getDueDate()).isNull();
    }

    @Test
    void createAcceptsPastDueDate() {
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDate past = LocalDate.of(2000, 1, 1);

        Task result = taskService.create(1L, "Backdated", null, past);

        assertThat(result.getDueDate()).isEqualTo(past);
    }

    @Test
    void createRejectsBlankOrNullTitle() {
        assertThatThrownBy(() -> taskService.create(1L, "   ", "Desc", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> taskService.create(1L, null, "Desc", null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createRejectsEmptyTitle() {
        assertThatThrownBy(() -> taskService.create(1L, "", "Desc", null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createRejectionMessageMatchesWhatTheApiReturns() {
        // GlobalExceptionHandler turns this message into the 400 {"error": "..."} body.
        assertThatThrownBy(() -> taskService.create(1L, " ", "Desc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Title must not be blank");
    }

    @Test
    void createRejectsBlankTitleEvenWhenDueDateIsValid() {
        assertThatThrownBy(() -> taskService.create(1L, "\t\n", "Desc", LocalDate.of(2026, 12, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createKeepsTitleWithSurroundingWhitespaceUnchanged() {
        // Only blank titles are rejected; a non-blank title is stored as given (no trimming).
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.create(1L, "  Padded  ", null, null);

        assertThat(result.getTitle()).isEqualTo("  Padded  ");
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void updateSetsDueDateWhenNoneWasSet() {
        Task task = new Task("Title", "Desc", 1L);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);
        LocalDate due = LocalDate.of(2026, 12, 1);

        Task result = taskService.update(1L, 10L, "Title", "Desc", due);

        assertThat(result.getDueDate()).isEqualTo(due);
    }

    @Test
    void updateChangesExistingDueDate() {
        Task task = new Task("Title", "Desc", 1L);
        task.setDueDate(LocalDate.of(2026, 12, 1));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task result = taskService.update(1L, 10L, "Title", "Desc", LocalDate.of(2027, 1, 15));

        assertThat(result.getDueDate()).isEqualTo(LocalDate.of(2027, 1, 15));
    }

    @Test
    void updateWithNullDueDateClearsExistingDueDate() {
        Task task = new Task("Title", "Desc", 1L);
        task.setDueDate(LocalDate.of(2026, 12, 1));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        Task result = taskService.update(1L, 10L, "Title", "Desc", null);

        assertThat(result.getDueDate()).isNull();
    }

    @Test
    void updateByAnotherOwnerDoesNotChangeDueDate() {
        LocalDate original = LocalDate.of(2026, 12, 1);
        Task task = new Task("Title", "Desc", 2L);
        task.setDueDate(original);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.update(1L, 10L, "Title", "Desc", LocalDate.of(2030, 1, 1)))
                .isInstanceOf(NoSuchElementException.class);
        assertThat(task.getDueDate()).isEqualTo(original);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateWithBlankTitleDoesNotChangeDueDate() {
        assertThatThrownBy(() -> taskService.update(1L, 10L, " ", "Desc", LocalDate.of(2026, 12, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(taskRepository, never()).findById(any());
        verify(taskRepository, never()).save(any());
    }
}
