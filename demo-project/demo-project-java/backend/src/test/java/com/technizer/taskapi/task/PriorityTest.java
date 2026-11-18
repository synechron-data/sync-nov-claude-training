package com.technizer.taskapi.task;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriorityTest {

    @ParameterizedTest
    @ValueSource(strings = {"low", "LOW", "Low", "lOw"})
    void parse_acceptsLowInAnyCase(String text) {
        assertThat(Priority.parse(text)).isEqualTo(Priority.LOW);
    }

    @ParameterizedTest
    @ValueSource(strings = {"medium", "MEDIUM", "Medium"})
    void parse_acceptsMediumInAnyCase(String text) {
        assertThat(Priority.parse(text)).isEqualTo(Priority.MEDIUM);
    }

    @ParameterizedTest
    @ValueSource(strings = {"high", "HIGH", "High"})
    void parse_acceptsHighInAnyCase(String text) {
        assertThat(Priority.parse(text)).isEqualTo(Priority.HIGH);
    }

    @Test
    void parse_trimsSurroundingWhitespace() {
        assertThat(Priority.parse("  high  ")).isEqualTo(Priority.HIGH);
        assertThat(Priority.parse("\tLow\n")).isEqualTo(Priority.LOW);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void parse_rejectsNullEmptyAndBlank(String text) {
        assertThatThrownBy(() -> Priority.parse(text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Priority must be one of: Low, Medium, High");
    }

    @ParameterizedTest
    @ValueSource(strings = {"urgent", "critical", "hi gh", "lowest", "1", "MEDIUMM"})
    void parse_rejectsUnknownValues(String text) {
        assertThatThrownBy(() -> Priority.parse(text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Priority must be one of: Low, Medium, High");
    }

    @Test
    void rank_ordersLowBelowMediumBelowHigh() {
        assertThat(Priority.LOW.getRank()).isLessThan(Priority.MEDIUM.getRank());
        assertThat(Priority.MEDIUM.getRank()).isLessThan(Priority.HIGH.getRank());
    }

    @Test
    void rank_hasExpectedValues() {
        assertThat(Priority.LOW.getRank()).isEqualTo(1);
        assertThat(Priority.MEDIUM.getRank()).isEqualTo(2);
        assertThat(Priority.HIGH.getRank()).isEqualTo(3);
    }

    @Test
    void values_areExactlyLowMediumHigh() {
        assertThat(Priority.values()).containsExactly(Priority.LOW, Priority.MEDIUM, Priority.HIGH);
    }
}
