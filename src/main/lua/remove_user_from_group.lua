--[[
CREATE OR REPLACE SCRIPT REMOVE_USER_FROM_GROUP(user_name, array user_groups) AS
--]]
-- [impl->dsn~remove-user-from-group~1]
-- [impl->dsn~remove-user-from-group-validates-user-name~1]
-- [impl->dsn~remove-user-from-group-validates-group-names~1]
import(exa.meta.script_schema .. '.EXA_IDENTIFIER', 'identifier')

function remove_members()
    local comma_separated_groups = table.concat(user_groups, "', '")
    query("DELETE FROM ::s.EXA_GROUP_MEMBERS WHERE EXA_USER_NAME = :u AND EXA_GROUP IN('" .. comma_separated_groups
            .. "')",
            { s = exa.meta.script_schema, u = user_name})
end

function drop_temporary_member_table()
    query("DROP TABLE ::s.EXA_NEW_GROUP_MEMBERS", { s = exa.meta.script_schema })
end

identifier.assert_user_name(user_name)
identifier.assert_groups(user_groups)
remove_members()
--[[
/
--]]