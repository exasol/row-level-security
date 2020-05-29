--[[
CREATE OR REPLACE SCRIPT EXA_IDENTIFIER AS
--]]
function validate(identifier)
  return ((identifier ~= null) and (identifier ~= nil) and (identifier ~= "") and string.find(identifier, '^[_%w]+$'))
end

function quote(string)
  if string == nil then
    return '<nil>'
  elseif string == null then
    return '<null>'
  else
    return '"' .. string .. '"'
  end
end

function assert_user_name(username)
  assert(validate(username),
    string.format("Invalid username %s. Must be a valid identifier (numbers, letters and underscores only).",
      quote(username)))
end
--[[
/
--]]
