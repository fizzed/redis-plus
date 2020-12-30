package com.fizzed.jedis;

import com.fizzed.crux.uri.Uri;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class JedisFactory {
    
    static public JedisPool createJedisPool(String uri) {
        return createJedisPool(new Uri(uri));
    }
    
    static public JedisPool createJedisPool(Uri uri) {
        return createJedisPool(new JedisOptions().setUri(uri));
    }
    
    static public JedisPool createJedisPool(JedisOptions options) {
        Objects.requireNonNull(options, "jedis options was null");
        Objects.requireNonNull(options.getHost(), "jedis options.host was null");
        Objects.requireNonNull(options.getPort(), "jedis options.port was null");
        Objects.requireNonNull(options.getConnectTimeout(), "jedis options.connect_timeout was null");
        
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        
        poolConfig.setMinIdle(options.getPoolMinSize());
        poolConfig.setMaxTotal(options.getPoolMaxSize());
        poolConfig.setMaxWaitMillis(options.getPoolWaitTimeout());
        
        // convert millis timeout to seconds
        int connectionTimeout = (int)TimeUnit.MILLISECONDS.convert(
            options.getConnectTimeout(), TimeUnit.SECONDS);
        
        return new DefaultJedisPool(poolConfig, options.getHost(), options.getPort(),
            connectionTimeout, options.getPassword(), options.getDatabase());
    }
    
    static public void validateJedisPool(Logger log, JedisPool jedisPool) {
        Uri redisUri = null;
        
        if (jedisPool instanceof DefaultJedisPool) {
            redisUri = ((DefaultJedisPool)jedisPool).getUri();
        }
        
        log.info("Connecting {}...", (redisUri != null ? redisUri : ""));
        
        try (Jedis resource = jedisPool.getResource()) {
            String redisVersion = null;
            String redisInfo = resource.info("server");
            if (redisInfo != null && redisInfo.contains("redis_version:")) {
                int startPos = redisInfo.indexOf("redis_version:");
                int endPos = redisInfo.indexOf("\n", startPos);
                redisVersion = redisInfo.substring(startPos+14, endPos);
            }
            log.info("Connected to redis {}", redisVersion);
        }
    }
    
}
