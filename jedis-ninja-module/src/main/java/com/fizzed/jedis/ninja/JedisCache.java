package com.fizzed.jedis.ninja;

import com.fizzed.crux.util.StopWatch;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.inject.Inject;
import ninja.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class JedisCache implements Cache {
    static private final Logger log = LoggerFactory.getLogger(JedisCache.class);

    static private final byte[] NX = serialize("NX");
    static private final byte[] XX = serialize("XX");
    static private final byte[] EX = serialize("EX");
    
    private final JedisPool jedisPool;

    @Inject
    public JedisCache(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }
    
    @Override
    public void add(String key, Object value, int expirationInSeconds) {
        this.safeAdd(key, value, expirationInSeconds);
    }

    @Override
    public boolean safeAdd(String key, Object value, int expirationInSeconds) {
        log.trace("add: {} = {} (expire in {} s)", key, value, expirationInSeconds);
        
        try (Jedis jedis = this.jedisPool.getResource()) {
            String reply = jedis.set(serialize(key), serialize(value), NX, EX, expirationInSeconds);
            log.trace("add reply: {}", reply);
            return StringUtils.equalsIgnoreCase("ok", reply);
        }
    }
    
    @Override
    public void set(String key, Object value, int expirationInSeconds) {
        this.safeSet(key, value, expirationInSeconds);
    }

    @Override
    public boolean safeSet(String key, Object value, int expirationInSeconds) {
        log.trace("set: {} = {} (expire in {} s)", key, value, expirationInSeconds);
        
        try (Jedis jedis = this.jedisPool.getResource()) {
            String reply = jedis.setex(serialize(key), expirationInSeconds, serialize(value));
            //log.trace("set reply: {}", reply);      // OK
            //return true;
            return StringUtils.equalsIgnoreCase("ok", reply);
        }
    }

    @Override
    public Object get(String key) {
        log.trace("get: {}", key);
        
        try (Jedis jedis = this.jedisPool.getResource()) {
            final StopWatch timer = StopWatch.timeMillis();
            byte[] value = jedis.get(serialize(key));
            log.trace("get (in {})", timer);
            if (value == null) {
                log.trace("get: {} was null", key);
                return null;
            }
            log.trace("get: {} present", key);
            return deserialize(value);
        }
    }
    
    @Override
    public void delete(String key) {
        this.safeDelete(key);
    }

    @Override
    public boolean safeDelete(String key) {
        log.trace("delete: {}", key);
        try (Jedis jedis = this.jedisPool.getResource()) {
            Long reply = jedis.del(serialize(key));
            log.trace("delete reply: {}", reply);
            return (reply != null && reply > 0);
        }
    }
    
    @Override
    public void replace(String key, Object value, int expirationInSeconds) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean safeReplace(String key, Object value, int expirationInSeconds) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String,Object> get(String[] keys) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long incr(String key, int by) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long decr(String key, int by) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    static public Object deserialize(byte[] data) {
        try {
            return new ObjectInputStream(new ByteArrayInputStream(data)) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc)
                        throws IOException, ClassNotFoundException {
                    return Class.forName(desc.getName(), false, Thread.currentThread().getContextClassLoader());
                }
            }.readObject();
        } catch (Exception e) {
            log.error("Unable to deserialize", e);
        }
        return null;
    }

    static public byte[] serialize(String object) {
        return object.getBytes(StandardCharsets.UTF_8);
    }
    
    static public byte[] serialize(Object object) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            new ObjectOutputStream(bos).writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("Unable to serialize", e);
        }
        return null;
    }
    
}