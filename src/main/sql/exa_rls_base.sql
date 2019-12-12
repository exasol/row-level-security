--/
CREATE OR REPLACE SCRIPT MY_SCHEMA.EXA_RLS_BASE AS
function get_role_id_by_name(role_name)
   res = query([[SELECT role_id FROM ::s.EXA_ROLES_MAPPING WHERE role_name = :r]],
               { s = exa.meta.script_schema, r = role_name })
   if #res>0 then return res[1][1] else return nil end
end
​
function get_role_name_by_id(role_id)
   res = query([[SELECT role_name FROM ::s.EXA_ROLES_MAPPING WHERE role_id = :i]],
               { s = exa.meta.script_schema, i = role_id })
   if #res>0 then return res[1][1] else return nil end
end
​
function get_roles_mask(roles)
   in_list = ''   -- will look like this: ':r1, :r2, :r3, :r4, ...'
   query_params = {s = exa.meta.script_schema}
   for i=1, #roles do
        if in_list~="" then in_list = in_list..', ' end
   		in_list = in_list..':r'..i
   		query_params['r'..i]=roles[i]
   end
   res = query([[select ROLE_ID from ::s.EXA_ROLES_MAPPING where ROLE_NAME IN (]]..in_list..[[)]],
               query_params)
   if #res == 0 then error('Role name not found') end
   role_mask = 0
   for i=1, #res do role_mask = role_mask + math.pow(2, res[i].ROLE_ID-1) end

   return role_mask
end
​
query([[CREATE TABLE IF NOT EXISTS ::s.EXA_ROLES_MAPPING(ROLE_NAME VARCHAR(128), ROLE_ID INT)]],
      { s = exa.meta.script_schema })
query([[CREATE TABLE IF NOT EXISTS ::s.EXA_RLS_USERS(EXA_USER_NAME VARCHAR(128), EXA_ROLE_MASK DECIMAL(20,0))]],
      { s = exa.meta.script_schema })
/;