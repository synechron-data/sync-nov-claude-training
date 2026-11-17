package com.technizer.taskapi.task;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    private final TaskRepository taskRepository;

    @Autowired
    TaskRepositoryTest(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
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
}
