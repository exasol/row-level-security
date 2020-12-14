-- [impl->dsn~get-a-role-mask~1]
--[[
CREATE OR REPLACE LUA SCALAR SCRIPT BIT_POSITIONS(num DOUBLE) EMITS(pos DOUBLE) AS
--]]
function run(ctx)
    num = ctx.num
    index = 1
    while num > 0 do
        rest = math.fmod(num, 2)
        if rest == 1 then
            ctx.emit(index)
        end
        num = (num - rest) / 2
        index = index + 1
    end
end
--[[
/
--]]