package com.exasol.adapter.dialects.rls;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class TableProtectionDetailsTest {
    @Test
    void testIsRoleProtectedFalseByDefault() {
        assertThat(TableProtectionDetails.builder().build().isRoleProtected(), equalTo(false));
    }

    @Test
    void testIsRoleProtectedTrue() {
        assertThat(TableProtectionDetails.builder().roleProtected(true).build().isRoleProtected(), equalTo(true));
    }

    @Test
    void testIsTenanatProtectedFalseByDefault() {
        assertThat(TableProtectionDetails.builder().build().isTenantProtected(), equalTo(false));
    }

    @Test
    void testIsTenantProtectedTrue() {
        assertThat(TableProtectionDetails.builder().tenantProtected(true).build().isTenantProtected(), equalTo(true));
    }

    @Test
    void testIsGroupProtectedFalseByDefault() {
        assertThat(TableProtectionDetails.builder().build().isGroupProtected(), equalTo(false));
    }

    @Test
    void testIsGroupProtectedTrue() {
        assertThat(TableProtectionDetails.builder().groupProtected(true).build().isGroupProtected(), equalTo(true));
    }

    @CsvSource({ //
            "false, false, false, false", //
            "true , false, false, true", //
            "false, true , false, true", //
            "false, false, true , true", //
            "true , true , false, true", //
            "false , true, true , true" })
    @ParameterizedTest
    void testIsProtected(final boolean roleProtected, final boolean tenantProtected, final boolean groupProtected,
            final boolean expectedGenerallyProtected) {
        final TableProtectionDetails.Builder builder = TableProtectionDetails.builder();
        if (roleProtected) {
            builder.roleProtected(true);
        }
        if (tenantProtected) {
            builder.tenantProtected(true);
        }
        if (groupProtected) {
            builder.groupProtected(true);
        }
        assertThat(
                "Protected by role: " + roleProtected + ", tenant: " + tenantProtected + ", group: " + groupProtected
                        + " is generally protected?",
                builder.build().isProtected(), equalTo(expectedGenerallyProtected));
    }

    @CsvSource({ //
            "false, false, false, Not protected.", //
            "true , false, false, Protected by role.", //
            "false, true , false, Protected by tenant.", //
            "false, false, true , Protected by group.", //
            "true , true , false, Protected by tenant and role.", //
            "false , true, true , Protected by tenant and group." })
    @ParameterizedTest
    void testDescribe(final boolean roleProtected, final boolean tenantProtected, final boolean groupProtected,
            final String expectedDescription) {
        final TableProtectionDetails.Builder builder = TableProtectionDetails.builder();
        if (roleProtected) {
            builder.roleProtected(true);
        }
        if (tenantProtected) {
            builder.tenantProtected(true);
        }
        if (groupProtected) {
            builder.groupProtected(true);
        }
        assertThat("Protected by role: " + roleProtected + ", tenant: " + tenantProtected + ", group: " + groupProtected
                + " described as?", builder.build().describe(), equalTo(expectedDescription));
    }
}