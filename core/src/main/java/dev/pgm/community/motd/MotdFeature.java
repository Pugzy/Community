package dev.pgm.community.motd;

import dev.pgm.community.feature.FeatureBase;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import tc.oc.pgm.util.Audience;

/** MotdFeature - Displays a configurable message at login * */
public class MotdFeature extends FeatureBase {

  public MotdFeature(Configuration config, Logger logger) {
    super(new MotdConfig(config), logger, "MOTD");
    if (getConfig().isEnabled()) {
      enable();
    }
  }

  public MotdConfig getMotdConfig() {
    return (MotdConfig) getConfig();
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (getMotdConfig().getLines().isEmpty()) return;
    Audience audience = Audience.get(event.getPlayer());
    getMotdConfig().getLines().forEach(audience::sendMessage);
  }
}
