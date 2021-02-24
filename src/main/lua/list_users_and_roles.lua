--[[
CREATE OR REPLACE SCRIPT LIST_USERS_AND_ROLES() RETURNS TABLE AS
--]]
-- [impl->dsn~listing-users-and-roles~1]
exit(query([[SELECT u.EXA_USER_NAME, rm.ROLE_NAME FROM ::s.EXA_ROLES_MAPPING rm
LEFT JOIN ::s.EXA_RLS_USERS u ON BIT_CHECK(u.EXA_ROLE_MASK, rm.ROLE_ID - 1)
ORDER BY u.EXA_USER_NAME, rm.ROLE_NAME]],
    { s = exa.meta.script_schema }))
--[[
/
--]]