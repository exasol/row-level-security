--[[
CREATE OR REPLACE SCRIPT LIST_USERS_AND_ROLES() RETURNS TABLE AS
--]]
-- [impl->dsn~listing-users-and-roles~1]
exit(query([[SELECT *
    FROM (
    (
        SELECT u.EXA_USER_NAME, '<has unmapped role(s)>' AS ROLE_NAME
        FROM ::schema.EXA_RLS_USERS u
        WHERE BIT_AND(u.EXA_ROLE_MASK, BIT_NOT(
                     SELECT SUM(DISTINCT(BIT_SET(0, rm.ROLE_ID - 1))) FROM ::schema.EXA_ROLES_MAPPING rm
                 )) > 0
    )
    UNION ALL
    (
        SELECT u.EXA_USER_NAME, rm.ROLE_NAME
        FROM ::schema.EXA_RLS_USERS u
        INNER JOIN ::schema.EXA_ROLES_MAPPING rm ON BIT_CHECK(u.EXA_ROLE_MASK, rm.ROLE_ID - 1)
    )
)
ORDER BY EXA_USER_NAME, ROLE_NAME]],
    { schema = exa.meta.script_schema }))
--[[
/
--]]