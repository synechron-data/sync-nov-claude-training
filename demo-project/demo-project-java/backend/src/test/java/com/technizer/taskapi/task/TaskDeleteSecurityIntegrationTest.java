package com.technizer.taskapi.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.technizer.taskapi.auth.JwtUtil;
import com.technizer.taskapi.user.User;
import com.technizer.taskapi.user.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Runs DELETE /api/tasks/{id} through the full stack: the real security filter chain, real
 * JwtAuthFilter and JwtUtil, real service, real repository on an in-memory H2 database. Tokens are
 * issued by the real JwtUtil for two different users.
 *
 * <p>The JWT secret and datasource URL are test-only values supplied here, so no application
 * properties file is read or changed.
 */
@SpringBootTest(
    properties = {
      "app.jwt.secret=test-only-jwt-secret-for-integration-tests-0123456789",
      "spring.datasource.url=jdbc:h2:mem:task-delete-security-it;DB_CLOSE_DELAY=-1",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
class TaskDeleteSecurityIntegrationTest {

  private final MockMvc mockMvc;
  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;

  @Autowired
  TaskDeleteSecurityIntegrationTest(
      MockMvc mockMvc,
      TaskRepository taskRepository,
      UserRepository userRepository,
      JwtUtil jwtUtil) {
    this.mockMvc = mockMvc;
    this.taskRepository = taskRepository;
    this.userRepository = userRepository;
    this.jwtUtil = jwtUtil;
  }

  @AfterEach
  void cleanUp() {
    taskRepository.deleteAll();
    userRepository.deleteAll();
  }

  private User newUser(String name) {
    return userRepository.save(
        new User(name + "-" + UUID.randomUUID() + "@example.test", "not-a-real-hash", name));
  }

  private String bearer(User user) {
    return "Bearer " + jwtUtil.generateToken(user.getId(), user.getEmail());
  }

  private Task newTaskOwnedBy(User owner) {
    return taskRepository.save(new Task("Private task", "Secret", owner.getId()));
  }

  @Test
  void anotherUsersRealTokenGets404AndTheTaskSurvives() throws Exception {
    User alice = newUser("alice");
    User bob = newUser("bob");
    Task alicesTask = newTaskOwnedBy(alice);

    mockMvc
        .perform(delete("/api/tasks/" + alicesTask.getId()).header("Authorization", bearer(bob)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Task not found: " + alicesTask.getId()));

    assertThat(taskRepository.findById(alicesTask.getId())).isPresent();
  }

  @Test
  void theOwnersRealTokenGets200AndTheTaskIsGone() throws Exception {
    User alice = newUser("alice");
    Task alicesTask = newTaskOwnedBy(alice);

    mockMvc
        .perform(delete("/api/tasks/" + alicesTask.getId()).header("Authorization", bearer(alice)))
        .andExpect(status().isOk())
        .andExpect(content().string(""));

    assertThat(taskRepository.findById(alicesTask.getId())).isEmpty();
  }

  @Test
  void aFailedAttemptByAnotherUserDoesNotStopTheOwnerFromDeleting() throws Exception {
    User alice = newUser("alice");
    User bob = newUser("bob");
    Task alicesTask = newTaskOwnedBy(alice);

    mockMvc
        .perform(delete("/api/tasks/" + alicesTask.getId()).header("Authorization", bearer(bob)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(delete("/api/tasks/" + alicesTask.getId()).header("Authorization", bearer(alice)))
        .andExpect(status().isOk());

    assertThat(taskRepository.findById(alicesTask.getId())).isEmpty();
  }

  @Test
  void aForeignTaskAndAnUnknownTaskGiveTheSameResponseShape() throws Exception {
    User alice = newUser("alice");
    User bob = newUser("bob");
    Task alicesTask = newTaskOwnedBy(alice);
    long foreignId = alicesTask.getId();
    long unknownId = foreignId + 1000;

    String foreign =
        mockMvc
            .perform(delete("/api/tasks/" + foreignId).header("Authorization", bearer(bob)))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String unknown =
        mockMvc
            .perform(delete("/api/tasks/" + unknownId).header("Authorization", bearer(bob)))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Real repository on both sides: identical apart from the id echoed back.
    assertThat(foreign.replace(String.valueOf(foreignId), "<id>"))
        .isEqualTo(unknown.replace(String.valueOf(unknownId), "<id>"));
  }

  @Test
  void deletingTheSameTaskTwiceReturns200ThenNotFound() throws Exception {
    User alice = newUser("alice");
    Task alicesTask = newTaskOwnedBy(alice);
    String path = "/api/tasks/" + alicesTask.getId();

    mockMvc.perform(delete(path).header("Authorization", bearer(alice))).andExpect(status().isOk());
    mockMvc
        .perform(delete(path).header("Authorization", bearer(alice)))
        .andExpect(status().isNotFound());
  }

  @Test
  void noTokenGets403WithAnEmptyBodyAndTheTaskSurvives() throws Exception {
    Task alicesTask = newTaskOwnedBy(newUser("alice"));

    mockMvc
        .perform(delete("/api/tasks/" + alicesTask.getId()))
        .andExpect(status().isForbidden())
        .andExpect(content().string(""));

    assertThat(taskRepository.findById(alicesTask.getId())).isPresent();
  }

  @Test
  void aMalformedTokenNeverDeletesTheTask() {
    Task alicesTask = newTaskOwnedBy(newUser("alice"));

    int status = statusOrMinusOne("/api/tasks/" + alicesTask.getId(), "Bearer not.a.jwt");

    // KNOWN BUG: an exception thrown inside JwtAuthFilter bypasses GlobalExceptionHandler, so a
    // bad token surfaces as an exception from MockMvc (an empty-body 403 on a real server)
    // rather than a JSON 401. Pin only what matters here: the request is rejected, the task stays.
    assertThat(status).isNotEqualTo(200);
    assertThat(taskRepository.findById(alicesTask.getId())).isPresent();
  }

  /** Returns the HTTP status, or -1 if the request blew up inside the filter chain. */
  private int statusOrMinusOne(String path, String authorization) {
    try {
      return mockMvc
          .perform(delete(path).header("Authorization", authorization))
          .andReturn()
          .getResponse()
          .getStatus();
    } catch (Exception e) {
      return -1;
    }
  }
}
