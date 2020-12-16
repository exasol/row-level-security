--[[
CREATE OR REPLACE SCRIPT LIST_ALL_ROLES() RETURNS TABLE AS
--]]
-- [impl->dsn~listing-all-roles~1]
exit(query("SELECT ROLE_NAME, ROLE_ID FROM ::s.EXA_ROLES_MAPPING",
    { s = exa.meta.script_schema }))
--[[
/
--]]