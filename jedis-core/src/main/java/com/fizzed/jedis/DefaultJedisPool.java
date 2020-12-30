package com.fizzed.jedis;

import com.fizzed.crux.uri.MutableUri;
import com.fizzed.crux.uri.Uri;
import java.lang.reflect.Field;
import java.util.Map;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

public class DefaultJedisPool extends JedisPool {
    static private final Logger log = LoggerFactory.getLogger(DefaultJedisPool.class);
    
    private final String host;
    private final Integer port;
    private final Integer database;
    
    public DefaultJedisPool(GenericObjectPoolConfig poolConfig, String host, Integer port, Integer connectionTimeout, String password, Integer database) {
        super(poolConfig, host, port, connectionTimeout, password, (database != null ? database : Protocol.DEFAULT_DATABASE));
        this.host = host;
        this.port = port;
        this.database = database;
    }

    public Uri getUri() {
        MutableUri uri = new MutableUri()
            .scheme("redis")
            .host(this.host)
            .port(this.port);
        
        if (this.database != null) {
            uri.rel(this.database);
        }
        
        return uri.immutable();
    }
    
    private Map<?,PooledObject<Jedis>> getAllPooledObjects() {
        try {
            // expose private field of all the objects in the pool
            Field field = this.internalPool.getClass().getDeclaredField("allObjects");
            field.setAccessible(true);
            return (Map<?,PooledObject<Jedis>>)field.get(this.internalPool);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException t) {
            log.error("Unable to get allObjects field from underlying jedis pool", t);
            return null;
        }
    }
    
    @Override
    public void close() {
        Map<?,PooledObject<Jedis>> allPooledObjects = this.getAllPooledObjects();
        
        // always call parent
        super.close();
        
        // follow-up by closing anything that is left
        if (allPooledObjects != null) {
            allPooledObjects.values().forEach(pooledObject -> {
                try {
                    // close client to force socket to close and unblock any read()s on it
                    pooledObject.getObject().getClient().close();
                } catch (Exception e) {
                    log.error("Unable to cleanly close jedis client: {}", e.getMessage());
                }
            });
        }
    }
    
}
