package com.technizer.taskapi.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unreadableBodyReturns400WithGenericMessage() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: Cannot deserialize value of type `java.time.LocalDate` from String \"bad\"",
                new MockHttpInputMessage(new byte[0]));

        ResponseEntity<Map<String, String>> response = handler.handleUnreadableBody(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsExactly(Map.entry("error", "Malformed request body"));
    }

    @Test
    void unreadableBodyResponseDoesNotLeakParserDetails() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "internal parser detail", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<Map<String, String>> response = handler.handleUnreadableBody(ex);

        assertThat(response.getBody().get("error")).doesNotContain("internal parser detail");
    }

    @Test
    void illegalArgumentStillReturns400WithItsMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleBadRequest(new IllegalArgumentException("Title must not be blank"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Title must not be blank");
    }

    @Test
    void noSuchElementStillReturns404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleNotFound(new NoSuchElementException("Task not found: 1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unexpectedExceptionStillReturnsGeneric500() {
        ResponseEntity<Map<String, String>> response = handler.handleGeneric(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "Unexpected server error");
    }
}
