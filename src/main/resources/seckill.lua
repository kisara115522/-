-- 先定义参数
local voucherId=ARGV[1]
local userId=ARGV[2]
--redis中的键
local stockKey="seckill:stock:"..voucherId
local orderKey="seckill:order:"..voucherId
--如果库存不足，返回1
if(tonumber(redis.call("get",stockKey))<=0) then
	return 1
end
--如果用户已下单，返回2
if(redis.call("sismember",orderKey,userId)==1) then
	return 2
end
--如果都不成立，减库存，将用户加入set 返回0
redis.call("incrby",stockKey,-1)
redis.call("sadd",orderKey,userId)
return 0