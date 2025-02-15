package dev.pgm.community.polls.commands;

import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.player.PlayerComponent.player;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.polls.PollThreshold;
import dev.pgm.community.polls.feature.PollFeature;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.lib.org.incendo.cloud.annotation.specifier.Greedy;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Flag;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextException;

@Command("poll")
@Permission(CommunityPermissions.POLL)
public class PollManagementCommands extends CommunityCommand {

  private final PollFeature polls;

  public PollManagementCommands() {
    this.polls = Community.get().getFeatures().getPolls();
  }

  @Command("")
  public void info(CommandAudience audience) {
    if (polls.isRunning()) {
      polls.sendPollDetails(polls.getPoll(), audience);
    } else {
      polls.sendBuilderDetails(polls.getBuilder(), audience);
    }
  }

  @Command("start [duration]")
  public void start(
      CommandAudience audience,
      @Argument("duration") Duration duration,
      @Flag(value = "delay", aliases = "d") Duration delay) {
    if (polls.isDelayedStartScheduled()) {
      audience.sendWarning(
          text("A poll is already scheduled to start soon!")
              .hoverEvent(
                  HoverEvent.showText(
                      text()
                          .append(text("Use "))
                          .append(text("/poll end", NamedTextColor.AQUA))
                          .append(text(" to cancel delayed start."))
                          .color(NamedTextColor.GRAY)
                          .build())));
      return;
    }

    if (!polls.canStart()) {
      audience.sendWarning(
          text()
              .append(
                  text("Your poll is not ready to start! Check ")
                      .append(text("/poll", NamedTextColor.AQUA))
                      .append(text(" for more info.")))
              .hoverEvent(HoverEvent.showText(text("Click to view poll info")))
              .clickEvent(ClickEvent.runCommand("/poll"))
              .build());
      return;
    }

    if (duration != null) {
      polls.getBuilder().duration(audience, duration);
    }

    polls.getBuilder().creator(audience.getId().orElse(null));
    if (delay == null) {
      polls.start(audience);
    } else {
      polls.delayedStart(audience, delay);
    }
  }

  @Command("end")
  public void end(CommandAudience audience) {
    polls.end(audience);
  }

  @Command("question <question>")
  public void question(CommandAudience audience, @Argument("question") @Greedy String question) {
    checkPoll();
    polls.getBuilder().question(audience, question);
  }

  @Command("duration <duration>")
  public void timelimit(CommandAudience audience, @Argument("duration") Duration duration) {
    checkPoll();
    polls.getBuilder().duration(audience, duration);
  }

  @Command("threshold <threshold>")
  public void threshold(CommandAudience audience, @Argument("threshold") PollThreshold threshold) {
    checkPoll();
    polls.getBuilder().threshold(audience, threshold);
  }

  @Command("map <map>")
  public void map(CommandAudience audience, @Argument("map") MapInfo map) {
    checkPoll();
    polls.getBuilder().map(audience, map);
  }

  @Command("kick <target>")
  public void kickPlayer(CommandAudience audience, @Argument("target") Player target) {
    checkPoll();

    if (target != null && Integration.isVanished(target)) {
      audience.sendWarning(
          text()
              .append(player(target, NameStyle.FANCY))
              .append(text(" is vanished! Please select a non-vanished target."))
              .build());
      return;
    }

    polls.getBuilder().kickPlayer(audience, target.getUniqueId());
  }

  @Command("command <command>")
  public void command(CommandAudience audience, @Argument("command") @Greedy String command) {
    checkPoll();
    polls.getBuilder().command(audience, command);
  }

  @Command("mutation <mutation>")
  @Permission(CommunityPermissions.MUTATION)
  public void mutation(CommandAudience audience, @Argument("mutation") MutationType mutation) {
    checkPoll();
    polls.getBuilder().mutation(audience, mutation);
  }

  @Command("remove <option>")
  public void remove(CommandAudience audience, @Argument("option") String option) {
    checkPoll();
    polls.getBuilder().remove(audience, option);
  }

  @Command("reset")
  public void reset(CommandAudience audience) {
    checkPoll();
    polls.resetBuilder();
    audience.sendWarning(text("Poll values have been reset!"));
  }

  private void checkPoll() {
    if (polls.isRunning()) {
      throw TextException.exception("Poll can not be adjusted at this time!");
    }
  }
}
