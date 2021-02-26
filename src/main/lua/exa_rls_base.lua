-- [impl->dsn~add-rls-role-creates-a-table~1]
-- [impl->dsn~add-rls-roles-checks-parameters~1]
-- [impl->dsn~assign-roles-to-user-creates-a-table~1]
--[[
CREATE OR REPLACE SCRIPT EXA_RLS_BASE AS
--]]
query([[CREATE TABLE IF NOT EXISTS ::s.EXA_ROLES_MAPPING
(ROLE_NAME VARCHAR(128) NOT NULL,
 ROLE_ID DECIMAL(2,0) NOT NULL PRIMARY KEY
)]],
    { s = exa.meta.script_schema })
query([[CREATE TABLE IF NOT EXISTS ::s.EXA_RLS_USERS
(EXA_USER_NAME VARCHAR(128) NOT NULL,
 EXA_ROLE_MASK DECIMAL(20,0) NOT NULL
)]],
    { s = exa.meta.script_schema })

---
-- Check if a role ID is valid.
-- <p>
-- Role IDs can be between 1 and 63. 64 is reserved for the public role.
-- </p>
--
-- @param role_id role ID to be checked
--
-- @return true if the role is valid
--
function role_id_is_valid(role_id)
    return role_id >= 1 and role_id <= 63
end

---
-- Get the ID of a role by its name.
--
-- @param role_name name of the role
--
-- @return ID of the role or <code>nil</code> if the role does not exist
--
function get_role_id_by_name(role_name)
    res = query([[SELECT role_id FROM ::s.EXA_ROLES_MAPPING WHERE upper(role_name) = upper(:r)]],
        { s = exa.meta.script_schema, r = role_name })
    if #res > 0 then
        return res[1][1]
    else
        return nil
    end
end

---
-- Get the name of a role by its ID.
--
-- @param role_id ID of the role
--
-- @return name of the role or <code>nil</code if the role does not exist
--
function get_role_name_by_id(role_id)
    res = query([[SELECT role_name FROM ::s.EXA_ROLES_MAPPING WHERE role_id = :i]],
        { s = exa.meta.script_schema, i = role_id })
    if #res > 0 then
        return res[1][1]
    else
        return nil
    end
end
--[[
/
--]]
