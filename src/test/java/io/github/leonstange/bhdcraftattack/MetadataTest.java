package io.github.leonstange.bhdcraftattack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetadataTest {
    @Test
    void paperCanLoadVersionAndCommandWithoutBlockingPublicRules() throws Exception {
        try (var stream = getClass().getResourceAsStream("/plugin.yml")) {
            assertNotNull(stream);
            var meta = new PluginDescriptionFile(stream);
            assertEquals("BHD-CraftAttack", meta.getName());
            assertEquals("0.0.1", meta.getVersion());
            assertEquals("26.3", meta.getAPIVersion());
            assertEquals(BhdCraftAttackPlugin.class.getName(), meta.getMain());
            assertNull(meta.getCommands().get("bhd-craftattack").get("permission"));
        }
    }

    @Test
    void defaultConfigHasAllRulesAndEnablesDeathCounter() throws Exception {
        try (var stream = getClass().getResourceAsStream("/config.yml")) {
            assertNotNull(stream);
            var config = new YamlConfiguration();
            config.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            assertTrue(config.getBoolean("death-counter.enabled"));
            assertEquals(8, config.getStringList("rules").size());
            assertTrue(config.getStringList("rules").getFirst().contains("respektvoll"));
        }
    }
}
