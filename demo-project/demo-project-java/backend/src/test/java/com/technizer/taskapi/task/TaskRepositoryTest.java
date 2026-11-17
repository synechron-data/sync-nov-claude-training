package com.technizer.taskapi.task;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    private final TaskRepository taskRepository;
    private final EntityManager entityManager;

    @Autowired
    TaskRepositoryTest(TaskRepository taskRepository, EntityManager entityManager) {
        this.taskRepository = taskRepository;
        this.entityManager = entityManager;
    }

    private Task reload(Long id) {
        entityManager.flush();
        entityManager.clear();
        return taskRepository.findById(id).orElseThrow();
    }

    private Task save(String title, Long ownerId, boolean completed) {
        Task task = new Task(title, null, ownerId);
        task.setCompleted(completed);
        return taskRepository.save(task);
    }

    @Test
    void findByOwnerIdAndCompletedTrueReturnsOnlyCompletedTasksOfThatOwner() {
        Task done = save("Done", 1L, true);
        save("Pending", 1L, false);
        save("Someone else's done", 2L, true);

        List<Task> result = taskRepository.findByOwnerIdAndCompletedTrue(1L);

        assertThat(result).containsExactly(done);
    }

    @Test
    void findByOwnerIdAndCompletedTrueReturnsEmptyWhenOwnerHasNoCompletedTasks() {
        save("Pending", 1L, false);

        assertThat(taskRepository.findByOwnerIdAndCompletedTrue(1L)).isEmpty();
    }

    @Test
    void findByOwnerIdAndCompletedTrueReturnsEmptyForUnknownOwner() {
        save("Done", 1L, true);

        assertThat(taskRepository.findByOwnerIdAndCompletedTrue(999L)).isEmpty();
    }

    @Test
    void dueDateIsPersistedAndReloaded() {
        Task task = new Task("With due date", null, 1L);
        task.setDueDate(LocalDate.of(2026, 12, 1));
        Long id = taskRepository.save(task).getId();

        assertThat(reload(id).getDueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void nullDueDateIsPersistedAsNull() {
        Long id = taskRepository.save(new Task("No due date", null, 1L)).getId();

        assertThat(reload(id).getDueDate()).isNull();
    }

    @Test
    void dueDateCanBeClearedAfterBeingSet() {
        Task task = new Task("Cleared", null, 1L);
        task.setDueDate(LocalDate.of(2026, 12, 1));
        Long id = taskRepository.save(task).getId();

        Task loaded = reload(id);
        loaded.setDueDate(null);
        taskRepository.save(loaded);

        assertThat(reload(id).getDueDate()).isNull();
    }
}
