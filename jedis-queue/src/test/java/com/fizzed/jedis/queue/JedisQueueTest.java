package com.fizzed.jedis.queue;

import com.fizzed.jedis.queue.JedisQueue;
import com.fizzed.jedis.JedisCodecs;
import com.fizzed.jedis.JedisFactory;
import com.fizzed.crux.util.StackTraces;
import com.fizzed.crux.util.StopWatch;
import com.fizzed.crux.util.WaitFor;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisQueueTest {
    static private final Logger log = LoggerFactory.getLogger(JedisQueueTest.class);

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
        JedisQueue<String> queue = new JedisQueue<>(
            "test.queue", jedisPool, JedisCodecs.STRING_ENCODE, JedisCodecs.STRING_DECODE);

        for (int i = 0; i < 100; i++) {
            queue.push("" + i);
        }

        for (int j = 0; j < 100; j++) {
            assertEquals(queue.pop(-1, null), "" + j);
        }
    }
    
    @Test
    public void pushWithTtl() throws Exception {
        JedisQueue<String> queue = new JedisQueue<>(
            "test.queue." + UUID.randomUUID(), jedisPool, JedisCodecs.STRING_ENCODE, JedisCodecs.STRING_DECODE);

        queue.push("1", 1, TimeUnit.SECONDS);
        queue.push("2", 1, TimeUnit.SECONDS);
        
        // one item should pop
        String pop1 = queue.pop(0, null);
        assertThat(pop1, is("1"));
        
        // wait 1.5 seconds, the queue should now be empty
        Thread.sleep(1500L);
        
        String pop2 = queue.pop(0, null);
        assertThat(pop2, is(nullValue()));
    }
    
    @Test
    public void pushWithTtlExtendsTtlInRedis() throws Exception {
        JedisQueue<String> queue = new JedisQueue<>(
            "test.queue." + UUID.randomUUID(), jedisPool, JedisCodecs.STRING_ENCODE, JedisCodecs.STRING_DECODE);

        queue.push("1", 2, TimeUnit.SECONDS);
        queue.push("2", 2, TimeUnit.SECONDS);
        
        // wait 1 second, now the keys above have just 1 second left
        Thread.sleep(1000L);
        
        queue.push("3", 3, TimeUnit.SECONDS);
        
        // wait 2 seconds, now the keys above would definitel have expired
        // unless the 3rd push extended the ttl
        Thread.sleep(2000L);
        
        // one item should pop
        String pop1 = queue.pop(0, null);
        assertThat(pop1, is("1"));
    }
    
    @Test
    public void pushAndPopImmediately() throws Exception {
        JedisQueue<String> queue = new JedisQueue<>(
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
        JedisQueue<String> queue = new JedisQueue<>(
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
      
    @Test
    public void closingJedisPoolWillCloseSocketAndThrowInterruptedException() throws Exception {
        JedisQueue<String> queue = new JedisQueue<>(
            "test.queue", jedisPool, JedisCodecs.STRING_ENCODE, JedisCodecs.STRING_DECODE);
        
        AtomicReference<Thread> threadRef = new AtomicReference<>();
        CountDownLatch interruptedLatch = new CountDownLatch(1);
        CountDownLatch exitLatch = new CountDownLatch(1);
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> submit = executor.submit(() -> {
            threadRef.set(Thread.currentThread());
            try {
                String item = queue.pop(-1, null);
                fail("queue.take() should have failed");
            } catch (InterruptedException e) {
                // expected
                interruptedLatch.countDown();
            } catch (Exception e) {
                log.error("take()", e);
                fail("Expected an interrupt, not this exception");
            } finally {
                exitLatch.countDown();
            }
        });
        
        StopWatch timer = StopWatch.timeMillis();
        
        // wait for thread to be in the "pop" method
        WaitFor.of(() -> {
            return threadRef.get() != null
                && StackTraces.anyMatch(threadRef.get(), ste -> ste.getMethodName().equals("pop"));
        }).requireMillis(2000L, 10L);
            
        jedisPool.close();
        
        boolean interrupted = interruptedLatch.await(1000, TimeUnit.MILLISECONDS);
        boolean exited = exitLatch.await(1000, TimeUnit.MILLISECONDS);
        
        // queue.take() was not interrupted. Underlying jedis socket clearly not closed
        assertThat(interrupted, is(true));
        assertThat(exited, is(true));
        assertThat(submit.isDone(), is(true));
    }
    
    @Test
    public void whatHappensAfterPoolClose() throws Exception {
        JedisQueue<String> queue = new JedisQueue<>(
            "test.queue", jedisPool, JedisCodecs.STRING_ENCODE, JedisCodecs.STRING_DECODE);
        
        jedisPool.close();
        
        try {
            queue.pop(-1, null);
            fail("take() should failed");
        } catch (JedisConnectionException e) {
            // expected
        }
        
        try {
            queue.push("test1");
            fail("put() should failed");
        } catch (JedisConnectionException e) {
            // expected
        }
    } 

}
