package com.fizzed.jedis.ninja;

import com.fizzed.jedis.JedisFactory;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import ninja.lifecycle.Start;
import ninja.utils.NinjaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

@Singleton
public class NinjaJedisLifecycle {
    static private final Logger log = LoggerFactory.getLogger(NinjaJedisLifecycle.class);
    
    private final Boolean validateAtStart;
    private final Provider<JedisPool> jedisPoolProvider;
    
    @Inject 
    public NinjaJedisLifecycle(
            NinjaProperties ninjaProperties,
            Provider<JedisPool> jedisPoolProvider) {
        this.validateAtStart = ninjaProperties.getBooleanWithDefault("redis.validate_at_start", Boolean.TRUE);
        this.jedisPoolProvider = jedisPoolProvider;
    }

    @Start(order = 80)
    public void start() {
        if (!this.validateAtStart) {
            return;
        }

        final JedisPool jedisPool = this.jedisPoolProvider.get();
        JedisFactory.validateJedisPool(log, jedisPool);
    }
    
}