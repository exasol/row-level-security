--/
CREATE OR REPLACE SCRIPT ADD_RLS_ROLE(role_name, role_id) AS
import('"'..exa.meta.script_schema..'".EXA_RLS_BASE', 'BASE')
if not BASE.role_id_is_valid(role_id) then error('Invalid role id. Role id must be between 1 and 63.') end
_role_id = BASE.get_role_id_by_name(role_name)
_role_name = BASE.get_role_name_by_id(role_id)
if _role_id~=nil then error('Role name "'..role_name..'" already exists (role id '.._role_id..').') end
if _role_name~=nil then error('Role id '..role_id..' already exists (role name "'.._role_name..'").') end
query([[INSERT INTO ::s.EXA_ROLES_MAPPING VALUES (:r, :i)]],
      {s = exa.meta.script_schema, r = role_name, i = role_id })
/