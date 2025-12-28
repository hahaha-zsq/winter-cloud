package com.winter.cloud.gateway;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.auth.api.dto.response.ValidateTokenDTO;
import com.zsq.winter.redis.ddc.service.WinterRedissionTemplate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 用户缓存测试类
 * 
 * 用于测试用户信息在 Redis 中的正确存储和读取
 */
@Slf4j
@SpringBootTest
public class RedisUserCacheTest {

    @Autowired
    private WinterRedissionTemplate stringRedisTemplate;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 测试使用 StringRedisTemplate 设置用户缓存
     * 这是推荐的方式，确保存储的是纯 JSON 字符串
     */
    @Test
    public void testSetUserCacheWithStringTemplate() throws Exception {
        log.info("=== 测试使用 StringRedisTemplate 设置用户缓存 ===");
        
        // 创建用户信息对象
        ObjectMapper objectMapper = new ObjectMapper();
        ValidateTokenDTO result = new ValidateTokenDTO();
        result.setValid(true);
        result.setUserId(12345L);
        result.setRoles(CollUtil.toList("role1", "role2"));
        result.setPermissions(CollUtil.toList("permission1", "permission2"));
        result.setUserName("userName");
        result.setMessage("Token 有效");
        
        // 序列化为 JSON 字符串
        String jsonString = objectMapper.writeValueAsString(result);
        log.info("序列化的 JSON 字符串: {}", jsonString);
        
        // 设置缓存键
        String cacheKey = "winter-cloud-userInfo:12345";
        
        // 存储到 Redis（设置1小时过期）
//        stringRedisTemplate.opsForValue().set(cacheKey, jsonString, Duration.ofHours(1));
        stringRedisTemplate.set(cacheKey, jsonString, 1, TimeUnit.HOURS);
        log.info("用户信息已存储到 Redis，Key: {}", cacheKey);
        
        // 验证存储结果
        Object retrievedJson = stringRedisTemplate.get(cacheKey);
//        String retrievedJson = stringRedisTemplate.opsForValue().get(cacheKey);
        log.info("从 Redis 获取的 JSON: {}", retrievedJson);
        
        // 验证反序列化
        ValidateTokenDTO parsedResult = objectMapper.readValue(retrievedJson.toString(), ValidateTokenDTO.class);
        log.info("反序列化成功: userId={}, userName={}, valid={}", 
                parsedResult.getUserId(), parsedResult.getUserName(), parsedResult.getValid());
        
        // 检查过期时间
//        Long ttl = stringRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        long ttl = stringRedisTemplate.getExpire(cacheKey);
//        Long ttl = stringRedisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        log.info("缓存过期时间: {} 秒", ttl);
    }
//
//    /**
//     * 批量设置多个用户的缓存信息
//     */
//    @Test
//    public void testBatchSetUserCache() throws Exception {
//        log.info("=== 测试批量设置用户缓存 ===");
//
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        // 创建多个测试用户
//        Long[] userIds = {12345L, 67890L, 11111L, 22222L};
//        String[] userNames = {"john_doe", "jane_smith", "admin_user", "test_user"};
//        String[][] roles = {
//            {"user"},
//            {"user", "vip"},
//            {"admin", "super_admin"},
//            {"user", "tester"}
//        };
//        String[][] permissions = {
//            {"user:read"},
//            {"user:read", "user:write"},
//            {"admin:read", "admin:write", "system:manage"},
//            {"user:read", "test:execute"}
//        };
//
//        for (int i = 0; i < userIds.length; i++) {
//            // 创建用户信息
//            ValidateTokenDTO userInfo = new ValidateTokenDTO();
//            userInfo.setValid(true);
//            userInfo.setUserId(userIds[i]);
//            userInfo.setUserName(userNames[i]);
//            userInfo.setRoles(CollUtil.toList(roles[i]));
//            userInfo.setPermissions(CollUtil.toList(permissions[i]));
//            userInfo.setMessage("Token 有效");
//
//            // 序列化并存储
//            String jsonString = objectMapper.writeValueAsString(userInfo);
//            String cacheKey = "winter-cloud-userInfo:" + userIds[i];
//
//            // 不同用户设置不同的过期时间（模拟实际场景）
//            Duration expiration = Duration.ofMinutes(30 + i * 10);
//            stringRedisTemplate.opsForValue().set(cacheKey, jsonString, expiration);
//
//            log.info("用户 {} 缓存已设置，过期时间: {} 分钟", userIds[i], expiration.toMinutes());
//        }
//
//        log.info("批量设置完成，共设置 {} 个用户缓存", userIds.length);
//    }
//
//    /**
//     * 测试设置无效用户缓存（用于测试认证失败场景）
//     */
//    @Test
//    public void testSetInvalidUserCache() throws Exception {
//        log.info("=== 测试设置无效用户缓存 ===");
//
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        // 创建无效用户信息
//        ValidateTokenDTO invalidUser = new ValidateTokenDTO();
//        invalidUser.setValid(false);
//        invalidUser.setUserId(99999L);
//        invalidUser.setUserName("disabled_user");
//        invalidUser.setRoles(CollUtil.newArrayList()); // 空角色
//        invalidUser.setPermissions(CollUtil.newArrayList()); // 空权限
//        invalidUser.setMessage("用户已被禁用");
//
//        String jsonString = objectMapper.writeValueAsString(invalidUser);
//        String cacheKey = "winter-cloud-userInfo:99999";
//
//        // 无效用户缓存时间较短（5分钟）
//        stringRedisTemplate.opsForValue().set(cacheKey, jsonString, Duration.ofMinutes(5));
//
//        log.info("无效用户缓存已设置: {}", jsonString);
//
//        // 验证
//        String retrieved = stringRedisTemplate.opsForValue().get(cacheKey);
//        ValidateTokenDTO parsed = objectMapper.readValue(retrieved, ValidateTokenDTO.class);
//        log.info("验证无效用户: valid={}, message={}", parsed.getValid(), parsed.getMessage());
//    }
//
//    /**
//     * 测试使用 RedisTemplate 设置缓存（如果配置了 Jackson 序列化器）
//     */
//    @Test
//    public void testSetUserCacheWithRedisTemplate() throws Exception {
//        if (redisTemplate == null) {
//            log.warn("RedisTemplate 未配置，跳过此测试");
//            return;
//        }
//
//        log.info("=== 测试使用 RedisTemplate 设置用户缓存 ===");
//
//        // 创建用户信息对象
//        ValidateTokenDTO result = new ValidateTokenDTO();
//        result.setValid(true);
//        result.setUserId(33333L);
//        result.setRoles(CollUtil.toList("role1", "role2"));
//        result.setPermissions(CollUtil.toList("permission1", "permission2"));
//        result.setUserName("redisTemplate_user");
//        result.setMessage("Token 有效");
//
//        String cacheKey = "winter-cloud-userInfo:33333";
//
//        // 直接存储对象（依赖 Jackson 序列化器）
//        redisTemplate.opsForValue().set(cacheKey, result, Duration.ofHours(1));
//        log.info("使用 RedisTemplate 存储用户信息完成");
//
//        // 获取并验证
//        Object retrieved = redisTemplate.opsForValue().get(cacheKey);
//        log.info("获取的数据类型: {}", retrieved.getClass().getName());
//        log.info("获取的数据: {}", retrieved);
//
//        // 如果是字符串，尝试反序列化
//        if (retrieved instanceof String) {
//            ObjectMapper objectMapper = new ObjectMapper();
//            ValidateTokenDTO parsed = objectMapper.readValue((String) retrieved, ValidateTokenDTO.class);
//            log.info("反序列化成功: userId={}, userName={}", parsed.getUserId(), parsed.getUserName());
//        }
//    }
//
//    /**
//     * 查看当前 Redis 中的所有用户缓存
//     */
//    @Test
//    public void testViewAllUserCache() throws Exception {
//        log.info("=== 查看当前 Redis 中的所有用户缓存 ===");
//
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        // 获取所有用户缓存键
//        var keys = stringRedisTemplate.keys("winter-cloud-userInfo:*");
//
//        if (keys == null || keys.isEmpty()) {
//            log.info("Redis 中没有找到用户缓存数据");
//            return;
//        }
//
//        log.info("找到 {} 个用户缓存", keys.size());
//
//        for (String key : keys) {
//            String jsonData = stringRedisTemplate.opsForValue().get(key);
//            Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
//
//            try {
//                ValidateTokenDTO userInfo = objectMapper.readValue(jsonData, ValidateTokenDTO.class);
//                log.info("Key: {}, UserId: {}, UserName: {}, Valid: {}, TTL: {}s",
//                        key, userInfo.getUserId(), userInfo.getUserName(),
//                        userInfo.getValid(), ttl);
//            } catch (Exception e) {
//                log.error("解析用户缓存失败，Key: {}, Data: {}, Error: {}",
//                        key, jsonData, e.getMessage());
//            }
//        }
//    }
//
//    /**
//     * 清理所有测试缓存数据
//     */
//    @Test
//    public void testCleanupTestCache() {
//        log.info("=== 清理测试缓存数据 ===");
//
//        var keys = stringRedisTemplate.keys("winter-cloud-userInfo:*");
//
//        if (keys != null && !keys.isEmpty()) {
//            Long deletedCount = stringRedisTemplate.delete(keys);
//            log.info("已删除 {} 个缓存键", deletedCount);
//        } else {
//            log.info("没有找到需要清理的缓存数据");
//        }
//    }
}