-- [impl->dsn~add-rls-role-creates-a-table~1]
-- [impl->dsn~add-rls-roles-checks-parameters~1]
-- [impl->dsn~assign-roles-to-user-creates-a-table~1]
--[[
CREATE OR REPLACE SCRIPT EXA_RLS_BASE AS
--]]
query([[CREATE TABLE IF NOT EXISTS ::s.EXA_ROLES_MAPPING(ROLE_NAME VARCHAR(128), ROLE_ID DECIMAL(2,0))]],
      { s = exa.meta.script_schema })
query([[CREATE TABLE IF NOT EXISTS ::s.EXA_RLS_USERS(EXA_USER_NAME VARCHAR(128), EXA_ROLE_MASK DECIMAL(20,0))]],
      { s = exa.meta.script_schema })
function role_id_is_valid(role_id)
   return role_id >= 1 and role_id <= 63
end
function get_role_id_by_name(role_name)
   res = query([[SELECT role_id FROM ::s.EXA_ROLES_MAPPING WHERE upper(role_name) = upper(:r)]],
               { s = exa.meta.script_schema, r = role_name })
   if #res>0 then return res[1][1] else return nil end
end
function get_role_name_by_id(role_id)
   res = query([[SELECT role_name FROM ::s.EXA_ROLES_MAPPING WHERE role_id = :i]],
               { s = exa.meta.script_schema, i = role_id })
   if #res>0 then return res[1][1] else return nil end
end

function get_roles_mask(roles)
   in_list = ''
   query_params = {s = exa.meta.script_schema}
   roles_not_found = {}
   for i=1, #roles do
        if in_list~="" then in_list = in_list..', ' end
   		in_list = in_list..':r'..i
   		query_params['r'..i]=roles[i]
   		roles_not_found[roles[i]] = true
   end
   res = query([[select ROLE_ID, ROLE_NAME from ::s.EXA_ROLES_MAPPING where ROLE_NAME IN (]]..in_list..[[)]],
               query_params)
   role_mask = 0
   for i=1, #res do
      roles_not_found[res[i].ROLE_NAME] = nil
      role_mask = role_mask + math.pow(2, res[i].ROLE_ID-1)
   end
   roles_not_found_list = ''
   for role_name in pairs(roles_not_found) do
      if roles_not_found_list ~= '' then roles_not_found_list = roles_not_found_list..', ' end
      roles_not_found_list = roles_not_found_list..role_name
   end
   if roles_not_found_list ~= '' then error('The following roles were not found: '..roles_not_found_list..'.') end
   return role_mask
end
--[[
/
--]]