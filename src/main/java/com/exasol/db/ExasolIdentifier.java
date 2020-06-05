package com.exasol.db;

import static java.lang.Character.*;

import java.util.Objects;

import javax.annotation.processing.Generated;

/**
 * Exasol database object identifier.
 * <p>
 * This class exists mainly to prevent SQL injection via variable identifiers in SQL statements.
 * </p>
 */
public final class ExasolIdentifier implements Identifier {
    private final String id;

    private ExasolIdentifier(final String id) {
        this.id = id;
    }

    /**
     * Get the identifier as a {@link String}.
     *
     * @return identifier string
     */
    @Override
    public String toString() {
        return this.id;
    }

    /**
     * Get the quoted identifier as a {@link String}.
     *
     * @return quoted identifier
     */
    @Override
    public String quote() {
        return "\"" + this.id + "\"";
    }

    @Generated("org.eclipse.Eclipse")
    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Generated("org.eclipse.Eclipse")
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ExasolIdentifier)) {
            return false;
        }
        final ExasolIdentifier other = (ExasolIdentifier) obj;
        return Objects.equals(this.id, other.id);
    }

    /**
     * Create a new {@link ExasolIdentifier}.
     * <p>
     * This method validates the identifier before creating it and throws and {@link AssertionError} in case the
     * identifier is not valid.
     * </p>
     *
     * @param id the identifier as {@link String}
     * @return new {@link ExasolIdentifier} instance
     */
    public static Identifier of(final String id) {
        if (validate(id)) {
            return new ExasolIdentifier(id);
        } else {
            throw new AssertionError("E-ID-1: Unable to create identifier \"" + id //
                    + "\" because it contains illegal characters." //
                    + " For information about valid identifiers, please refer to" //
                    + " https://docs.exasol.com/sql_references/basiclanguageelements.htm#SQL_Identifier");
        }
    }

    /**
     * Check if a string is a valid identifier.
     *
     * @param id identifier to be validated
     * @return {@code true} if the string is a valid identifier
     */
    public static boolean validate(final String id) {
        if ((id == null) || id.isEmpty()) {
            return false;
        }
        if (!validateFirstCharacter(id.codePointAt(0))) {
            return false;
        }
        for (int i = 1; i < id.length(); ++i) {
            if (!validateFollowUpCharacter(id.codePointAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean validateFirstCharacter(final int codePoint) {
        switch (Character.getType(codePoint)) {
        case UPPERCASE_LETTER:
        case LOWERCASE_LETTER:
        case TITLECASE_LETTER:
        case MODIFIER_LETTER:
        case OTHER_LETTER:
        case LETTER_NUMBER:
            return true;
        default:
            return false;
        }
    }

    private static boolean validateFollowUpCharacter(final int codePoint) {
        switch (Character.getType(codePoint)) {
        case UPPERCASE_LETTER:
        case LOWERCASE_LETTER:
        case TITLECASE_LETTER:
        case MODIFIER_LETTER:
        case OTHER_LETTER:
        case LETTER_NUMBER:
        case NON_SPACING_MARK:
        case COMBINING_SPACING_MARK:
        case DECIMAL_DIGIT_NUMBER:
        case CONNECTOR_PUNCTUATION:
        case FORMAT:
            return true;
        default:
            return (codePoint == 0x00B7);
        }
    }
}