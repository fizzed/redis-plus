package com.fizzed.jedis;

import com.fizzed.crux.uri.Uri;
import com.fizzed.crux.util.BindingPropertyMap;
import com.fizzed.crux.util.BindingPropertySupport;
import java.util.Objects;

public class JedisOptions<A extends JedisOptions<A>> implements BindingPropertySupport<A> {
    
    protected final BindingPropertyMap<A> bindingPropertyMap = new BindingPropertyMap<A>()
        .bindString("password", A::setPassword)
        .bindInteger("database", A::setDatabase)
        .bindLong("connect_timeout", A::setConnectTimeout)
        .bindInteger("pool_min_size", A::setPoolMinSize)
        .bindInteger("pool_max_size", A::setPoolMaxSize)
        .bindLong("pool_wait_timeout", A::setPoolWaitTimeout);

    private String host;
    private Integer port;
    private Integer database;
    private String password;
    private Long connectTimeout;
    // pool configuration
    private Integer poolMinSize;
    private Integer poolMaxSize;
    private Long poolWaitTimeout;

    public JedisOptions() {
        this((Uri)null);
    }
    
    public JedisOptions(String uri) {
        this(new Uri(uri));
    }
    
    public JedisOptions(Uri uri) {
        // defaults
        this.port = 6379;
        this.connectTimeout = 60000L;
        this.poolMinSize = 1;
        this.poolMaxSize = 5;
        this.poolWaitTimeout = 5000L;
        if (uri != null) {
            this.setUri(uri);
        }
    }
    
    @Override
    public BindingPropertyMap<A> getPropertyMap() {
        return this.bindingPropertyMap;
    }

    public String getHost() {
        return host;
    }

    public JedisOptions setHost(String host) {
        this.host = host;
        return this;
    }

    public Integer getPort() {
        return port;
    }

    public JedisOptions setPort(Integer port) {
        this.port = port;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public JedisOptions setPassword(String password) {
        this.password = password;
        return this;
    }

    public Integer getDatabase() {
        return database;
    }

    public void setDatabase(Integer database) {
        this.database = database;
    }

    public Long getConnectTimeout() {
        return connectTimeout;
    }

    public JedisOptions setConnectTimeout(Long connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public Integer getPoolMinSize() {
        return poolMinSize;
    }

    public void setPoolMinSize(Integer poolMinSize) {
        this.poolMinSize = poolMinSize;
    }

    public Integer getPoolMaxSize() {
        return poolMaxSize;
    }

    public void setPoolMaxSize(Integer poolMaxSize) {
        this.poolMaxSize = poolMaxSize;
    }

    public Long getPoolWaitTimeout() {
        return poolWaitTimeout;
    }

    public void setPoolWaitTimeout(Long poolWaitTimeout) {
        this.poolWaitTimeout = poolWaitTimeout;
    }

    public final JedisOptions setUri(Uri uri) {
        Objects.requireNonNull(uri);
        
        // scheme should be redis
        if (!Objects.equals(uri.getScheme(), "redis")) {
            throw new IllegalArgumentException("Unsupported scheme " + uri.getScheme() + " (supported are: redis)");
        }
        
        if (uri.getHost() != null) {
            this.setHost(uri.getHost());
        }
        
        if (uri.getPort() != null) {
            this.setPort(uri.getPort());
        }
        
        String rel0 = uri.getRel(0);
        if (rel0 != null) {
            try {
                Integer db = Integer.valueOf(rel0);
                this.setDatabase(db);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unsupported database " + rel0 + " (must be an integer)");
            }
        }
        
        // passthru remaining properties
        this.setProperties(uri.getQueryFirstMap());
        
        return this;
    }
    
}