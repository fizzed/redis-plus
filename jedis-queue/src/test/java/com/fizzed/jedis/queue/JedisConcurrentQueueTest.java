package com.fizzed.jedis.queue;

import com.fizzed.jedis.queue.JedisConcurrentQueue;
import com.fizzed.jedis.JedisCodecs;
import com.fizzed.jedis.JedisFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class JedisConcurrentQueueTest {
    static private final Logger log = LoggerFactory.getLogger(JedisConcurrentQueueTest.class);

    private JedisPool jedisPool;
    
    @Before
    public void before() throws Exception {
        jedisPool = JedisFactory.createJedisPool("redis://localhost:26379");
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
        }
    }
    
    @Test
    public void pushAndPopForever() throws Exception {
        JedisConcurrentQueue<String> queue = new JedisConcurrentQueue<>(
            "test.queue", jedisPool, JedisCodecs.STRING_ENCODE, JedisCodecs.STRING_DECODE);

        for (int i = 0; i < 100; i++) {
            queue.push("" + i);
        }

        for (int j = 0; j < 100; j++) {
            assertEquals(queue.pop(-1, null), "" + j);
        }
    }
    
    @Test
    public void pushAndPopImmediately() throws Exception {
        JedisConcurrentQueue<String> queue = new JedisConcurrentQueue<>(
            "test.queue", jedisPool, JedisCodecs.STRING_ENCODE, JedisCodecs.STRING_DECODE);

        String item;
        
        item = queue.pop(0, null);
        assertThat(item, is(nullValue()));
        
        queue.push("test1");
        item = queue.pop(0, null);
        assertThat(item, is("test1"));
        
        item = queue.pop(0, null);
        assertThat(item, is(nullValue()));
    }
    
    @Test
    public void pushAndPopWaitable() throws Exception {
        JedisConcurrentQueue<String> queue = new JedisConcurrentQueue<>(
            "test.queue", jedisPool, JedisCodecs.STRING_ENCODE, JedisCodecs.STRING_DECODE);

        String item;
        
        item = queue.pop(1, TimeUnit.SECONDS);
        assertThat(item, is(nullValue()));
        
        queue.push("test1");
        item = queue.pop(1, TimeUnit.SECONDS);
        assertThat(item, is("test1"));
        
        item = queue.pop(1, TimeUnit.SECONDS);
        assertThat(item, is(nullValue()));
    }
    
    public void concurrencyWithPermits(int threads, int permits) throws Exception {
        final JedisConcurrentQueue<String> queue = new JedisConcurrentQueue<>(
            "test.queue", jedisPool, JedisCodecs.STRING_ENCODE, JedisCodecs.STRING_DECODE, permits);

        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger popped = new AtomicInteger();
        
        ExecutorService executors = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            executors.submit(() -> {
                try {
                    String item;
                    do {
                        item = queue.pop(1L, TimeUnit.SECONDS);
                        if (item != null) {
                            //log.debug("item received {}", item);
                            popped.incrementAndGet();
                        } else {
                            //log.debug("item was null");
                        }
                    } while (item != null);
                } catch (InterruptedException e) {
                    fail("interrupted thread");
                } catch (Throwable t) {
                    log.error("", t);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // push 100 items now
        for (int i = 0; i < 100; i++) {
            queue.push("" + i);
        }
        
        if (!latch.await(10000L, TimeUnit.MILLISECONDS)) {
            fail("threads failed to finished im time");
        }
        
        //executors.shutdown();
        
        assertThat(popped.get(), is(100));
        assertThat(jedisPool.getNumIdle(), greaterThan(permits));
    }
    
    @Test
    public void concurrencyWith1Permit() throws Exception {
        this.concurrencyWithPermits(5, 1);
    }
    
    @Test
    public void concurrencyWith3Permits() throws Exception {
        this.concurrencyWithPermits(10, 3);
    }

}
