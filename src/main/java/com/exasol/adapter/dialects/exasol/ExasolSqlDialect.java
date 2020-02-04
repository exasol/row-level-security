package com.exasol.adapter.dialects.exasol;

import static com.exasol.adapter.AdapterProperties.*;
import static com.exasol.adapter.capabilities.MainCapability.*;
import static com.exasol.adapter.dialects.exasol.ExasolProperties.EXASOL_CONNECTION_STRING_PROPERTY;
import static com.exasol.adapter.dialects.exasol.ExasolProperties.EXASOL_IMPORT_PROPERTY;
import static com.exasol.adapter.sql.ScalarFunction.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.capabilities.*;
import com.exasol.adapter.dialects.*;
import com.exasol.adapter.jdbc.*;

/**
 * Exasol SQL dialect.
 */
public class ExasolSqlDialect extends AbstractSqlDialect {
    static final String NAME = "EXASOL";
    private static final Capabilities CAPABILITIES = createCapabilityList();
    private static final List<String> SUPPORTED_PROPERTIES = Arrays.asList(SQL_DIALECT_PROPERTY,
            CONNECTION_NAME_PROPERTY, CONNECTION_STRING_PROPERTY, USERNAME_PROPERTY, PASSWORD_PROPERTY,
            CATALOG_NAME_PROPERTY, SCHEMA_NAME_PROPERTY, TABLE_FILTER_PROPERTY, EXASOL_IMPORT_PROPERTY,
            EXASOL_CONNECTION_STRING_PROPERTY, IS_LOCAL_PROPERTY, EXCLUDED_CAPABILITIES_PROPERTY,
            DEBUG_ADDRESS_PROPERTY, LOG_LEVEL_PROPERTY);

    /**
     * Create a new instance of the {@link ExasolSqlDialect}.
     *
     * @param connectionFactory factory for the JDBC connection to the remote data source
     * @param properties        adapter properties
     */
    public ExasolSqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties) {
        super(connectionFactory, properties);
        this.omitParenthesesMap.add(SYSDATE);
        this.omitParenthesesMap.add(SYSTIMESTAMP);
        this.omitParenthesesMap.add(CURRENT_SCHEMA);
        this.omitParenthesesMap.add(CURRENT_SESSION);
        this.omitParenthesesMap.add(CURRENT_STATEMENT);
        this.omitParenthesesMap.add(CURRENT_USER);
    }

    @Override
    public String getName() {
        return NAME;
    }

    private static Capabilities createCapabilityList() {
        return Capabilities.builder() //
                .addMain(SELECTLIST_PROJECTION, SELECTLIST_EXPRESSIONS, FILTER_EXPRESSIONS, AGGREGATE_SINGLE_GROUP,
                        AGGREGATE_GROUP_BY_COLUMN, AGGREGATE_GROUP_BY_EXPRESSION, AGGREGATE_GROUP_BY_TUPLE,
                        AGGREGATE_HAVING, ORDER_BY_COLUMN, ORDER_BY_EXPRESSION, LIMIT, LIMIT_WITH_OFFSET, JOIN,
                        JOIN_TYPE_INNER, JOIN_TYPE_LEFT_OUTER, JOIN_TYPE_RIGHT_OUTER, JOIN_TYPE_FULL_OUTER,
                        JOIN_CONDITION_EQUI) //
                .addLiteral(LiteralCapability.values()) //
                .addPredicate(PredicateCapability.values()) //
                .addAggregateFunction(AggregateFunctionCapability.values()) //
                .addScalarFunction(ScalarFunctionCapability.values()) //
                .build();
    }

    @Override
    protected RemoteMetadataReader createRemoteMetadataReader() {
        try {
            return new ExasolMetadataReader(this.connectionFactory.getConnection(), this.properties);
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException("Unable to create Exasol remote metadata reader.", exception);
        }
    }

    /**
     * Create a query rewriter.
     * <p>
     * Virtual Schema for Exasol supports the following import variants which are represented by dedicated query
     * re-writers:
     * <dl>
     * <dt>local</dt>
     * <dd>Create a {@code SELECT} statement that is directly executed on the local Exasol database.
     * <dt>{@code IMPORT FROM EXA}</dt>
     * <dd>Create dedicated import statement for a remote Exasol database that is more efficient than a regular JDBC
     * import.</dd>
     * <dt>JDBC import</dt>
     * <dd>Create a regular JDBC import</dd>
     * </dl>
     */
    @Override
    protected QueryRewriter createQueryRewriter() {
        if (this.properties.isLocalSource()) {
            return new ExasolLocalQueryRewriter(this);
        } else if (isImportFromExa(this.properties)) {
            return new ExasolFromExaQueryRewriter(this, createRemoteMetadataReader(), this.connectionFactory);
        } else {
            return new ExasolJdbcQueryRewriter(this, createRemoteMetadataReader(), this.connectionFactory);
        }
    }

    private boolean isImportFromExa(final AdapterProperties properties) {
        return properties.isEnabled(EXASOL_IMPORT_PROPERTY);
    }

    @Override
    public StructureElementSupport supportsJdbcCatalogs() {
        return StructureElementSupport.SINGLE;
    }

    @Override
    public StructureElementSupport supportsJdbcSchemas() {
        return StructureElementSupport.MULTIPLE;
    }

    @Override
    public Capabilities getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public String applyQuote(final String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public boolean requiresCatalogQualifiedTableNames(final SqlGenerationContext context) {
        return false;
    }

    @Override
    public boolean requiresSchemaQualifiedTableNames(final SqlGenerationContext context) {
        return true;
    }

    @Override
    public NullSorting getDefaultNullSorting() {
        return NullSorting.NULLS_SORTED_HIGH;
    }

    @Override
    public void validateProperties() throws PropertyValidationException {
        super.validateProperties();
        checkImportPropertyConsistency(EXASOL_IMPORT_PROPERTY, EXASOL_CONNECTION_STRING_PROPERTY);
        validateBooleanProperty(EXASOL_IMPORT_PROPERTY);
        validateBooleanProperty(IS_LOCAL_PROPERTY);
    }

    @Override
    protected List<String> getSupportedProperties() {
        return SUPPORTED_PROPERTIES;
    }
}