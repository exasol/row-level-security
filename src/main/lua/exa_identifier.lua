--[[
CREATE OR REPLACE SCRIPT EXA_IDENTIFIER AS
--]]

---
-- Validate an Exasol identifier.
-- 
-- @param identifer identifier to be validated against the rules for being a valid Exasol identifer
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
    string.format("Invalid username %s. Must be a valid identifier (numbers, letters and underscores only).",
      quote(user_name)))
end

---
-- Assert that the listed groups ara all valid identifiers.
--
-- @param user_name database user name
-- 
-- @return nothing if all user groups valid identifier, assertion error otherwise
--
function assert_groups(user_groups)
    local invalid_groups = {}
    for i = 1, #user_groups do
        if(not validate(user_groups[i])) then
            invalid_groups[#invalid_groups + 1] = quote(user_groups[i])
        end
    end
    assert(next(invalid_groups) == nil,
            string.format("Groups found that are not valid identifiers (numbers, letters and underscores only): %s",
            table.concat(invalid_groups, ", ")))
end
--[[
/
--]]
