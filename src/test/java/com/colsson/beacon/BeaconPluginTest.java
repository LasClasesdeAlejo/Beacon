package com.colsson.beacon;

import com.colsson.beacon.config.BeaconConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class BeaconPluginTest {

    private BeaconPlugin plugin;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger("BeaconTest");
        plugin = new BeaconPlugin(logger);
    }

    @AfterEach
    void tearDown() {
        if (plugin.isEnabled()) {
            plugin.disable();
        }
    }

    @Test
    void initialState_isDisabled() {
        assertFalse(plugin.isEnabled());
        assertNull(plugin.getConfig());
        assertNull(plugin.getApi());
        assertNull(plugin.getCommandRouter());
        assertNull(plugin.getDatabase());
    }

    @Test
    void disable_whenNotEnabled_doesNotThrow() {
        assertDoesNotThrow(() -> plugin.disable());
        assertFalse(plugin.isEnabled());
    }
}
