CREATE OR REPLACE LUA SET SCRIPT roles_mask(role_id int) RETURNS INT AS
function run(ctx)
   roles_mask = 0
   repeat roles_mask = roles_mask + math.pow(2, ctx.role_id:tonumber()-1) until not ctx.next()
   return decimal(roles_mask)
end
/