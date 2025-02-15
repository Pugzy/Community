package dev.pgm.community.moderation.commands;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.commands.player.TargetPlayer;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import org.bukkit.Bukkit;
import tc.oc.pgm.lib.org.incendo.cloud.annotation.specifier.FlagYielding;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Flag;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;

public class BanCommand extends CommunityCommand {

  private final ModerationFeature moderation;
  private final UsersFeature usernames;

  public BanCommand() {
    this.moderation = Community.get().getFeatures().getModeration();
    this.usernames = Community.get().getFeatures().getUsers();
  }

  @Command("ban <target> <reason>")
  @CommandDescription("Issue a ban punishment")
  @Permission(CommunityPermissions.BAN)
  public void ban(
      CommandAudience audience,
      @Argument("target") TargetPlayer target,
      @Argument("reason") @FlagYielding String reason,
      @Flag(value = "time", aliases = "t") Duration time,
      @Flag(value = "silent", aliases = "s") boolean silent) {
    if (time == null) {
      permBan(audience, target, reason, silent);
    } else {
      tempBan(audience, target, time, reason, silent);
    }
  }

  @Command("permban|pb <target> <reason>")
  @CommandDescription("Permanently ban a player from the server")
  @Permission(CommunityPermissions.BAN)
  public void permBan(
      CommandAudience audience,
      @Argument("target") TargetPlayer target,
      @Argument("reason") @FlagYielding String reason,
      @Flag(value = "silent", aliases = "s") boolean silent) {
    getTarget(target.getIdentifier(), usernames)
        .thenAccept(
            id -> {
              if (id.isPresent()) {
                moderation.punish(
                    PunishmentType.BAN,
                    id.get(),
                    audience,
                    reason,
                    null,
                    true,
                    isDisguised(audience) || silent);
              } else {
                audience.sendWarning(formatNotFoundComponent(target.getIdentifier()));
              }
            });
  }

  @Command("tempban|tb <target> <time> <reason>")
  @CommandDescription("Temporarily ban a player from the server")
  @Permission(CommunityPermissions.BAN)
  public void tempBan(
      CommandAudience audience,
      @Argument("target") TargetPlayer target,
      @Argument("time") Duration time,
      @Argument("reason") @FlagYielding String reason,
      @Flag(value = "silent", aliases = "s") boolean silent) {
    getTarget(target.getIdentifier(), usernames)
        .thenAccept(
            id -> {
              if (id.isPresent()) {
                moderation.punish(
                    PunishmentType.TEMP_BAN,
                    id.get(),
                    audience,
                    reason,
                    time,
                    true,
                    isDisguised(audience) || silent);
              } else {
                audience.sendWarning(formatNotFoundComponent(target.getIdentifier()));
              }
            });
  }

  @Command("nameban|nb <target>")
  @CommandDescription("Ban a player based on their username. Auto unbans if name changes")
  @Permission(CommunityPermissions.BAN)
  public void nameBan(
      CommandAudience audience,
      @Argument("target") TargetPlayer target,
      @Flag(value = "silent", aliases = "s") boolean silent) {
    usernames
        .getStoredProfile(target.getIdentifier())
        .thenAccept(
            profile -> {
              if (profile != null) {
                Bukkit.getScheduler()
                    .runTask(
                        Community.get(),
                        () -> {
                          // Due to async username lookup, must run task sync to avoid async kick
                          moderation.punish(
                              PunishmentType.NAME_BAN,
                              profile.getId(),
                              audience,
                              profile.getUsername(),
                              null,
                              true,
                              isDisguised(audience) || silent);
                        });
              } else {
                audience.sendWarning(formatNotFoundComponent(target.getIdentifier()));
              }
            });
  }
}
