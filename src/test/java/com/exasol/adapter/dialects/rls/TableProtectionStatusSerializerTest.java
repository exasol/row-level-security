package com.exasol.adapter.dialects.rls;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class TableProtectionStatusSerializerTest {
    @Test
    void testSerialize() {
        final TableProtectionStatus tableProtectionStatus = TableProtectionStatus //
                .builder()//
                .addRoleProtectedTable("TableA") //
                .addTenantProtectedTable("TableB") //
                .addTable("TableC", true, true) //
                .build();
        final TableProtectionStatusSerializer serializer = new TableProtectionStatusSerializer();
        assertThat(Arrays.asList(serializer.serialize(tableProtectionStatus).split("\n")),
                containsInAnyOrder("TableA:r-", "TableB:-t", "TableC:rt"));
    }

    @Test
    void testDeserialize() {
        final TableProtectionStatusSerializer serializer = new TableProtectionStatusSerializer();
        final String serializedStatus = "TableD:-t\nTableE:r-\nTableF:rt";
        final TableProtectionStatus status = serializer.deserialize(serializedStatus);
        assertAll(() -> assertThat("TableD:-*", status.isTableRoleProtected("TableD"), equalTo(false)),
                () -> assertThat("TableD:*t", status.isTableTenantProtected("TableD"), equalTo(true)),
                () -> assertThat("TableE:r*", status.isTableRoleProtected("TableE"), equalTo(true)),
                () -> assertThat("TableE:*-", status.isTableTenantProtected("TableE"), equalTo(false)),
                () -> assertThat("TableF:r*", status.isTableTenantProtected("TableF"), equalTo(true)),
                () -> assertThat("TableF:*t", status.isTableTenantProtected("TableF"), equalTo(true)));
    }
}