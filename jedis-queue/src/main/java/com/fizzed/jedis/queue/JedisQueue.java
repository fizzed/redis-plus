package com.fizzed.jedis.queue;

import com.fizzed.queue.AbstractQueue;
import com.fizzed.jedis.JedisClients;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Implement of a redis-backed queue.
 * 
 * @author jjlauer
 * @param <E> 
 */
public class JedisQueue<E> extends AbstractQueue<E> {
//    static private final Logger log = LoggerFactory.getLogger(JedisQueue.class);

    protected final JedisPool jedisPool;
    protected final byte[] key;
    protected final Function<E,byte[]> encode;
    protected final Function<byte[],E> decode;
    
    public JedisQueue(String name, JedisPool jedisPool, Function<E,byte[]> encode, Function<byte[],E> decode) {
        super(name);
        this.jedisPool = jedisPool;
        this.key = name.getBytes(StandardCharsets.UTF_8);
        this.encode = encode;
        this.decode = decode;
    }

    public JedisPool getJedisPool() {
        return this.jedisPool;
    }
    
    @Override
    public void push(E e) throws InterruptedException {
        // push a value on and do not set the ttl
        this.push(e, -1, TimeUnit.SECONDS);
    }
    
    /**
     * Push an item onto the queue.  Optionally set a time-to-live (expiration)
     * on the item where this "key" be deleted after this amount of time. New items
     * that are pushed on help bump up the time-to-live if items exist on the queue.
     * @param e The item to push onto the queue
     * @param ttl If greater than 0 then the amount of expiration to set on this key
     *      or 0 or -1 for no expiration.
     * @param unit The unit of the ttl
     * @throws InterruptedException 
     */
    public void push(E e, long ttl, TimeUnit unit) throws InterruptedException {
        this.checkNotClosed();
        byte[] bytes = this.encode.apply(e);
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.rpush(key, bytes);
            if (ttl > 0) {
                int seconds = (int)TimeUnit.SECONDS.convert(ttl, unit);
                if (seconds < 1) {
                    seconds = 1;    // 1-second minimum
                }
                jedis.expire(key, seconds);
            }
        }
    }

    @Override
    public E pop(long timeout, TimeUnit unit) throws InterruptedException {
        this.checkNotClosed();
        byte[] bytes = doPop(timeout, unit);
        if (bytes != null) {
            return this.decode.apply(bytes);
        }
        return null;
    }
    
    protected byte[] doPop(long timeout, TimeUnit unit) throws InterruptedException {
        if (timeout > 0) {
            Objects.requireNonNull(unit, "unit was null");
        }
        if (timeout <= 0 && unit == null) {
            unit = TimeUnit.MILLISECONDS;
        }
        //log.debug("Acquiring jedis resource...");
        try (Jedis jedis = this.jedisPool.getResource()) {
            //log.debug("Acquired jedis resource...");
            try {
                if (timeout == 0) {
                    // do not block - pop and immediately return null or element
                    return jedis.lpop(key);
                } else if (timeout < 0) {
                    // block forever - but only poll every X millis so we can be interrupted
                    return JedisClients.blpop(
                        jedis, key, -1, 1000L, TimeUnit.MILLISECONDS);
                } else {
                    // block but be interruptible (round up at least 1000ms since
                    // jedis client does not allow anything under 1 sec)
                    long timeoutMillis = Math.max(unit.toMillis(timeout), 1000L);
                    return JedisClients.blpop(
                        jedis, key, timeoutMillis, 1000L, TimeUnit.MILLISECONDS);
                }
            } catch (TimeoutException e) {
                return null;
            } catch (JedisException e) {
                if (this.jedisPool.isClosed()) {
                    throw new InterruptedException("jedis pool is closed");
                }
                throw e;
            }
        }
    }
}