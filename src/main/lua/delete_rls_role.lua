--[[
CREATE OR REPLACE SCRIPT DELETE_RLS_ROLE(role_name) AS
--]]
-- [impl->dsn~delete-a-role~1]
-- [impl->dsn~delete-rls-role-removes-a-role-from-administrative-tables~1]
-- [impl->dsn~delete-rls-role-removes-a-role-from-roles-secured-tables~1]
-- [impl->dsn~delete-rls-role-removes-a-role-from-user-table~1]
import(exa.meta.script_schema .. '.EXA_RLS_BASE', 'BASE')

function assert_role_exists(role_id)
    if role_id == nil then
        error('Unable to delete RLS role "' .. role_name .. '" because it does not exist.')
    end
end

function delete_role_from_mapping(role_id)
    query([[DELETE FROM ::s.EXA_ROLES_MAPPING WHERE role_id = :i]],
          {s = exa.meta.script_schema, i = role_id})
end

function unassign_role_from_users(role_id)
    query([[UPDATE ::s.EXA_RLS_USERS SET EXA_ROLE_MASK = BIT_AND(EXA_ROLE_MASK, BIT_NOT(BIT_SET(0, :i - 1)))
            WHERE BIT_CHECK(EXA_ROLE_MASK, :i - 1)]],
            {s = exa.meta.script_schema, i = role_id})
end

function remove_role_from_protected_rows(role_id)
    res = query([[SELECT COLUMN_TABLE FROM SYS.EXA_ALL_COLUMNS
                  WHERE COLUMN_SCHEMA = :s AND COLUMN_NAME = 'EXA_ROW_ROLES']],
                 {s = exa.meta.script_schema })
    for i = 1, #res do
       query([[UPDATE ::s.::t SET EXA_ROW_ROLES = BIT_AND(EXA_ROW_ROLES, BIT_NOT(BIT_SET(0, :i - 1)))
               WHERE BIT_CHECK(EXA_ROW_ROLES, :i - 1)]],
               {s = exa.meta.script_schema, t = res[i].COLUMN_TABLE, i = role_id})
    end
end

local role_id = BASE.get_role_id_by_name(role_name)
assert_role_exists(role_id)
delete_role_from_mapping(role_id)
unassign_role_from_users(role_id)
remove_role_from_protected_rows(role_id)

--[[
/
--]]