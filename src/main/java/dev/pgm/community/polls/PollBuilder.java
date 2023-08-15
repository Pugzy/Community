package dev.pgm.community.polls;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.polls.ending.EndAction;
import dev.pgm.community.polls.ending.types.CommandEndAction;
import dev.pgm.community.polls.ending.types.KickPlayerAction;
import dev.pgm.community.polls.ending.types.MutationEndAction;
import dev.pgm.community.polls.ending.types.NullEndAction;
import dev.pgm.community.polls.ending.types.SetNextAction;
import dev.pgm.community.polls.types.NormalPoll;
import dev.pgm.community.polls.types.TimedPoll;
import dev.pgm.community.utils.CommandAudience;
import java.time.Duration;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.map.MapInfo;

public class PollBuilder {

  private PollEditAlerter alert; // Used to broadcast changes to values

  // Required
  private Component question;
  private UUID creator;
  private PollThreshold threshold = PollThreshold.SIMPLE;

  // Optional
  private Duration duration;
  private EndAction endAction = new NullEndAction();

  public PollBuilder(PollConfig config, PollEditAlerter alert) {
    this.alert = alert;
    this.duration = (config.getDuration().isNegative() ? null : config.getDuration());
    this.threshold = config.getThreshold();
  }

  public PollBuilder question(CommandAudience sender, String question) {
    this.question = (question != null ? text(question) : null);
    alert.broadcastChange(sender, "Poll Question", question);
    return this;
  }

  public PollBuilder creator(UUID creator) {
    this.creator = creator;
    return this;
  }

  public PollBuilder duration(CommandAudience sender, Duration duration) {
    duration = duration.abs();
    this.duration = duration;
    alert.broadcastChange(sender, "Poll Duration", duration);
    return this;
  }

  public PollBuilder command(CommandAudience sender, String command) {
    alert.broadcastChange(sender, "Poll Command", command);

    if (command == null || command.isEmpty()) {
      this.endAction = new NullEndAction();
      return this;
    }

    this.endAction = new CommandEndAction(command);
    return this;
  }

  public PollBuilder map(CommandAudience sender, MapInfo map) {
    alert.broadcastChange(sender, "Poll Map", map);

    if (map == null) {
      this.endAction = new NullEndAction();
      return this;
    }

    this.endAction = new SetNextAction(map);
    return this;
  }

  public PollBuilder kickPlayer(CommandAudience sender, Player target) {
    alert.broadcastChange(sender, "Poll Kick Target", target);

    if (target == null || !target.isOnline()) {
      this.endAction = new NullEndAction();
      return this;
    }

    this.endAction = new KickPlayerAction(target.getUniqueId());
    return this;
  }

  public PollBuilder mutation(CommandAudience sender, MutationType mutation) {
    alert.broadcastChange(sender, "Poll Mutation", mutation);

    if (mutation == null) {
      this.endAction = new NullEndAction();
      return this;
    }

    this.endAction = new MutationEndAction(mutation);
    return this;
  }

  public PollBuilder threshold(CommandAudience sender, PollThreshold threshold) {
    alert.broadcastChange(sender, "Poll Threshold", threshold);

    if (threshold == null) {
      this.threshold = PollThreshold.SIMPLE;
      return this;
    }

    this.threshold = threshold;
    return this;
  }

  public Poll build() {
    Poll poll;

    if (question == null) {
      question = endAction.getDefaultQuestion();
    }

    if (duration != null) {
      poll = new TimedPoll(question, creator, threshold, endAction, duration);
    } else {
      poll = new NormalPoll(question, creator, threshold, endAction);
    }
    return poll;
  }

  @Nullable
  public Component getQuestion() {
    return question;
  }

  @Nullable
  public Duration getDuration() {
    return duration;
  }

  public EndAction getEndAction() {
    return endAction;
  }

  public PollThreshold getThreshold() {
    return threshold;
  }

  public boolean canBuild() {
    return question != null || !(endAction instanceof NullEndAction);
  }
}
