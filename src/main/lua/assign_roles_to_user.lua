-- [impl->dsn~assign-roles-to-a-user~1]
-- [impl->dsn~assign-roles-to-user-creates-a-role~1]
--[[
CREATE OR REPLACE SCRIPT ASSIGN_ROLES_TO_USER(user_name, array roles) AS
--]]
import(exa.meta.script_schema .. '.EXA_RLS_BASE', 'base') -- initializes missing database objects
import(exa.meta.script_schema .. '.EXA_IDENTIFIER', 'identifier')

function assign_roles_to_user()
    local comma_separated_role_list = "'" .. table.concat(roles, "', '") .. "'"
    local sql = [[MERGE INTO ::s.EXA_RLS_USERS U
                  USING (SELECT :u AS EXA_USER_NAME,
                  (SELECT SUM(DISTINCT BIT_SET(0, (ROLE_ID) - 1))
                          FROM ::s.EXA_ROLES_MAPPING
                          WHERE ROLE_NAME IN (]] .. comma_separated_role_list .. [[)
                  ) AS EXA_ROLE_MASK) N
                  ON U.EXA_USER_NAME = N.EXA_USER_NAME
                  WHEN MATCHED THEN UPDATE SET U.EXA_ROLE_MASK = N.EXA_ROLE_MASK
                  WHEN NOT MATCHED THEN INSERT VALUES (N.EXA_USER_NAME, N.EXA_ROLE_MASK)]]
    query(sql, { s = exa.meta.script_schema, r = roles, u = user_name })
end

identifier.assert_user_name(user_name)
identifier.assert_roles(roles)
assign_roles_to_user()
--[[
/
--]]
