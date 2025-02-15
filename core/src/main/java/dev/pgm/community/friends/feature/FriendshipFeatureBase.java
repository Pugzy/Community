package dev.pgm.community.friends.feature;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.Community;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.friends.FriendshipConfig;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.utils.Sounds;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.Audience;

public abstract class FriendshipFeatureBase extends FeatureBase implements FriendshipFeature {

  @Nullable
  protected PGMFriendIntegration integration;

  public FriendshipFeatureBase(Configuration config, Logger logger, String featureName) {
    super(new FriendshipConfig(config), logger, featureName);

    if (getConfig().isEnabled()) {
      enable();
    }
  }

  public FriendshipConfig getFriendshipConfig() {
    return (FriendshipConfig) getConfig();
  }

  @Override
  public void enable() {
    super.enable();
    if (isPGMEnabled()) {
      this.integration = new PGMFriendIntegration();
    }
  }

  public void sendFriendRequestLoginMessage(Player player, int requestCount) {
    Component requestsMessage = text()
        .append(BroadcastUtils.RIGHT_DIV.color(NamedTextColor.GOLD))
        .append(text(" You have "))
        .append(text(requestCount, NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
        .append(text(" pending friend request" + (requestCount != 1 ? "s " : " ")))
        .append(BroadcastUtils.LEFT_DIV.color(NamedTextColor.GOLD))
        .color(NamedTextColor.DARK_GREEN)
        .hoverEvent(
            HoverEvent.showText(text("Click to view pending friend requests", NamedTextColor.GRAY)))
        .clickEvent(ClickEvent.runCommand("/friend requests"))
        .build();

    Audience.get(player).sendMessage(requestsMessage);
    Audience.get(player).playSound(Sounds.FRIEND_REQUEST_LOGIN);
  }

  @EventHandler
  public void onDelayedPlayerJoin(PlayerJoinEvent event) {
    // Used to send online friend requests notifications AFTER all other login messages have been
    // sent
    Bukkit.getScheduler()
        .scheduleSyncDelayedTask(
            Community.get(),
            new Runnable() {
              @Override
              public void run() {
                onDelayedLogin(event);
              }
            },
            40L);
  }

  @EventHandler
  public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
    this.onPreLogin(event);
  }

  private boolean isPGMEnabled() {
    return PGMUtils.isPGMEnabled() && getFriendshipConfig().isIntegrationEnabled();
  }
}
