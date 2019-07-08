package com.ding.miaosha.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


@Service
public class RedisPoolFactory {
    @Autowired
    RedisConf redisConf;
    @Bean
    public JedisPool JedisFactory(){
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(redisConf.getPoolMaxIdle());
        poolConfig.setMaxTotal(redisConf.getPoolMaxTotal());
        poolConfig.setMaxWaitMillis(redisConf.getPoolMaxWait() * 1000);
        JedisPool jp = new JedisPool(poolConfig,redisConf.getHost(),redisConf.getPort(),
                redisConf.getTimeout()*1000,redisConf.getPassword(),0);
        return jp;
    }
}
