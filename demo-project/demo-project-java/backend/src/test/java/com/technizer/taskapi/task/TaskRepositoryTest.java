package com.technizer.taskapi.task;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void findByIdAndOwnerIdReturnsTheTaskWhenOwnerMatches() {
        Task mine = save("Mine", 1L, false);

        assertThat(taskRepository.findByIdAndOwnerId(mine.getId(), 1L)).contains(mine);
    }

    @Test
    void findByIdAndOwnerIdReturnsEmptyWhenTaskBelongsToAnotherOwner() {
        Task theirs = save("Theirs", 2L, false);

        assertThat(taskRepository.findByIdAndOwnerId(theirs.getId(), 1L)).isEmpty();
    }

    @Test
    void findByIdAndOwnerIdReturnsEmptyForUnknownId() {
        save("Mine", 1L, false);

        assertThat(taskRepository.findByIdAndOwnerId(999_999L, 1L)).isEmpty();
    }

    @Test
    void findByIdAndOwnerIdPicksTheRequestedTaskAmongSeveralOfTheSameOwner() {
        save("First", 1L, false);
        Task second = save("Second", 1L, true);

        assertThat(taskRepository.findByIdAndOwnerId(second.getId(), 1L)).contains(second);
    }

    @Test
    void deletingTheTaskFoundByIdAndOwnerIdLeavesOtherOwnersTasksUntouched() {
        Task mine = save("Mine", 1L, false);
        Task theirs = save("Theirs", 2L, false);

        taskRepository.delete(taskRepository.findByIdAndOwnerId(mine.getId(), 1L).orElseThrow());
        entityManager.flush();
        entityManager.clear();

        assertThat(taskRepository.findById(mine.getId())).isEmpty();
        assertThat(taskRepository.findById(theirs.getId())).isPresent();
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

    @Test
    void taskSavedWithoutAnExplicitPriorityIsPersistedAndReloadedAsMedium() {
        Long id = taskRepository.save(new Task("No priority set", null, 1L)).getId();

        assertThat(reload(id).getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void newTaskDefaultsToMediumPriorityInMemory() {
        assertThat(new Task("Fresh", null, 1L).getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void highPriorityIsPersistedAndReloaded() {
        Task task = new Task("Urgent", null, 1L);
        task.setPriority(Priority.HIGH);
        Long id = taskRepository.save(task).getId();

        assertThat(reload(id).getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void lowPriorityIsPersistedAndReloaded() {
        Task task = new Task("Whenever", null, 1L);
        task.setPriority(Priority.LOW);
        Long id = taskRepository.save(task).getId();

        assertThat(reload(id).getPriority()).isEqualTo(Priority.LOW);
    }

    @Test
    void priorityCanBeChangedAfterBeingSet() {
        Task task = new Task("Reprioritised", null, 1L);
        task.setPriority(Priority.LOW);
        Long id = taskRepository.save(task).getId();

        Task loaded = reload(id);
        loaded.setPriority(Priority.HIGH);
        taskRepository.save(loaded);

        assertThat(reload(id).getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void priorityIsStoredAsTheEnumNameText() {
        Task task = new Task("Stored as text", null, 1L);
        task.setPriority(Priority.HIGH);
        Long id = taskRepository.save(task).getId();
        entityManager.flush();

        Object stored = entityManager
                .createNativeQuery("select priority from task where id = ?1")
                .setParameter(1, id)
                .getSingleResult();

        assertThat(stored).isEqualTo("HIGH");
    }

    @Test
    void priorityDoesNotAffectOwnerScopedQueries() {
        Task high = new Task("High", null, 1L);
        high.setPriority(Priority.HIGH);
        taskRepository.save(high);
        Task otherUsers = new Task("Other user's", null, 2L);
        otherUsers.setPriority(Priority.HIGH);
        taskRepository.save(otherUsers);

        assertThat(taskRepository.findByOwnerId(1L))
                .extracting(Task::getTitle, Task::getPriority)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("High", Priority.HIGH));
    }

    @Test
    void nullPriorityIsRejectedByThePersistenceLayer() {
        Task task = new Task("Null priority", null, 1L);
        task.setPriority(null);

        assertThatThrownBy(() -> {
            taskRepository.save(task);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
