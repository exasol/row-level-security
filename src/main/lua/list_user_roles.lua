--[[
CREATE OR REPLACE SCRIPT LIST_USER_ROLES(user_name) RETURNS TABLE AS
--]]
-- [impl->dsn~listing-user-roles~1]
exit(query([[SELECT u.EXA_USER_NAME, rm.ROLE_NAME FROM ::s.EXA_ROLES_MAPPING rm
LEFT JOIN ::s.EXA_RLS_USERS u ON BIT_CHECK(u.EXA_ROLE_MASK, rm.ROLE_ID - 1)
WHERE u.EXA_USER_NAME = :username
ORDER BY u.EXA_USER_NAME, rm.ROLE_NAME]],
    { s = exa.meta.script_schema, username = user_name }))
--[[
/
--]]