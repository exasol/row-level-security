--[[
CREATE OR REPLACE SCRIPT LIST_RLS_GROUPS() RETURNS TABLE AS
--]]
-- [impl->dsn~list-groups~1]
exit(query("SELECT EXA_GROUP, COUNT(1) FROM ::s.EXA_GROUP_MEMBERS GROUP BY EXA_GROUP ORDER BY EXA_GROUP",
        { s = exa.meta.script_schema }))
--[[
/
--]]