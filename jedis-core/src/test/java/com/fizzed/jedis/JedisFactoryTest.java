package com.fizzed.jedis;

import java.util.Arrays;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import org.junit.After;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

public class JedisFactoryTest {
    static private final Logger log = LoggerFactory.getLogger(JedisFactoryTest.class);

    private JedisPool jedisPool;
    
    @Before
    public void before() throws Exception {
        this.jedisPool = JedisFactory.createJedisPool("redis://localhost:26379");
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.flushAll();
        }
    }
    
    @After
    public void after() throws Exception {
        this.jedisPool.close();
    }
    
    @Test
    public void validate() {
        try (Jedis jedis = jedisPool.getResource()) {
            assertThat(jedis.getDB(), is(0L));
        }
        
        assertThat(jedisPool.getNumIdle(), is(1));
        
        JedisFactory.validateJedisPool(log, jedisPool);
        
        assertThat(jedisPool.getNumIdle(), is(1));
    }
    
    @Test
    public void database() {
        this.jedisPool.close();
        this.jedisPool = JedisFactory.createJedisPool("redis://localhost:26379/2");
        
        JedisFactory.validateJedisPool(log, jedisPool);
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
            
            assertThat(jedis.getDB(), is(2L));
        }
    }
    
    @Test
    public void minPoolSize() {
        this.jedisPool.close();
        // configure pool with max of 1 and small wait millis
        this.jedisPool = JedisFactory.createJedisPool("redis://localhost:26379/1?pool_min_size=5");
        
        JedisFactory.validateJedisPool(log, jedisPool);
        
        // get 5 conns then give them up
        List<Jedis> resources = Arrays.asList(
            jedisPool.getResource(), jedisPool.getResource(), jedisPool.getResource(),
            jedisPool.getResource(), jedisPool.getResource());
        
        // close 'em all
        resources.forEach(Jedis::close);
        
        assertThat(this.jedisPool.getNumIdle(), is(5));
    }
    
    @Test
    public void waitTimeout() {
        this.jedisPool.close();
        // configure pool with max of 1 and small wait millis
        this.jedisPool = JedisFactory.createJedisPool("redis://localhost:26379/1?pool_max_size=1&pool_wait_timeout=100");
        
        JedisFactory.validateJedisPool(log, jedisPool);
        
        // get 1 resource
        try (Jedis jedis1 = jedisPool.getResource()) {
            // this should block and fail
            try (Jedis jedis2 = jedisPool.getResource()) {
                fail("should have failed with timeout");
            } catch (JedisException e) {
                // expected
            }
        }
    }

}
