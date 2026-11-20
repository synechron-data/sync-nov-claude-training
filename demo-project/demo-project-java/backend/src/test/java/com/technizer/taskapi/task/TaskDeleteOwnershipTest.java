package com.technizer.taskapi.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.technizer.taskapi.exception.GlobalExceptionHandler;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Exercises DELETE /api/tasks/{id} through the real controller, real TaskService and
 * GlobalExceptionHandler with only the repository mocked. The principal is placed in the
 * SecurityContext directly (not via @WithMockUser, whose principal is not a Long).
 */
class TaskDeleteOwnershipTest {

  private final TaskRepository taskRepository = mock(TaskRepository.class);
  private final Task usersOneTask = new Task("User 1's task", "Private", 1L);
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new TaskController(new TaskService(taskRepository)))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

    // Scoped lookup: only the owning user (1) can see task 10.
    when(taskRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(usersOneTask));
    when(taskRepository.findByIdAndOwnerId(10L, 2L)).thenReturn(Optional.empty());
    when(taskRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAs(Long userId) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
  }

  @Test
  void ownerDeletingOwnTaskGets200AndTheTaskIsDeleted() throws Exception {
    authenticateAs(1L);

    mockMvc
        .perform(delete("/api/tasks/10"))
        .andExpect(status().isOk())
        .andExpect(content().string(""));

    verify(taskRepository).delete(usersOneTask);
  }

  @Test
  void anotherUserDeletingTheTaskGets404JsonAndTheTaskIsUntouched() throws Exception {
    authenticateAs(2L);

    mockMvc
        .perform(delete("/api/tasks/10"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Task not found: 10"));

    verify(taskRepository, never()).delete(any(Task.class));
    verify(taskRepository, never()).deleteById(any());
  }

  @Test
  void deletingANonExistentTaskGets404Json() throws Exception {
    authenticateAs(1L);

    mockMvc
        .perform(delete("/api/tasks/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Task not found: 99"));

    verify(taskRepository, never()).delete(any(Task.class));
  }

  @Test
  void foreignAndNonExistentTaskResponsesHaveTheSameShapeSoIdsCannotBeProbed() throws Exception {
    // Task 10 exists but belongs to user 1; task 99 exists for nobody. User 2 asks for both.
    when(taskRepository.findByIdAndOwnerId(99L, 2L)).thenReturn(Optional.empty());
    authenticateAs(2L);

    String foreign = deleteAndReadNotFoundBody("/api/tasks/10");
    String missing = deleteAndReadNotFoundBody("/api/tasks/99");

    // Identical apart from the echoed id: same status, same JSON shape, same wording.
    assertThat(foreign.replace("10", "<id>")).isEqualTo(missing.replace("99", "<id>"));
    assertThat(foreign).contains("\"error\"").contains("Task not found: 10");
    assertThat(missing).contains("\"error\"").contains("Task not found: 99");
  }

  private String deleteAndReadNotFoundBody(String path) throws Exception {
    return mockMvc
        .perform(delete(path))
        .andExpect(status().isNotFound())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  void ownersTaskIsStillDeletableByItsOwnerAfterAnotherUsersFailedAttempt() throws Exception {
    authenticateAs(2L);
    mockMvc.perform(delete("/api/tasks/10")).andExpect(status().isNotFound());

    authenticateAs(1L);
    mockMvc.perform(delete("/api/tasks/10")).andExpect(status().isOk());

    verify(taskRepository).delete(usersOneTask);
  }
}
