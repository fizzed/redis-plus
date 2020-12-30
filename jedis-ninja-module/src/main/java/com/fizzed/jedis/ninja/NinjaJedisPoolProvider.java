package com.fizzed.jedis.ninja;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.fizzed.jedis.JedisFactory;
import com.fizzed.jedis.JedisOptions;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import ninja.utils.NinjaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

@Singleton
public class NinjaJedisPoolProvider implements Provider<JedisPool> {
    static private Logger log = LoggerFactory.getLogger(NinjaJedisPoolProvider.class);
    
    private final NinjaProperties ninjaProperties;
    private final Supplier<JedisPool> memoizedSupplier;
    
    @Inject
    public NinjaJedisPoolProvider(NinjaProperties ninjaProperties) {
        this.ninjaProperties = ninjaProperties;
        this.memoizedSupplier = Suppliers.memoize(() -> {
            return this.build();
        });
    }
    
    @Override
    public JedisPool get() {
        return this.memoizedSupplier.get();
    }
    
    public JedisPool build() {
        final String url = this.ninjaProperties.getOrDie("redis.url");
        final String password = this.ninjaProperties.get("redis.password");

        log.info("Using redis url {}", url);
        
        JedisOptions options = new JedisOptions(url);
        
        if (password != null) {
            options.setPassword(password);
        }
        
        return JedisFactory.createJedisPool(options);
    }
    
}