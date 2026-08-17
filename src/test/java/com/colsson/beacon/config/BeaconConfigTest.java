package com.colsson.beacon.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BeaconConfigTest {

    @Test
    void defaults_createsValidConfig() {
        BeaconConfig config = BeaconConfig.defaults();
        assertEquals("localhost", config.getMysqlHost());
        assertEquals(3306, config.getMysqlPort());
        assertEquals("beacon", config.getMysqlDatabase());
        assertEquals("root", config.getMysqlUsername());
        assertEquals("", config.getMysqlPassword());
        assertTrue(config.isCacheEnabled());
        assertEquals(300, config.getCacheTtlSeconds());
        assertEquals(1000, config.getCacheMaxSize());
        assertEquals("default", config.getServerName());
        assertTrue(config.isAutoMigrate());
    }

    @Test
    void testConfig_returnsValidTestConfig() {
        BeaconConfig config = BeaconConfig.testConfig();
        assertTrue(config.isCacheEnabled());
        assertEquals(300, config.getCacheTtlSeconds());
        assertEquals("test", config.getServerName());
    }

    @Test
    void setters_getters_mysqlHost() {
        BeaconConfig config = new BeaconConfig();
        config.setMysqlHost("192.168.1.100");
        assertEquals("192.168.1.100", config.getMysqlHost());
    }

    @Test
    void setters_getters_mysqlPort() {
        BeaconConfig config = new BeaconConfig();
        config.setMysqlPort(3307);
        assertEquals(3307, config.getMysqlPort());
    }

    @Test
    void setters_getters_mysqlDatabase() {
        BeaconConfig config = new BeaconConfig();
        config.setMysqlDatabase("beacon_prod");
        assertEquals("beacon_prod", config.getMysqlDatabase());
    }

    @Test
    void setters_getters_mysqlCredentials() {
        BeaconConfig config = new BeaconConfig();
        config.setMysqlUsername("admin");
        config.setMysqlPassword("secret123");
        assertEquals("admin", config.getMysqlUsername());
        assertEquals("secret123", config.getMysqlPassword());
    }

    @Test
    void setters_getters_cacheEnabled() {
        BeaconConfig config = new BeaconConfig();
        config.setCacheEnabled(false);
        assertFalse(config.isCacheEnabled());
    }

    @Test
    void setters_getters_cacheTtl() {
        BeaconConfig config = new BeaconConfig();
        config.setCacheTtlSeconds(60);
        assertEquals(60, config.getCacheTtlSeconds());
    }

    @Test
    void setters_getters_cacheMaxSize() {
        BeaconConfig config = new BeaconConfig();
        config.setCacheMaxSize(500);
        assertEquals(500, config.getCacheMaxSize());
    }

    @Test
    void setters_getters_serverName() {
        BeaconConfig config = new BeaconConfig();
        config.setServerName("lobby");
        assertEquals("lobby", config.getServerName());
    }

    @Test
    void setters_getters_autoMigrate() {
        BeaconConfig config = new BeaconConfig();
        config.setAutoMigrate(false);
        assertFalse(config.isAutoMigrate());
    }

    @Test
    void toString_containsHostAndDb() {
        BeaconConfig config = BeaconConfig.defaults();
        String str = config.toString();
        assertTrue(str.contains("localhost"));
        assertTrue(str.contains("beacon"));
        assertTrue(str.contains("default"));
    }
}
