-- [dsn~add-group-member-adds-a-user-to-a-group~1]
--[[
CREATE OR REPLACE SCRIPT ADD_USER_TO_GROUP(user_name, array user_groups) AS
--]]
import(exa.meta.script_schema .. '.EXA_IDENTIFIER', 'identifier')

function validate_groups()
  local invalid_groups = {}
  for i=1, #user_groups do
    if(not identifier.validate(user_groups[i])) then
      invalid_groups[#invalid_groups+1]=identifier.quote(user_groups[i])
    end
  end
  return (#invalid_groups == 0), invalid_groups
end

function create_temporary_member_table()
  query("CREATE OR REPLACE TABLE ::s.EXA_NEW_GROUP_MEMBERS (EXA_USER_NAME VARCHAR(128), EXA_GROUP VARCHAR(128))",
    { s = exa.meta.script_schema })
end

function populate_temporary_member_table()
  for i = 1, #user_groups do
    query("INSERT INTO ::s.EXA_NEW_GROUP_MEMBERS (EXA_USER_NAME, EXA_GROUP) VALUES (:u, :g)",
      { s = exa.meta.script_schema, u = user_name, g = user_groups[i] })
  end
end

function create_member_table_if_not_exists()
  query("CREATE TABLE IF NOT EXISTS ::s.EXA_GROUP_MEMBERS (EXA_USER_NAME VARCHAR(128), EXA_GROUP VARCHAR(128))",
    { s = exa.meta.script_schema })
end

function merge_new_members()
  query([[MERGE INTO ::s.EXA_GROUP_MEMBERS M
                      USING EXA_NEW_GROUP_MEMBERS N
                      ON M.EXA_USER_NAME = N.EXA_USER_NAME
                      WHEN NOT MATCHED THEN INSERT VALUES (N.EXA_USER_NAME, N.EXA_GROUP)]],
  { s = exa.meta.script_schema })
end

function drop_temporary_member_table()
  query("DROP TABLE ::s.EXA_NEW_GROUP_MEMBERS", { s = exa.meta.script_schema })
end

identifier.assert_user_name(user_name)
local groups_valid, invalid_groups = validate_groups()
assert(groups_valid,
  string.format("Groups found that are not valid identifiers (numbers, letters and underscores only): %s",
    table.concat(invalid_groups, ", ")))

create_temporary_member_table()
populate_temporary_member_table()
create_member_table_if_not_exists()
merge_new_members()
drop_temporary_member_table()
--[[
/
--]]
