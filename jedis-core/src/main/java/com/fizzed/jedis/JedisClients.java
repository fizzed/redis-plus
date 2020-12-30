package com.fizzed.jedis;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import redis.clients.jedis.Jedis;

/**
 * Utility methods for executing commands with a Jedis client.
 */
public class JedisClients {
    
    static public <R> R execute(String name, Jedis jedis, long commandTimeout, long pollingTimeout, TimeUnit unit, Function<Integer,R> executable) throws TimeoutException, InterruptedException {
        Objects.requireNonNull(jedis, "jedis was null");
        Objects.requireNonNull(unit, "unit was null");
        
        if (commandTimeout > 0 && pollingTimeout > commandTimeout) {
            throw new IllegalArgumentException("pollingTimeout > commandTimeout");
        }
        
        if (pollingTimeout < 0) {
            throw new IllegalArgumentException("pollingTimeout < 0");
        }

        // NOTE: jedis will block forever on a socket read() and does not
        // honor thread interrupts - the workaround is to use smaller blocking
        // timeouts and check for interrupts
        final long commandTimeoutMillis = unit.toMillis(commandTimeout);
        final int pollingTimeoutSecs = Math.max(1, (int)unit.toSeconds(pollingTimeout));
        final long start = System.currentTimeMillis();
        
        while (true) { 
            // interrupted?
            if (Thread.interrupted()) {
                throw new InterruptedException("Redis " + name + " command interrupted");
            }

            // timeout?
            if (commandTimeoutMillis > 0 && (System.currentTimeMillis() - start) >= commandTimeoutMillis) {
                throw new TimeoutException("Redis " + name + " command timeout (in " + commandTimeoutMillis + " ms)");
            }

            R result = executable.apply(pollingTimeoutSecs);
            
            if (result == null) {
                // jedis timeout, keep trying
            } else {
                return result;
            }
        }
    }
    
    static public byte[] blpop(Jedis jedis, byte[] key, long commandTimeout, long pollingTimeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        Objects.requireNonNull(key, "key was null");
        
        return execute("blpop", jedis, commandTimeout, pollingTimeout, unit, (timeout) -> {
            List<byte[]> items = jedis.blpop(timeout, key);

            // When a non-zero timeout is specified, and the BLPOP operation timed out
            // the return value is a nil multi bulk reply.
            if (items == null || items.isEmpty() || items.size() != 2) {
                return null;
            } else {
                return items.get(1);
            }
        });
    }
    
}
