package com.fizzed.jedis.ninja;

import com.google.inject.AbstractModule;
import redis.clients.jedis.JedisPool;

public class NinjaJedisModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(JedisPool.class).toProvider(NinjaJedisPoolProvider.class);
        bind(NinjaJedisLifecycle.class);
    }
    
}