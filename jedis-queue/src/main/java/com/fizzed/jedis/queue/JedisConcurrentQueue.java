package com.fizzed.jedis.queue;

import com.fizzed.crux.util.StopWatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import redis.clients.jedis.JedisPool;

/**
 * Queue that allows multiple threads to compete for a set number of permits
 * before being permitted to actually pop() items from a redis-backed queue.
 * Helps prevent a pool of threads from each grabbing a connection to redis
 * simply to sit there and wait for an item to exist.
 * 
 * @author jjlauer
 * @param <E> 
 */
public class JedisConcurrentQueue<E> extends JedisQueue<E> {
//    static private final Logger log = LoggerFactory.getLogger(JedisConcurrentQueue.class);

    protected final Semaphore semaphore;
    
    public JedisConcurrentQueue(String name, JedisPool jedisPool, Function<E,byte[]> encode, Function<byte[],E> decode) {
        this(name, jedisPool, encode, decode, 1);
    }
    
    public JedisConcurrentQueue(String name, JedisPool jedisPool, Function<E,byte[]> encode, Function<byte[],E> decode, int permits) {
        super(name, jedisPool, encode, decode);
        this.semaphore = new Semaphore(permits);
        if (permits <= 0) {
            throw new IllegalArgumentException("permits must be > 0");
        }
    }
    
    protected boolean acquirePermit(Semaphore semaphore, long timeout, TimeUnit unit) throws InterruptedException {
        if (timeout < 0) {
            semaphore.acquire();
            return true;
        } else if (timeout == 0) {
            return semaphore.tryAcquire();
        } else {
            return semaphore.tryAcquire(timeout, unit);
        }
    }
    
    @Override
    protected byte[] doPop(long timeout, TimeUnit unit) throws InterruptedException {
        StopWatch timer = StopWatch.timeMillis();
        
        // only permit 1 thread to actually pop the jedis queue at time
        if (!this.acquirePermit(this.semaphore, timeout, unit)) {
            return null;
        }
        
        // permit acquired
        //log.debug("Acquired pop lock (in {})", timer);
        try {
            if (timeout < 0) {
                return super.doPop(timeout, unit);
            } else if (timeout == 0) {
                return super.doPop(timeout, unit);
            } else {
                // calculate timeout remaining
                double remaining = TimeUnit.MILLISECONDS.convert(timeout, unit)
                    - timer.elapsedMillis();

                //log.debug("Timeout remaining {} ms", remaining);
            
                // if nothing is left try an immediate pop at least
                if (remaining < 0) {
                    //log.debug("Timeout remaining < 0, trying an immediate pop");
                    return super.doPop(0, null);
                } else {
                    long remainingMillis = (long)remaining;
                    //log.debug("Timeout remaining {} ms, doing doPop", remainingMillis);
                    return super.doPop(remainingMillis, TimeUnit.MILLISECONDS);
                }
            }
        } finally {
            this.semaphore.release();
        }
    }
}