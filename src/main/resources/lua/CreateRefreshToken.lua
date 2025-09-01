local userId = KEYS[1]
local tokenId = ARGV[1]
local provider = ARGV[2]
local providerId = ARGV[3]
local createdAt = tonumber(ARGV[4])
local expiresAt = tonumber(ARGV[5])
local maxTokens = tonumber(ARGV[6])

if not createdAt or not expiresAt or not maxTokens then
    return redis.error_reply("Invalid numeric argument")
end

local tokenKey = "refreshToken:" .. tokenId
local userTokensSetKey = "userTokens:" .. userId

redis.call("HMSET", tokenKey,
    "id", tokenId,
    "provider", provider,
    "providerId", providerId,
    "userId", userId,
    "used", "false",
    "createdAt", createdAt,
    "expiresAt", expiresAt
)

redis.call("EXPIRE", tokenKey, 86400)

redis.call("ZADD", userTokensSetKey, createdAt, tokenId)

redis.call("EXPIRE", userTokensSetKey, 86400)

local currentTokenCount = redis.call("ZCARD", userTokensSetKey)

if currentTokenCount > maxTokens then
    local oldestTokenId = redis.call("ZPOPMIN", userTokensSetKey, 1)
    if oldestTokenId and oldestTokenId[1] then
        redis.call("DEL", "refreshToken:" .. oldestTokenId[1])
    end
end

return tokenId