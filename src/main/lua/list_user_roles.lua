--[[
CREATE OR REPLACE SCRIPT LIST_USER_ROLES(user_name) RETURNS TABLE AS
--]]
-- [impl->dsn~listing-user-roles~1]
exit(query([[
WITH USER_ROLES(USER_NAME, ROLE_ID) AS
(SELECT EXA_USER_NAME, BIT_POSITIONS(EXA_ROLE_MASK)
 FROM ::s.EXA_RLS_USERS)
SELECT u.user_name, r.ROLE_NAME FROM USER_ROLES u LEFT OUTER JOIN ::s.EXA_ROLES_MAPPING r ON CAST(u.ROLE_ID AS DECIMAL(20,0)) = r.ROLE_ID
WHERE u.USER_NAME = :username
]],
    { s = exa.meta.script_schema, username = user_name }))
--[[
/
--]]