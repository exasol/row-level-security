package com.exasol.adapter.dialects.rls;

import com.exasol.adapter.AdapterException;
import com.exasol.adapter.sql.*;

public class RlsEnablingStatementVisitor implements SqlNodeVisitor<SqlStatementSelect.Builder> {

    @Override
    public SqlStatementSelect.Builder visit(SqlStatementSelect select) throws AdapterException {
        return SqlStatementSelect.builder().selectList(select.getSelectList()).fromClause(select.getFromClause())
              .groupBy(select.getGroupBy()).having(select.getHaving()).orderBy(select.getOrderBy())
              .whereClause(select.getWhereClause()).limit(select.getLimit());
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlSelectList selectList) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlGroupBy groupBy) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlColumn sqlColumn) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlFunctionAggregate sqlFunctionAggregate) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlFunctionAggregateGroupConcat sqlFunctionAggregateGroupConcat)
          throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlFunctionScalar sqlFunctionScalar) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlFunctionScalarCase sqlFunctionScalarCase) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlFunctionScalarCast sqlFunctionScalarCast) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlFunctionScalarExtract sqlFunctionScalarExtract) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlLimit sqlLimit) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlLiteralBool sqlLiteralBool) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlLiteralDate sqlLiteralDate) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlLiteralDouble sqlLiteralDouble) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlLiteralExactnumeric sqlLiteralExactnumeric) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlLiteralNull sqlLiteralNull) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlLiteralString sqlLiteralString) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlLiteralTimestamp sqlLiteralTimestamp) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlLiteralTimestampUtc sqlLiteralTimestampUtc) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlLiteralInterval sqlLiteralInterval) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlOrderBy sqlOrderBy) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlPredicateAnd sqlPredicateAnd) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlPredicateBetween sqlPredicateBetween) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlPredicateEqual sqlPredicateEqual) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlPredicateInConstList sqlPredicateInConstList) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlPredicateLess sqlPredicateLess) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlPredicateLessEqual sqlPredicateLessEqual) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlPredicateLike sqlPredicateLike) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlPredicateLikeRegexp sqlPredicateLikeRegexp) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlPredicateNot sqlPredicateNot) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlPredicateNotEqual sqlPredicateNotEqual) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlPredicateOr sqlPredicateOr) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlPredicateIsNotNull sqlPredicateOr) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlPredicateIsNull sqlPredicateOr) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlTable sqlTable) throws AdapterException {
        return null;
    }

    @Override
    public SqlStatementSelect.Builder visit(SqlJoin sqlJoin) throws AdapterException {
        return null;
    }
}
