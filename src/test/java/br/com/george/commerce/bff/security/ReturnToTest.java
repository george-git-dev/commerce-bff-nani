package br.com.george.commerce.bff.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ReturnToTest {

    @ParameterizedTest
    @ValueSource(strings = {"/", "/minha-conta", "/finalizar-compra", "/produtos/lattafa-asad?tipo=decant"})
    void shouldAcceptPathsOfTheSite(String path) {
        assertThat(ReturnTo.isSafe(path)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"https://evil.com", "//evil.com", "/\\evil.com", "minha-conta", "/x?next=https://evil.com"})
    void shouldRejectAnythingThatCouldLeaveTheSite(String path) {
        assertThat(ReturnTo.isSafe(path)).isFalse();
    }
}
