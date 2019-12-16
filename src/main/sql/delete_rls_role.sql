--/
create or replace SCRIPT MY_SCHEMA.DELETE_RLS_ROLE(role_name) AS
import('"'..exa.meta.script_schema..'".EXA_RLS_BASE', 'BASE')
_role_id = BASE.get_role_id_by_name(role_name)
if _role_id==nil then error('no such role_name: "'..role_name) end
-- Remove role from EXA_ROLES_MAPPING table
query([[delete from ::s.EXA_ROLES_MAPPING where role_id = :i]],
      {s = exa.meta.script_schema, i = _role_id })
-- Remove role from all users
query([[update ::s.EXA_RLS_USERS set EXA_ROLE_MASK = EXA_ROLE_MASK - power(2,:i-1)
        where BIT_AND(EXA_ROLE_MASK, power(2,:i-1)) != 0]],
        {s = exa.meta.script_schema, i = _role_id })
-- Remove role from all rows in each role-secured table
res = query([[select COLUMN_TABLE from SYS.EXA_ALL_COLUMNS
              where COLUMN_SCHEMA = :s and COLUMN_NAME = 'EXA_ROW_ROLES']],
             {s = exa.meta.script_schema });
for i=1, #res do
   query([[update ::s.::t set EXA_ROW_ROLES = EXA_ROW_ROLES - power(2,:i-1)
           where BIT_AND(EXA_ROW_ROLES, power(2,:i-1)) != 0]],
           {s = exa.meta.script_schema, t = res[i].COLUMN_TABLE, i = _role_id })
end
/;