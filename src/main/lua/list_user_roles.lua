--[[
CREATE OR REPLACE SCRIPT LIST_USER_ROLES(user_name) RETURNS TABLE AS
--]]
-- [impl->dsn~listing-user-roles~1]
--exit(query([[SELECT u.EXA_USER_NAME, rm.ROLE_NAME FROM ::s.EXA_ROLES_MAPPING rm
--LEFT JOIN ::s.EXA_RLS_USERS u ON BIT_CHECK(u.EXA_ROLE_MASK, rm.ROLE_ID - 1)
--WHERE u.EXA_USER_NAME = :username
--ORDER BY u.EXA_USER_NAME, rm.ROLE_NAME]],
--    { s = exa.meta.script_schema, username = user_name }))
exit(query([[SELECT *
    FROM (
    (
        SELECT u.EXA_USER_NAME, '<has unmapped role(s)>' AS ROLE_NAME
        FROM ::schema.EXA_RLS_USERS u
        WHERE u.EXA_USER_NAME = :user_name
            AND BIT_AND(u.EXA_ROLE_MASK, BIT_NOT(
                     SELECT SUM(DISTINCT(BIT_SET(0, rm.ROLE_ID - 1))) FROM ::schema.EXA_ROLES_MAPPING rm
                 )) > 0
    )
    UNION ALL
    (
        SELECT u.EXA_USER_NAME, rm.ROLE_NAME
        FROM ::schema.EXA_RLS_USERS u
        INNER JOIN ::schema.EXA_ROLES_MAPPING rm ON BIT_CHECK(u.EXA_ROLE_MASK, rm.ROLE_ID - 1)
        WHERE u.EXA_USER_NAME = :user_name
    )
)
ORDER BY EXA_USER_NAME, ROLE_NAME]],
    { schema = exa.meta.script_schema, user_name = user_name }))
--[[
/
--]]