package dev.pgm.community.requests.commands.sponsor;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.commands.player.TargetPlayer;
import dev.pgm.community.requests.RequestConfig;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.feature.RequestFeature;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import java.time.Duration;
import java.time.Instant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;

@Command("tokens|sponsortokens|token")
public class TokenCommands extends CommunityCommand {

  private final UsersFeature users;
  private final RequestFeature requests;

  public TokenCommands() {
    this.users = Community.get().getFeatures().getUsers();
    this.requests = Community.get().getFeatures().getRequests();
  }

  @Command("[target]")
  @CommandDescription("Check your token balance")
  public void tokens(CommandAudience audience, @Argument("target") TargetPlayer target) {
    if (target != null && audience.hasPermission(CommunityPermissions.TOKEN_BALANCE)) {
      getTarget(target.getIdentifier(), users).thenAcceptAsync(uuid -> {
        if (uuid.isPresent()) {
          RequestProfile profile = requests.getRequestProfile(uuid.get()).join();
          if (profile == null) {
            audience.sendWarning(formatNotFoundComponent(target.getIdentifier()));
            return;
          }

          Component name = users.renderUsername(uuid, NameStyle.FANCY).join();
          sendTokenBalanceMessage(audience.getAudience(), name, profile.getSponsorTokens());
        } else {
          audience.sendWarning(formatNotFoundComponent(target.getIdentifier()));
        }
      });
    } else if (audience.isPlayer()) {
      Player player = audience.getPlayer();
      requests.getRequestProfile(player.getUniqueId()).thenAcceptAsync(profile -> {
        int tokens = profile.getSponsorTokens();
        sendTokenBalanceMessage(audience.getAudience(), null, tokens);
        sendRefreshDuration(audience.getAudience(), player, profile);
      });
    } else {
      audience.sendWarning(text("Please provide a username to check the token balance of"));
    }
  }

  @Command("give <target> <amount>")
  @CommandDescription("Give the targeted player sponsor tokens")
  @Permission(CommunityPermissions.ADMIN)
  public void give(
      CommandAudience audience,
      @Argument("target") TargetPlayer target,
      @Argument("amount") int amount) {
    getTarget(target.getIdentifier(), users).thenAcceptAsync(targetId -> {
      if (targetId.isPresent()) {
        RequestProfile profile = requests.getRequestProfile(targetId.get()).join();
        if (profile != null) {
          int total = profile.giveSponsorToken(amount);
          requests.update(profile);
          audience.sendMessage(text()
              .append(MessageUtils.TOKEN)
              .append(space())
              .append(
                  users.renderUsername(profile.getPlayerId(), NameStyle.FANCY).join())
              .append(text(" now has "))
              .append(text(total, NamedTextColor.YELLOW, TextDecoration.BOLD))
              .append(text(" sponsor token" + (total != 1 ? "s" : "")))
              .color(NamedTextColor.GRAY)
              .build());
          return;
        }
      }
      audience.sendWarning(formatNotFoundComponent(target.getIdentifier()));
    });
  }

  private void sendTokenBalanceMessage(Audience viewer, Component name, int tokens) {
    viewer.sendMessage(text()
        .append(MessageUtils.TOKEN)
        .append(text(" "))
        .append(name == null ? text("You have ") : name.append(text(" has ", NamedTextColor.GRAY)))
        .append(text(tokens, NamedTextColor.YELLOW, TextDecoration.BOLD))
        .append(text(" sponsor token" + (tokens != 1 ? "s" : "") + "."))
        .color(NamedTextColor.GRAY)
        .build());
  }

  public static class TokenRefreshAmount {
    private Duration timeLeft;
    private int amount;

    public TokenRefreshAmount(Duration timeLeft, int amount) {
      this.timeLeft = timeLeft;
      this.amount = amount;
    }

    public Duration getDuration() {
      return timeLeft;
    }

    public int getAmount() {
      return amount;
    }
  }

  public static TokenRefreshAmount getTimeLeft(
      Player player, Instant lastRefresh, RequestFeature requests) {
    RequestConfig config = (RequestConfig) requests.getConfig();
    int amount = 0;
    Duration timeLeft = null;
    if (player.hasPermission(CommunityPermissions.TOKEN_DAILY)) {
      amount = config.getDailyTokenAmount();
      timeLeft = Duration.ofDays(1).minus(Duration.between(lastRefresh, Instant.now()));
    } else if (player.hasPermission(CommunityPermissions.TOKEN_WEEKLY)) {
      amount = config.getWeeklyTokenAmount();
      timeLeft = Duration.ofDays(7).minus(Duration.between(lastRefresh, Instant.now()));
    }
    return new TokenRefreshAmount(timeLeft, amount);
  }

  private void sendRefreshDuration(Audience viewer, Player player, RequestProfile profile) {
    if (!player.hasPermission(CommunityPermissions.REQUEST_SPONSOR)) return;

    if (profile.getLastTokenRefreshTime() == null) {
      viewer.sendWarning(
          text("Please rejoin the server to claim more tokens!", NamedTextColor.GOLD));
      return;
    }

    TokenRefreshAmount info = getTimeLeft(player, profile.getLastTokenRefreshTime(), requests);
    if (info.getDuration() != null) {
      if (profile.getSponsorTokens() >= ((RequestConfig) requests.getConfig()).getMaxTokens()) {
        viewer.sendMessage(text("Spend some tokens in order to claim more", NamedTextColor.GRAY));
        return;
      }

      if (info.getDuration().isNegative()) {
        viewer.sendMessage(
            text("Token refresh ready! Please rejoin the server to claim", NamedTextColor.GRAY));
        return;
      }

      viewer.sendMessage(text()
          .append(text("-  ", NamedTextColor.YELLOW))
          .append(text("Next token ("))
          .append(text("+", NamedTextColor.GREEN, TextDecoration.BOLD))
          .append(text(info.getAmount(), NamedTextColor.GREEN, TextDecoration.BOLD))
          .append(text(") in "))
          .append(duration(info.getDuration(), NamedTextColor.YELLOW))
          .color(NamedTextColor.GRAY)
          .build());
    }
  }
}
