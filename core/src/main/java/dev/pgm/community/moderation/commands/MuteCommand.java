package dev.pgm.community.moderation.commands;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.commands.player.TargetPlayer;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.org.incendo.cloud.annotation.specifier.FlagYielding;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Flag;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;

public class MuteCommand extends CommunityCommand {

  private final ModerationFeature moderation;
  private final UsersFeature usernames;

  public MuteCommand() {
    this.moderation = Community.get().getFeatures().getModeration();
    this.usernames = Community.get().getFeatures().getUsers();
  }

  @Command("mute|m <target> <duration> <reason>")
  @CommandDescription("Prevent player from speaking in the chat")
  @Permission(CommunityPermissions.MUTE)
  public void mutePlayer(
      CommandAudience audience,
      @Argument("target") TargetPlayer target,
      @Argument("duration") Duration length,
      @Argument("reason") @FlagYielding String reason,
      @Flag(value = "silent", aliases = "s") boolean silent) {
    getTarget(target.getIdentifier(), usernames)
        .thenAccept(
            id -> {
              if (id.isPresent()) {
                moderation.punish(
                    PunishmentType.MUTE,
                    id.get(),
                    audience,
                    reason,
                    length,
                    true,
                    isDisguised(audience) || silent);
              } else {
                audience.sendWarning(formatNotFoundComponent(target.getIdentifier()));
              }
            });
  }

  @Command("unmute|um <target>")
  @CommandDescription("Unmute a player")
  @Permission(CommunityPermissions.MUTE)
  public void unMutePlayer(CommandAudience audience, @Argument("target") TargetPlayer target) {
    getTarget(target.getIdentifier(), usernames)
        .thenAccept(
            id -> {
              if (id.isPresent()) {

                moderation
                    .isMuted(id.get())
                    .thenAcceptAsync(
                        isMuted -> {
                          usernames
                              .renderUsername(id, NameStyle.FANCY)
                              .thenAcceptAsync(
                                  name -> {
                                    if (isMuted.isPresent()) {
                                      moderation
                                          .unmute(id.get(), audience.getId().orElse(null))
                                          .thenAcceptAsync(
                                              pardon -> {
                                                if (!pardon) {
                                                  audience.sendWarning(
                                                      text()
                                                          .append(name)
                                                          .append(
                                                              text(
                                                                  " could not be ",
                                                                  NamedTextColor.GRAY))
                                                          .append(text("unmuted"))
                                                          .color(NamedTextColor.RED)
                                                          .build());
                                                } else {
                                                  BroadcastUtils.sendAdminChatMessage(
                                                      text()
                                                          .append(name)
                                                          .append(
                                                              text(
                                                                  " was unmuted by ",
                                                                  NamedTextColor.GRAY))
                                                          .append(audience.getStyledName())
                                                          .build(),
                                                      CommunityPermissions.MUTE);

                                                  Player online = Bukkit.getPlayer(id.get());
                                                  if (online != null) {
                                                    Audience.get(online)
                                                        .sendWarning(
                                                            translatable(
                                                                "moderation.unmute.target",
                                                                NamedTextColor.GREEN));
                                                  }
                                                }
                                              });
                                    } else {
                                      audience.sendWarning(
                                          text()
                                              .append(name)
                                              .append(text(" is not muted", NamedTextColor.GRAY))
                                              .build());
                                    }
                                  });
                        });
              }
            });
  }

  @Command("mutes")
  @CommandDescription("List all online players who are muted")
  @Permission(CommunityPermissions.MUTE)
  public void listOnlineMuted(CommandAudience audience) {
    Set<Player> mutedPlayers = moderation.getOnlineMutes();

    List<Component> onlineMutes =
        mutedPlayers.stream()
            .map(player -> PlayerComponent.player(player, NameStyle.FANCY))
            .collect(Collectors.toList());

    if (onlineMutes.isEmpty()) {
      audience.sendWarning(translatable("moderation.mute.none"));
      return;
    }

    Component names = Component.join(text(", ", NamedTextColor.GRAY), onlineMutes);
    Component message =
        translatable("moderation.mute.list", NamedTextColor.GOLD)
            .append(text("(", NamedTextColor.GRAY))
            .append(text(Integer.toString(onlineMutes.size()), NamedTextColor.YELLOW))
            .append(text("): ", NamedTextColor.GRAY))
            .append(names);

    audience.sendMessage(message);
  }
}
