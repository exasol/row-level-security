package com.exasol.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import nl.jqno.equalsverifier.EqualsVerifier;

class ExasolIdentifierTest {
    @ValueSource(strings = { "t", "T", "foo", "FOO", "fOO", "Foo", "F1234", "FOO_BAR", "你好", "F\u00b7", "ǅ1", "Aǅ", "Ⅻ",
            "AⅫ" })
    @ParameterizedTest
    void testValidateTrue(final String id) {
        assertThat(ExasolIdentifier.validate(id), equalTo(true));
    }

    @ValueSource(strings = { "", "1234", "1FOO", "FOO-BAR", "FOO BAR" })
    @ParameterizedTest
    void testValidateFalse(final String id) {
        assertThat(ExasolIdentifier.validate(id), equalTo(false));
    }

    void testValdiateFalseWhenNull() {
        assertThat(ExasolIdentifier.validate(null), equalTo(false));
    }

    @Test
    void testGet() {
        assertThat(ExasolIdentifier.of("THE_TABLE").toString(), equalTo("THE_TABLE"));
    }

    @CsvSource({ "a, \"a\"", "Foo, \"Foo\"", "foo_bar, \"foo_bar\"" })
    @ParameterizedTest
    void testQuote(final String original, final String quoted) {
        assertThat(ExasolIdentifier.of(original).quote(), equalTo(quoted));
    }

    @Test
    void testOfIllegalStringThrowsAssertionError() {
        assertThrows(AssertionError.class, () -> ExasolIdentifier.of("4"));
    }

    @Test
    void testEqualsContract() {
        EqualsVerifier.forClass(ExasolIdentifier.class).verify();
    }
}