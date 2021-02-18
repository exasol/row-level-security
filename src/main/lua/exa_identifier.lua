--[[
CREATE OR REPLACE SCRIPT EXA_IDENTIFIER AS
--]]

---
-- Validate an Exasol identifier.
--
-- @param identifer identifier to be validated against the rules for being a valid Exasol identifier
--
-- @return <code>true</code> if the identifier is valid
--
function validate(identifier)
    return ((identifier ~= null) and (identifier ~= nil) and (identifier ~= "") and string.find(identifier, '^[_%w]+$'))
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
    assert(validate(user_name),
        string.format("The user name %s is not a valid identifier. Use numbers, letters and underscores only.",
            quote(user_name)))
end

function list_items(items)
    table.sort(items)
    return table.concat(items, ", ")
end

---
-- Assert that the given groups are all valid identifiers.
-- <p>
-- Note that the function only checks whether the are valid, not whether they exist!
-- </p>
--
-- @param user_name database user name
--
-- @return nothing if all user groups are valid identifiers, assertion error otherwise
--
function assert_groups(user_groups)
    local invalid_groups = {}
    for _, user_group in ipairs(user_groups) do
        if not validate(user_group) then
            invalid_groups[#invalid_groups + 1] = quote(user_group)
        end
    end
    assert(next(invalid_groups) == nil,
        string.format("The following group names are not valid identifiers: %s. Use numbers, letters and underscores only.",
            list_items(invalid_groups)))
end

---
-- Assert that the given roles are all valid identifiers.
-- <p>
-- Note that the function only checks whether the are valid, not whether they exist!
-- </p>
--
-- @param role_name RLS role name
--
-- @return nothing if all role names valid are identifiers, assertion error otherwise
--
function assert_roles(role_names)
    local invalid_roles = {}
    for _, role_name in ipairs(role_names) do
        if not validate(role_name) then
            invalid_roles[#invalid_roles + 1] = quote(role_name)
        end
    end
    assert(next(invalid_roles) == nil,
        string.format("The following role names are not valid identifiers: %s. Use numbers, letters and underscores only.",
            list_items(invalid_roles)))
end
--[[
/
--]]
