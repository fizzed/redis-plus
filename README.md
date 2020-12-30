Redis Plus by Fizzed
============================================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.fizzed/redis-plus/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.fizzed/redis-plus)

[Fizzed, Inc.](http://fizzed.com) (Follow on Twitter: [@fizzed_inc](http://twitter.com/fizzed_inc))

## Overview

Utilities and framework integrations for Java and Redis. Includes an integration
of [Redis](https://redis.io/) with the [Ninja Framework](https://github.com/ninjaframework/ninja).

## Ninja Framework

Ninja Framework module for Redis based on Jedis. Will help provide connectivity to Redis,
a connection pool, and a Ninja cache implementation.

### Setup

Add the jedis-ninja-module dependency to your Maven pom.xml

```xml
<dependency>
    <groupId>com.fizzed</groupId>
    <artifactId>jedis-ninja-module</artifactId>
    <version>1.0.0</version>
</dependency>
```

In your `conf/Module.java` file:

```java
package conf;

import com.fizzed.jedis.ninja.NinjaJedisModule;
import com.google.inject.AbstractModule;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        install(new NinjaJedisModule());
    }

}
```

In your `conf/application.conf` file:

```java
#
# redis
#
redis.url = redis://localhost:5432
redis.password = test
redis.validate_at_start = true
```

## License

Copyright (C) 2020 Fizzed, Inc.

This work is licensed under the Apache License, Version 2.0. See LICENSE for details.