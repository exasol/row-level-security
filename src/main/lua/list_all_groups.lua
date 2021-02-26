--[[
CREATE OR REPLACE SCRIPT LIST_ALL_GROUPS() RETURNS TABLE AS
--]]
-- [impl->dsn~listing-all-groups~1]
exit(query([[SELECT EXA_GROUP, COUNT(1) AS NUMBER_OF_MEMBERS
FROM ::s.EXA_GROUP_MEMBERS
GROUP BY EXA_GROUP
ORDER BY EXA_GROUP]],
    { s = exa.meta.script_schema }))
--[[
/
--]]