--/
CREATE OR REPLACE SCRIPT DELETE_RLS_ROLE(role_name) AS
import('"'..exa.meta.script_schema..'".EXA_RLS_BASE', 'BASE')
_role_id = BASE.get_role_id_by_name(role_name)
if _role_id==nil then error('No such role_name: "'..role_name) end
-- Remove role from EXA_ROLES_MAPPING table
query([[DELETE FROM ::s.EXA_ROLES_MAPPING WHERE role_id = :i]],
      {s = exa.meta.script_schema, i = _role_id })
-- Remove role from all users
query([[UPDATE ::s.EXA_RLS_USERS SET EXA_ROLE_MASK = BIT_AND(EXA_ROLE_MASK,BIT_NOT(POWER(2,:i-1)))
        WHERE BIT_AND(EXA_ROLE_MASK, POWER(2,:i-1)) != 0]],
        {s = exa.meta.script_schema, i = _role_id })
-- Remove role from all rows in each role-secured table
res = query([[SELECT COLUMN_TABLE FROM SYS.EXA_ALL_COLUMNS
              WHERE COLUMN_SCHEMA = :s AND COLUMN_NAME = 'EXA_ROW_ROLES']],
             {s = exa.meta.script_schema });
for i=1, #res do
   query([[UPDATE ::s.::t SET EXA_ROW_ROLES = EXA_ROW_ROLES - POWER(2,:i-1)
           WHERE BIT_AND(EXA_ROW_ROLES, POWER(2,:i-1)) != 0]],
           {s = exa.meta.script_schema, t = res[i].COLUMN_TABLE, i = _role_id })
end
/