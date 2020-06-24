--[[
CREATE OR REPLACE SCRIPT LIST_USER_GROUPS(user_name) RETURNS TABLE AS
--]]
-- [impl->dsn~listing-a-users-groups~1]
exit(query("SELECT EXA_GROUP FROM ::s.EXA_GROUP_MEMBERS WHERE EXA_USER_NAME = :u ORDER BY EXA_GROUP",
    { s = exa.meta.script_schema, u = user_name }))
--[[
/
--]]