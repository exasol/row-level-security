--/
CREATE OR REPLACE SCRIPT MY_SCHEMA.DELETE_RLS_ROLE(role_name) AS
import('"'..exa.meta.script_schema..'".EXA_RLS_BASE', 'BASE')
_role_id = BASE.get_role_id_by_name(role_name)
if _role_id==nil then error('no such role_name: "'..role_name) end
query([[DELETE FROM ::s.EXA_ROLES_MAPPING WHERE role_name = :i]],
      {s = exa.meta.script_schema, r = _role_id })
query([[UPDATE ::s.EXA_RLS_USERS SET EXA_ROLE_MASK = EXA_ROLE_MASK - POWER(2,:i-1)
        WHERE BIT_AND(EXA_ROLE_MASK, POWER(2,:i-1)) != 0]],
        {s = exa.meta.script_schema, i = _role_id })
/;