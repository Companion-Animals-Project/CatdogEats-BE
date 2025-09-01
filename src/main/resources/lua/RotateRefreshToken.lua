local tokenKey = KEYS[1]
local currentTime = tonumber(ARGV[1])

if not currentTime then
    return cjson.encode({code = -99, provider = '', providerId = '', userId = '', message = 'INVALID_CURRENT_TIME'})
end

-- 토큰 존재 확인
if redis.call('EXISTS', tokenKey) == 0 then
    return cjson.encode({code = -1, provider = '', providerId = '', userId = '', message = 'TOKEN_NOT_FOUND'})
end

-- 토큰 정보 조회
local tokenData = redis.call('HGETALL', tokenKey)
if #tokenData == 0 then
    redis.call('DEL', tokenKey)
    return cjson.encode({code = -1, provider = '', providerId = '', userId = '', message = 'TOKEN_NOT_FOUND'})
end

local tokenMap = {}
for i = 1, #tokenData, 2 do
    tokenMap[tokenData[i]] = tokenData[i + 1]
end

local userId = tokenMap['userId'] or ''
local expiresAtStr = tokenMap['expiresAt']
local expiresAt = nil
if expiresAtStr then
    expiresAt = tonumber(expiresAtStr)
end

-- expiresAt 존재/파싱 확인
if not expiresAt then
    if userId ~= '' then
        local userTokensSetKey = 'userTokens:' .. userId
        local tokenIds = redis.call('ZRANGE', userTokensSetKey, 0, -1)
        for _, tid in ipairs(tokenIds) do
            redis.call('DEL', 'refreshToken:' .. tid)
        end
        redis.call('DEL', userTokensSetKey)
    end
    redis.call('DEL', tokenKey)
    return cjson.encode({code = -2, provider = '', providerId = '', userId = '', message = 'TOKEN_EXPIRED_OR_INVALID'})
end

-- 만료 체크
if expiresAt < currentTime then
    if userId ~= '' then
        local userTokensSetKey = 'userTokens:' .. userId
        local tokenIds = redis.call('ZRANGE', userTokensSetKey, 0, -1)
        for _, tid in ipairs(tokenIds) do
            redis.call('DEL', 'refreshToken:' .. tid)
        end
        redis.call('DEL', userTokensSetKey)
    end
    redis.call('DEL', tokenKey)
    return cjson.encode({code = -2, provider = '', providerId = '', userId = '', message = 'TOKEN_EXPIRED'})
end

-- 사용 여부 검사
local used = tokenMap['used'] or 'false'
if used == 'true' then
    if userId ~= '' then
        local userTokensSetKey = 'userTokens:' .. userId
        local tokenIds = redis.call('ZRANGE', userTokensSetKey, 0, -1)
        for _, tid in ipairs(tokenIds) do
            redis.call('DEL', 'refreshToken:' .. tid)
        end
        redis.call('DEL', userTokensSetKey)
    end
    redis.call('DEL', tokenKey)
    return cjson.encode({code = -3, provider = '', providerId = '', userId = '', message = 'TOKEN_REUSE_DETECTED'})
end

-- 사용 처리
redis.call('HSET', tokenKey, 'used', 'true')

-- 성공 반환 (일관된 구조)
return cjson.encode({
    code = 1,
    provider = tokenMap['provider'] or '',
    providerId = tokenMap['providerId'] or '',
    userId = userId,
    message = 'SUCCESS'
})