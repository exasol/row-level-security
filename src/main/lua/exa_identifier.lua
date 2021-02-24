--[[
CREATE OR REPLACE SCRIPT EXA_IDENTIFIER AS
--]]

local ALLOWED_IDENTIFIER = "Allowed identifiers are ASCII only, starting with a letter. " ..
    "Optionally followed by letters, numbers or underscores. Up to 128 characters."

---
-- Validate an Exasol identifier.
-- <p>
-- Note that string parameters passed to Exasol scripts can never be empty. Exasol replaces empty <code>VARCHAR</code>
-- values by <code>NULL</code>. So if an identifier is stored or passed as as <code>VARCHAR</code> this rule applies.
-- </p>
--
-- @param identifer identifier to be validated against the rules for being a valid Exasol identifier
--
-- @return <code>true</code> if the identifier is valid. Otherwise return false and a quoted identifier for use in an
-- error message
--
function validate(identifier)
    if(identifier == nil) then
        return false, "<nil>"
    elseif (identifier == null) then
        return false, "<null>"
    elseif (identifier == "") then
        return false, "<empty>"
    elseif (string.len(identifier) > 128) or not string.find(identifier, '^[A-Za-z][_%w]*$') then
        return false, '"' .. identifier .. '"'
    else
        return true
    end
end

---
-- Add quotes around a string and display <code>nil</code> and <code>null</code> in a clear fashion.
--
-- @param string string to be quoted
--
-- @return quoted string
--
function quote(string)
    if string == nil then
        return '<nil>'
    elseif string == null then
        return '<null>'
    elseif string == "" then
        return '<empty>'
    else
        return '"' .. string .. '"'
    end
end

---
-- Assert that a user name is a valid identifier.
--
-- @param user_name database user name
--
-- @return nothing if the user name is a valid identifier, assertion error otherwise
--
function assert_user_name(user_name)
    local valid, quoted_user_name = validate(user_name)
    if not valid then
        error("The user name " .. quoted_user_name .. " is invalid. " .. ALLOWED_IDENTIFIER)
    end
end

local function list_items(items)
    table.sort(items)
    return table.concat(items, ", ")
end

---
-- Assert that the given groups are all valid identifiers.
-- <p>
-- Note that the function only checks whether the names are valid, not whether they exist!
-- </p>
--
-- @param user_group_names RLS groups to be checked
--
-- @return nothing if all user groups are valid identifiers, assertion error otherwise
--
function assert_groups(user_group_names)
    local invalid_group_names = {}
    for _, user_group_name in ipairs(user_group_names) do
        local valid, quoted_user_group_name = validate(user_group_name)
        if not valid then
            invalid_group_names[#invalid_group_names + 1] = quoted_user_group_name
        end
    end
    if next(invalid_group_names) ~= nil then
        error("The following group names are invalid: " .. list_items(invalid_group_names) .. ". "
            .. ALLOWED_IDENTIFIER)
    end
end

---
-- Assert that the given roles are all valid identifiers.
-- <p>
-- Note that the function only checks whether the names are valid, not whether they exist!
-- </p>
--
-- @param role_names RLS role names to be checked
--
-- @return nothing if all role names are valid identifiers, assertion error otherwise
--
function assert_roles(role_names)
    local invalid_role_names = {}
    for _, role_name in ipairs(role_names) do
        local valid, quoted_role_name = validate(role_name)
        if not valid then
            invalid_role_names[#invalid_role_names + 1] = quoted_role_name
        end
    end
    if next(invalid_role_names) ~= nil then
        error("The following role names are invalid: " .. list_items(invalid_role_names) .. ". " .. ALLOWED_IDENTIFIER)
    end
end
--[[
/
--]]
