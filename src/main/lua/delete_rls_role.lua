--[[
CREATE OR REPLACE SCRIPT DELETE_RLS_ROLE(role_name) AS
--]]
-- [impl->dsn~delete-a-role~1]
-- [impl->dsn~delete-rls-role-removes-a-role-from-administrative-tables~1]
-- [impl->dsn~delete-rls-role-removes-a-role-from-roles-secured-tables~1]
-- [impl->dsn~delete-rls-role-removes-a-role-from-user-table~1]
import(exa.meta.script_schema .. '.EXA_RLS_BASE', 'BASE')

function assert_role_exists()
    _role_id = BASE.get_role_id_by_name(role_name)
    if _role_id == nil then
        error('Unable to delete RLS role "' .. role_name .. '" because it does not exist.')
    end
end

function delete_role_from_mapping()
    query([[DELETE FROM ::s.EXA_ROLES_MAPPING WHERE role_id = :i]],
          {s = exa.meta.script_schema, i = _role_id})
end

function unassign_role_from_users()
    query([[UPDATE ::s.EXA_RLS_USERS SET EXA_ROLE_MASK = BIT_AND(EXA_ROLE_MASK,BIT_NOT(POWER(2, :i - 1)))
            WHERE BIT_AND(EXA_ROLE_MASK, POWER(2, :i - 1)) != 0]],
            {s = exa.meta.script_schema, i = _role_id})
end

function remove_role_from_protected_rows()
    res = query([[SELECT COLUMN_TABLE FROM SYS.EXA_ALL_COLUMNS
                  WHERE COLUMN_SCHEMA = :s AND COLUMN_NAME = 'EXA_ROW_ROLES']],
                 {s = exa.meta.script_schema });
    for i = 1, #res do
       query([[UPDATE ::s.::t SET EXA_ROW_ROLES = EXA_ROW_ROLES - POWER(2, :i - 1)
               WHERE BIT_AND(EXA_ROW_ROLES, POWER(2, :i - 1)) != 0]],
               {s = exa.meta.script_schema, t = res[i].COLUMN_TABLE, i = _role_id})
    end
end

assert_role_exists()
delete_role_from_mapping()
unassign_role_from_users()
remove_role_from_protected_rows()

--[[
/
--]]