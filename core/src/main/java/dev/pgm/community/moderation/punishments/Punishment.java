package dev.pgm.community.moderation.punishments;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.title.Title.title;
import static tc.oc.pgm.util.text.TemporalComponent.briefNaturalApproximate;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import com.google.common.collect.Lists;
import dev.pgm.community.Community;
import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.punishments.types.BanPunishment;
import dev.pgm.community.moderation.punishments.types.ExpirablePunishment;
import dev.pgm.community.moderation.punishments.types.KickPunishment;
import dev.pgm.community.moderation.punishments.types.MutePunishment;
import dev.pgm.community.moderation.punishments.types.TempBanPunishment;
import dev.pgm.community.moderation.punishments.types.UsernameBanPunishment;
import dev.pgm.community.moderation.punishments.types.WarnPunishment;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.Sounds;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.UsernameFormatUtils;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextTranslations;

public class Punishment implements Comparable<Punishment> {

  private PunishmentType type;

  private UUID punishmentId;
  private UUID targetId;
  private @Nullable UUID issuerId;
  private String reason;
  private long timeIssued;
  private boolean active;
  private @Nullable Duration duration;

  private long lastUpdated;
  private @Nullable UUID lastUpdatedBy;

  private String service;

  public Punishment() {}

  public Punishment(
      PunishmentType type,
      UUID punishmentId,
      UUID targetId,
      @Nullable UUID issuerId,
      String reason,
      Duration duration,
      long timeIssued,
      boolean active,
      long lastUpdated,
      @Nullable UUID lastUpdatedBy,
      String service) {
    this.type = type;
    this.punishmentId = punishmentId;
    this.targetId = targetId;
    this.issuerId = issuerId;
    this.reason = reason;
    this.timeIssued = timeIssued;
    this.active = active;
    this.lastUpdated = lastUpdated;
    this.lastUpdatedBy = lastUpdatedBy;
    this.service = service;
    this.duration = duration;
  }

  public boolean punish(boolean silent) {
    return false;
  }

  public PunishmentType getType() {
    return type;
  }

  public UUID getId() {
    return punishmentId;
  }

  public UUID getTargetId() {
    return targetId;
  }

  @Nullable
  public UUID getIssuerId() {
    return issuerId;
  }

  public boolean isConsole() {
    return getIssuerId() == null;
  }

  public String getReason() {
    return reason;
  }

  public Instant getTimeIssued() {
    return Instant.ofEpochMilli(timeIssued);
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Instant getLastUpdated() {
    return Instant.ofEpochMilli(lastUpdated);
  }

  public @Nullable UUID getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public String getService() {
    return service;
  }

  public Duration getDuration() {
    return duration;
  }

  public boolean wasUpdated() {
    return !getTimeIssued().equals(getLastUpdated());
  }

  public boolean isBan() {
    return getType() == PunishmentType.BAN || getType() == PunishmentType.TEMP_BAN;
  }

  @Override
  public int compareTo(Punishment o) {
    return -getTimeIssued().compareTo(o.getTimeIssued());
  }

  public ModerationConfig getConfig() {
    return (ModerationConfig) Community.get().getFeatures().getModeration().getConfig();
  }

  public Optional<Player> getTargetPlayer() {
    return Optional.ofNullable(Bukkit.getPlayer(getTargetId()));
  }

  public boolean kick(boolean silent) {
    Optional<Player> player = getTargetPlayer();
    if (player.isPresent()) {
      player
          .get()
          .getPlayer()
          .kickPlayer(formatPunishmentScreen(
              getConfig(),
              isConsole()
                  ? UsernameFormatUtils.CONSOLE_NAME
                  : PlayerComponent.player(getIssuerId(), NameStyle.FANCY),
              silent));
      return true;
    }
    return false;
  }

  private static final Component WARN_SYMBOL = text(" \u26a0 ", NamedTextColor.YELLOW);

  /*
   * Sends a formatted title and plays a sound warning a user of their actions
   */
  public void sendWarning(Audience target, String reason) {
    Component titleWord = translatable("misc.warning", NamedTextColor.DARK_RED);
    Component title =
        text().append(WARN_SYMBOL).append(titleWord).append(WARN_SYMBOL).build();
    Component subtitle;
    if (Duration.between(getTimeIssued(), Instant.now()).getSeconds() >= 60) {
      subtitle = text()
          .append(TemporalComponent.relativePastApproximate(getTimeIssued())
              .color(NamedTextColor.YELLOW)
              .append(text(": ", NamedTextColor.YELLOW)))
          .append(text(reason, NamedTextColor.GOLD))
          .build();
    } else {
      subtitle = text(reason, NamedTextColor.GOLD);
    }

    target.showTitle(title(
        title, subtitle, Times.of(Ticks.duration(5), Ticks.duration(200), Ticks.duration(10))));
    target.playSound(Sounds.WARN_SOUND);
  }

  public Component formatBroadcast(Component issuer, Component target, String formatString) {
    Map<String, Component> placeholders = new HashMap<>();
    placeholders.put("%issuer%", issuer);
    placeholders.put("%type%", getType().getPunishmentPrefix());
    placeholders.put("%full_type%", getType().getPunishmentPrefix());
    if (this.getDuration() != null) {
      placeholders.put("%full_type%", formatTimeComponent());
    }
    placeholders.put("%target%", target);
    placeholders.put("%reason%", text(getReason(), NamedTextColor.RED));
    placeholders.put("%div%", BroadcastUtils.BROADCAST_DIV);

    TextComponent.Builder builder = text();

    String[] parts = formatString.split(" ");

    for (String part : parts) {
      if (placeholders.containsKey(part)) {
        builder.append(placeholders.get(part));
      } else {
        builder.append(text(part));
      }
    }

    return builder.build();
  }

  public Component formatTimeComponent() {
    Duration length = this.getDuration();
    String time = TextTranslations.translateLegacy(duration(length));

    // TODO: Clean up (There's most likely an easier way to do this)
    String[] timeParts = time.split(" ");
    boolean seconds = timeParts.length == 2 && timeParts[1].toLowerCase().startsWith("s");
    if (!seconds && time.contains("s")) {
      time = time.substring(0, time.lastIndexOf('s'));
    } else if (time.lastIndexOf("s") == time.length() - 1) {
      time = time.substring(0, time.length() - 1);
    }
    return getType().getPunishmentPrefix(text(time, NamedTextColor.GOLD));
  }

  public Component getExpireDateMessage() {
    Duration banLength = ((ExpirablePunishment) this).getDuration();
    Duration timeSince = Duration.between(getTimeIssued(), Instant.now());
    Duration remaining = banLength.minus(timeSince);
    Component timeLeft = briefNaturalApproximate(remaining);
    return translatable("moderation.screen.expires", NamedTextColor.GRAY, timeLeft);
  }

  /** Formats a string for multi-line kick message */
  public String formatPunishmentScreen(
      ModerationConfig config, Component issuerName, boolean disguised) {
    List<Component> lines = Lists.newArrayList();

    lines.add(empty());
    lines.add(getType()
        .getScreenComponent(
            Duration.between(getTimeIssued(), Instant.now()).getSeconds() >= 60
                ? text()
                    .append(TemporalComponent.relativePastApproximate(getTimeIssued())
                        .color(NamedTextColor.YELLOW)
                        .append(text(": ", NamedTextColor.YELLOW)))
                    .append(text(reason, NamedTextColor.RED))
                    .build()
                : text(reason, NamedTextColor.RED)));

    // If punishment expires, display when
    if (this instanceof ExpirablePunishment) {
      lines.add(empty());
      lines.add(getExpireDateMessage());
    }

    // Staff Sign-off
    if (!disguised && config.isStaffSignoff()) {
      lines.add(empty());
      lines.add(translatable("moderation.screen.signoff", NamedTextColor.GRAY, issuerName));
    }

    // Alert match banned players they won't be able to participate
    if (getType() == PunishmentType.KICK && config.getMatchBanDuration() != null) {
      lines.add(empty());
      lines.add(text("You may rejoin, but will be unable to participate for ")
          .append(duration(config.getMatchBanDuration(), NamedTextColor.YELLOW))
          .color(NamedTextColor.GRAY));
    }

    // Alert name banned players when their ban will be lifted
    if (getType() == PunishmentType.NAME_BAN) {
      lines.add(empty());
      lines.add(text("Please change your username", NamedTextColor.GRAY));
      lines.add(text("Once complete, ban will automatically be removed", NamedTextColor.GRAY));
    }

    // Link to rules for review by player
    if (config.getRulesLink() != null && !config.getRulesLink().isEmpty()) {
      Component rules = text(config.getRulesLink(), NamedTextColor.BLUE, TextDecoration.UNDERLINED);

      lines.add(empty());
      lines.add(
          translatable("moderation.screen.rulesLink", NamedTextColor.GRAY, rules)); // Link to rules
    }

    // Configurable last line (for appeal message or etc)
    if (config.getAppealMessage() != null
        && !config.getAppealMessage().isEmpty()
        && getType().equals(PunishmentType.BAN)) {
      lines.add(empty());
      lines.add(text(config.getAppealMessage()));
    }

    lines.add(empty());

    return MessageUtils.formatKickScreenMessage(Community.get().getServerName(), lines);
  }

  public static Punishment of(Punishment punishment) {
    return of(
        punishment.getId(),
        punishment.getTargetId(),
        punishment.getIssuerId(),
        punishment.getReason(),
        punishment.getTimeIssued().toEpochMilli(),
        punishment.getDuration(),
        punishment.getType(),
        punishment.isActive(),
        punishment.getLastUpdated().toEpochMilli(),
        punishment.getLastUpdatedBy(),
        punishment.getService());
  }

  public static Punishment of(
      UUID id,
      UUID target,
      @Nullable UUID issuer,
      String reason,
      long time,
      @Nullable Duration length,
      PunishmentType type,
      boolean active,
      long lastUpdated,
      @Nullable UUID lastUpdatedBy,
      String service) {
    switch (type) {
      case WARN:
        return new WarnPunishment(
            id, target, issuer, reason, time, active, lastUpdated, lastUpdatedBy, service);
      case MUTE:
        return new MutePunishment(
            id, target, issuer, reason, time, length, active, lastUpdated, lastUpdatedBy, service);
      case KICK:
        return new KickPunishment(
            id, target, issuer, reason, time, active, lastUpdated, lastUpdatedBy, service);
      case TEMP_BAN:
        return new TempBanPunishment(
            id, target, issuer, reason, time, length, active, lastUpdated, lastUpdatedBy, service);
      case BAN:
        return new BanPunishment(
            id, target, issuer, reason, time, active, lastUpdated, lastUpdatedBy, service);
      case NAME_BAN:
        return new UsernameBanPunishment(
            id, target, issuer, reason, time, active, lastUpdated, lastUpdatedBy, service);
    }
    return null;
  }
}
