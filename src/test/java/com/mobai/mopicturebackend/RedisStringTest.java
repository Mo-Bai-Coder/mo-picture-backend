package com.mobai.mopicturebackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
public class RedisStringTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Test
    public void testRedisStringOperations() {
        // 获取操作对象
        ValueOperations<String,String> valueOperations = stringRedisTemplate.opsForValue();

        //Key 和 Value
        String key = "First-key";

        String value = "First-value";

        //1.测试新增或更新操作
        valueOperations.set(key,value);

        String storedValue = valueOperations.get(key);

        assertEquals(value,storedValue,"存储的值与预期的值不一致");

        //2.测试修改
        String updatedValue = "Second-Value";

        valueOperations.set(key,updatedValue);

        String storedV2Value = valueOperations.get(key);

        assertEquals(updatedValue,storedV2Value,"修改后的值与预期的值不一致");

        //3.测试删除
        stringRedisTemplate.delete(key);
        String deletedValue = valueOperations.get(key);
        assertNull(deletedValue,"删除后的值不为空");





    }
}
