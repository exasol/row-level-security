-- [impl->dsn~assign-roles-to-a-user~1]
-- [impl->dsn~assign-roles-to-user-creates-a-role~1]
--[[
CREATE OR REPLACE SCRIPT ASSIGN_ROLES_TO_USER(user_name, array roles) AS
--]]
import(exa.meta.script_schema .. '.EXA_RLS_BASE', 'BASE')
_roles_mask = BASE.get_roles_mask(roles)
query([[MERGE INTO ::s.EXA_RLS_USERS U
              USING (SELECT :u AS EXA_USER_NAME, :r AS EXA_ROLE_MASK) N
              ON U.EXA_USER_NAME = N.EXA_USER_NAME
              WHEN MATCHED THEN UPDATE SET U.EXA_ROLE_MASK = N.EXA_ROLE_MASK
              WHEN NOT MATCHED THEN INSERT VALUES (N.EXA_USER_NAME, N.EXA_ROLE_MASK)]],
              { s = exa.meta.script_schema, r = _roles_mask, u = user_name })
--[[
/
--]]