package com.fizzed.jedis;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisClientsTest {

    private JedisPool jedisPool;
    
    @Before
    public void before() throws Exception {
        jedisPool = JedisFactory.createJedisPool("redis://localhost:26379");
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
        }
    }
    
    @Test
    public void interruptibleBlpopItemPopped() throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.rpush("test".getBytes(), "value1".getBytes());

            byte[] value = JedisClients.blpop(jedis, "test".getBytes(), 1000L, 200L, TimeUnit.MILLISECONDS);
            
            assertThat(value, is("value1".getBytes()));
        }
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void interruptibleBlpopPollingTimeoutGreaterThanOperationTimeout() throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            byte[] value = JedisClients.blpop(jedis, "test".getBytes(), 1000L, 1001L, TimeUnit.MILLISECONDS);
        }
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void interruptibleBlpopInvalidPollingTimeout() throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            byte[] value = JedisClients.blpop(jedis, "test".getBytes(), 1000L, 1001L, TimeUnit.MILLISECONDS);
        }
    }
    
    @Test
    public void interruptibleBlpopTimeoutException() throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            try {
                JedisClients.blpop(jedis, "test".getBytes(), 1000L, 200L, TimeUnit.MILLISECONDS);
                fail("expected timeout");
            } catch (TimeoutException e) {
                // expected
            }
        }
    }
    
    @Test
    public void interruptibleBlpopInterruptedException() throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            // spin up another thread that will block
            AtomicReference<Exception> exceptionRef = new AtomicReference<>();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> task = executor.submit(() -> {
                try {
                    // block forever potentially
                    JedisClients.blpop(jedis, "test".getBytes(), 0L, 1000L, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    exceptionRef.set(e);
                }
            });
            
            // wait a bit to make sure we're waiting on redis
            Thread.sleep(500L);
            
            // interrupt task
            task.cancel(true);
            
            executor.shutdown();
            boolean terminated = executor.awaitTermination(5000L, TimeUnit.MILLISECONDS);
            
            // termination should have been clean and the task should have thrown a specific exception
            assertThat(terminated, is(true));
            assertThat(exceptionRef.get(), instanceOf(InterruptedException.class));
        }
    }
    
    @Test
    public void clientClose() throws Exception {
        try (Jedis jedis = this.jedisPool.getResource()) {
            // spin up another thread that will block
            AtomicReference<Exception> exceptionRef = new AtomicReference<>();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> task = executor.submit(() -> {
                try {
                    // block forever potentially
                    JedisClients.blpop(jedis, "test".getBytes(), 0L, 1000L, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    exceptionRef.set(e);
                }
            });
            
            // wait a bit to make sure we're waiting on redis
            Thread.sleep(500L);
            
            // close the client
            jedis.getClient().close();
            
            executor.shutdown();
            boolean terminated = executor.awaitTermination(5000L, TimeUnit.MILLISECONDS);
            
            // termination should have been clean and the task should have thrown a specific exception
            assertThat(terminated, is(true));
            assertThat(exceptionRef.get(), instanceOf(JedisConnectionException.class));
        }
    }
    
}
