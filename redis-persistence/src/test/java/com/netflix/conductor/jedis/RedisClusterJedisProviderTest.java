package com.netflix.conductor.jedis;

import com.netflix.dyno.connectionpool.Host;
import com.netflix.dyno.connectionpool.HostSupplier;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisNoReachableClusterNodeException;

import java.net.UnknownHostException;
import java.util.Arrays;

import static org.mockito.Mockito.when;

public class RedisClusterJedisProviderTest {


    @Test
    public void onGetWithNullPasswordInHosts_commandsIsNotNull() {
        HostSupplier hostSupplier = Mockito.mock(HostSupplier.class);
        Host mockHost = new Host("hostname", 123, "region");
        when(hostSupplier.getHosts()).thenReturn(Arrays.asList(mockHost));
        RedisClusterJedisProvider provider = new RedisClusterJedisProvider(hostSupplier);
        JedisCommands commands = provider.get();
        Assert.assertNotNull(commands);
    }

    @Test(expected = JedisConnectionException.class)
    public void onGetWithPasswordInHosts_commandsIsNotNull() {
        HostSupplier hostSupplier = Mockito.mock(HostSupplier.class);
        Host mockHost = new Host("hostname", 123, "region", Host.Status.Up, "hastag", "password");

        when(hostSupplier.getHosts()).thenReturn(Arrays.asList(mockHost));
        RedisClusterJedisProvider provider = new RedisClusterJedisProvider(hostSupplier);
        JedisCommands commands = provider.get();
    }

}
