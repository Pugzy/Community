package dev.pgm.community.requests.commands.sponsor;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TemporalComponent.duration;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.requests.RequestConfig;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.SponsorRequest;
import dev.pgm.community.requests.commands.sponsor.TokenCommands.TokenRefreshAmount;
import dev.pgm.community.requests.feature.RequestFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.PGMUtils.MapSizeBounds;
import dev.pgm.community.utils.PaginatedComponentResults;
import dev.pgm.community.utils.VisibilityUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.lib.org.incendo.cloud.annotation.specifier.Greedy;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Default;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Flag;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.suggestion.Suggestions;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandContext;
import tc.oc.pgm.util.LiquidMetal;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.named.MapNameStyle;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

@Command("sponsor")
public class SponsorCommands extends CommunityCommand {

  private final RequestFeature requests;

  public SponsorCommands() {
    this.requests = Community.get().getFeatures().getRequests();
  }

  @Command("")
  public void info(CommandAudience audience, Player player) {
    Component header = TextFormatter.horizontalLineHeading(
        audience.getSender(),
        text("Sponsor", NamedTextColor.YELLOW, TextDecoration.BOLD),
        NamedTextColor.GOLD,
        TextFormatter.MAX_CHAT_WIDTH);

    Component footer =
        TextFormatter.horizontalLine(NamedTextColor.GOLD, TextFormatter.MAX_CHAT_WIDTH);

    RequestConfig config = ((RequestConfig) requests.getConfig());

    requests.getRequestProfile(player.getUniqueId()).thenAcceptAsync(profile -> {
      Component tokenBalance = text()
          .append(RequestFeature.TOKEN)
          .append(text(" Token balance: "))
          .append(text(profile.getSponsorTokens(), NamedTextColor.YELLOW))
          .append(text(" / "))
          .append(text(config.getMaxTokens(), NamedTextColor.GOLD))
          .append(renderExtraInfo(player, profile))
          .color(NamedTextColor.GRAY)
          .clickEvent(ClickEvent.runCommand("/tokens"))
          .hoverEvent(HoverEvent.showText(text("Click to view tokens", NamedTextColor.GRAY)))
          .build();

      Component buttons = text()
          .append(text("     "))
          .append(button(
              "Map List",
              NamedTextColor.DARK_AQUA,
              "/sponsor maps 1",
              "Click to view available maps as a chat list"))
          .append(text("       "))
          .append(button(
              "Map Menu",
              NamedTextColor.GREEN,
              "/sponsor menu",
              "Click to view available maps as a GUI menu"))
          .append(text("       "))
          .append(button(
              "Queue",
              NamedTextColor.DARK_GREEN,
              "/sponsor queue",
              "View a list of waiting sponsor requests ("
                  + ChatColor.YELLOW
                  + requests.getSponsorQueue().size()
                  + ChatColor.GRAY
                  + ")"))
          .build();

      audience.sendMessage(header);

      // TOKEN BALANCE
      audience.sendMessage(tokenBalance);

      // Existing request status
      requests.getPendingSponsor(player.getUniqueId()).ifPresent(sponsor -> {
        int queueIndex = requests.queueIndex(sponsor);
        boolean next = queueIndex == 0;

        Component current = text("Current Request: ", NamedTextColor.GRAY, TextDecoration.BOLD)
            .append(button(
                "Cancel", NamedTextColor.RED, "/sponsor cancel", "Click to cancel this request"));
        Component queue = next
            ? text("(Will be added to the next vote)", NamedTextColor.GRAY, TextDecoration.ITALIC)
            : text()
                .append(text("(Queue location: "))
                .append(text("#" + queueIndex, NamedTextColor.YELLOW))
                .append(text(")"))
                .color(NamedTextColor.GRAY)
                .hoverEvent(HoverEvent.showText(
                    text("Your request's location in the queue", NamedTextColor.GRAY)))
                .build();

        audience.sendMessage(empty());
        audience.sendMessage(current);
        audience.sendMessage(text()
            .append(text(" - ", NamedTextColor.YELLOW))
            .append(sponsor.getMap().getStyledName(MapNameStyle.COLOR_WITH_AUTHORS))
            .build());
        audience.sendMessage(queue);
      });

      audience.sendMessage(empty());

      // Cooldown message
      if (!requests.canSponsor(player)) {
        audience.sendMessage(text()
            .append(text("Cooldown", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(text(": ", NamedTextColor.GRAY))
            .append(MessageUtils.formatTimeLeft(
                ((RequestConfig) requests.getConfig()).getSponsorCooldown(player),
                profile.getLastSponsorTime(),
                NamedTextColor.RED))
            .color(NamedTextColor.GRAY)
            .hoverEvent(HoverEvent.showText(
                text("Time until you can sponsor another map", NamedTextColor.GRAY)))
            .build());
        audience.sendMessage(empty());
      }

      // [Maps] [Queue]
      audience.sendMessage(buttons);

      audience.sendMessage(footer);
    });
  }

  @Command("request <map>")
  @CommandDescription("Sponsor a map request")
  public void sponsor(
      CommandAudience audience, Player sender, @Argument("map") @Greedy MapInfo map) {
    requests.sponsor(sender, map);
  }

  @Command("cancel")
  public void cancel(CommandAudience audience, Player sender) {
    if (requests.cancelSponsorRequest(sender.getUniqueId())) {
      audience.sendMessage(text("Removed sponsor request!", NamedTextColor.GREEN));
    } else {
      audience.sendWarning(text("You don't have any pending sponsor requests to cancel"));
    }
  }

  @Command("menu")
  public void menu(CommandAudience audience, Player sender) {
    requests.openMenu(sender);
  }

  @Command("maps [page]")
  @CommandDescription("View a list of maps which can be sponsored")
  public void viewMapList(
      CommandAudience audience,
      @Argument("page") @Default("1") int page,
      @Flag(value = "tags", aliases = "t", repeatable = true, suggestions = "maptags")
          List<String> tags,
      @Flag(value = "author", aliases = "a") String author,
      @Flag(value = "name", aliases = "n") String name) {
    Stream<MapInfo> search = requests.getAvailableSponsorMaps().stream();

    if (!tags.isEmpty()) {
      final Map<Boolean, Set<String>> tagSet = tags.stream()
          .flatMap(t -> Arrays.stream(t.split(",")))
          .map(String::toLowerCase)
          .map(String::trim)
          .collect(Collectors.partitioningBy(
              s -> s.startsWith("!"),
              Collectors.mapping(
                  (String s) -> s.startsWith("!") ? s.substring(1) : s, Collectors.toSet())));
      search = search.filter(map -> matchesTags(map, tagSet.get(false), tagSet.get(true)));
    }

    if (author != null) {
      String query = StringUtils.normalize(author);
      search = search.filter(map -> matchesAuthor(map, query));
    }

    Set<MapInfo> maps = search.collect(Collectors.toCollection(TreeSet::new));

    int resultsPerPage = 8;
    int pages = (maps.size() + resultsPerPage - 1) / resultsPerPage;

    MapSizeBounds bounds = requests.getCurrentMapSizeBounds();
    Component hover = text()
        .append(text("Current online player range", NamedTextColor.DARK_AQUA))
        .appendSpace()
        .append(text("(", NamedTextColor.GRAY))
        .append(text(bounds.getLowerBound(), NamedTextColor.GOLD))
        .append(text("-", NamedTextColor.GRAY))
        .append(text(bounds.getUpperBound(), NamedTextColor.GOLD))
        .append(text(")", NamedTextColor.GRAY))
        .build();
    Component title = text()
        .append(text("Available Maps"))
        .hoverEvent(HoverEvent.showText(hover))
        .build();

    Component paginated = TextFormatter.paginate(
        title, page, pages, NamedTextColor.DARK_AQUA, NamedTextColor.AQUA, true);

    Component formattedTitle = TextFormatter.horizontalLineHeading(
        audience.getSender(), paginated, NamedTextColor.DARK_PURPLE, 250);

    new PaginatedComponentResults<MapInfo>(formattedTitle, resultsPerPage) {
      @Override
      public Component format(MapInfo map, int index) {
        Component mapName = map.getStyledName(MapNameStyle.COLOR_WITH_AUTHORS)
            .clickEvent(ClickEvent.runCommand("/map " + map.getName()))
            .hoverEvent(HoverEvent.showText(translatable(
                "command.maps.hover", NamedTextColor.GRAY, map.getStyledName(MapNameStyle.COLOR))));

        return text()
            .append(text((index + 1) + ". "))
            .append(mapName)
            .append(renderSponsorButton(audience.getSender(), map))
            .color(NamedTextColor.GRAY)
            .build();
      }

      @Override
      public Component formatEmpty() {
        return text("There are no available maps to sponsor!", NamedTextColor.RED);
      }
    }.display(audience.getAudience(), maps, page);

    // Add page button when more than 1 page
    if (pages > 1) {
      TextComponent.Builder buttons = text();

      if (page > 1) {
        buttons.append(text()
            .append(BroadcastUtils.LEFT_DIV.color(NamedTextColor.GOLD))
            .append(text(" Previous Page", NamedTextColor.BLUE))
            .hoverEvent(
                HoverEvent.showText(text("Click to view previous page", NamedTextColor.GRAY)))
            .clickEvent(ClickEvent.runCommand("/sponsor maps " + (page - 1))));
      }

      if (page > 1 && page < pages) {
        buttons.append(text(" | ", NamedTextColor.DARK_GRAY));
      }

      if (page < pages) {
        buttons.append(text()
            .append(text("Next Page ", NamedTextColor.BLUE))
            .append(BroadcastUtils.RIGHT_DIV.color(NamedTextColor.GOLD))
            .hoverEvent(HoverEvent.showText(text("Click to view next page", NamedTextColor.GRAY)))
            .clickEvent(ClickEvent.runCommand("/sponsor maps " + (page + 1))));
      }
      audience.sendMessage(TextFormatter.horizontalLineHeading(
          audience.getSender(), buttons.build(), NamedTextColor.DARK_PURPLE, 250));
    }
  }

  @Suggestions("maptags")
  public List<String> suggestMapTags(CommandContext<CommandSender> sender, String input) {
    int commaIdx = input.lastIndexOf(',');

    final String prefix = input.substring(0, commaIdx == -1 ? 0 : commaIdx + 1);
    final String toComplete =
        input.substring(commaIdx + 1).toLowerCase(Locale.ROOT).replace("!", "");

    return MapTag.getAllTagIds().stream()
        .filter(mt -> LiquidMetal.match(mt, toComplete))
        .flatMap(tag -> Stream.of(prefix + tag, prefix + "!" + tag))
        .collect(Collectors.toList());
  }

  private static boolean matchesTags(
      MapInfo map, Collection<String> posTags, Collection<String> negTags) {
    int matches = 0;
    for (MapTag tag : assertNotNull(map).getTags()) {
      if (negTags != null && negTags.contains(tag.getId())) {
        return false;
      }
      if (posTags != null && posTags.contains(tag.getId())) {
        matches++;
      }
    }
    return posTags == null || matches == posTags.size();
  }

  private static boolean matchesAuthor(MapInfo map, String query) {
    for (Contributor contributor : map.getAuthors()) {
      if (StringUtils.normalize(contributor.getNameLegacy()).contains(query)) {
        return true;
      }
    }
    return false;
  }

  @Command("queue [page]")
  @CommandDescription("View the sponsored maps queue")
  public void viewQueue(CommandAudience audience, @Argument("page") @Default("1") int page) {
    Queue<SponsorRequest> queue = requests.getSponsorQueue();

    int resultsPerPage = ((RequestConfig) requests.getConfig()).getMaxQueue();
    int pages = (queue.size() + resultsPerPage - 1) / resultsPerPage;

    Component paginated = TextFormatter.paginate(
        text("Sponsor Queue"), page, pages, NamedTextColor.DARK_AQUA, NamedTextColor.AQUA, true);

    Component formattedTitle = TextFormatter.horizontalLineHeading(
        audience.getSender(), paginated, NamedTextColor.DARK_PURPLE, 250);

    new PaginatedComponentResults<SponsorRequest>(formattedTitle, resultsPerPage) {
      @Override
      public Component format(SponsorRequest sponsor, int index) {
        MapInfo map = sponsor.getMap();
        Component mapName = map.getStyledName(MapNameStyle.COLOR)
            .clickEvent(ClickEvent.runCommand("/map " + map.getName()))
            .hoverEvent(HoverEvent.showText(translatable(
                "command.maps.hover", NamedTextColor.GRAY, map.getStyledName(MapNameStyle.COLOR))));

        Component playerName = VisibilityUtils.isDisguised(sponsor.getPlayerId())
            ? empty()
            : text()
                .append(BroadcastUtils.BROADCAST_DIV)
                .append(player(sponsor.getPlayerId(), NameStyle.FANCY))
                .build();

        return text()
            .append(text((index + 1) + ". "))
            .append(mapName)
            .append(playerName)
            .color(NamedTextColor.GRAY)
            .build();
      }

      @Override
      public Component formatEmpty() {
        return text("There are no maps in the sponsor queue!", NamedTextColor.RED);
      }
    }.display(audience.getAudience(), queue, page);
  }

  private Component renderExtraInfo(Player player, RequestProfile profile) {
    RequestConfig config = (RequestConfig) requests.getConfig();
    TokenRefreshAmount info =
        TokenCommands.getTimeLeft(player, profile.getLastTokenRefreshTime(), requests);

    // If token refresh is disabled, display cooldown
    if (config.getDailyTokenAmount() == 0) {
      return text()
          .append(text(" | "))
          .append(text("Cooldown: "))
          .append(duration(config.getSponsorCooldown(player), NamedTextColor.GOLD))
          .hoverEvent(HoverEvent.showText(
              text("This is your cooldown time between sponsor requests", NamedTextColor.GRAY)))
          .color(NamedTextColor.GRAY)
          .build();
    }

    if (info.getDuration() != null) {
      boolean canClaim = profile.getSponsorTokens() < config.getMaxTokens();

      if (canClaim) {
        return text()
            .append(text(" | "))
            .append(text("Next token ("))
            .append(text("+" + info.getAmount(), NamedTextColor.GREEN, TextDecoration.BOLD))
            .append(text("): "))
            .append(
                info.getDuration().isNegative()
                    ? text("Now! Please rejoin")
                    : duration(info.getDuration(), NamedTextColor.YELLOW))
            .build();
      } else {
        return text()
            .append(text(" | No tokens to claim"))
            .hoverEvent(HoverEvent.showText(text(
                "You have the max amount of sponsor tokens! To claim more you need to spend some first.",
                NamedTextColor.RED)))
            .build();
      }
    }

    return empty();
  }

  private Component renderSponsorButton(CommandSender sender, MapInfo map) {
    if (sender instanceof Player) {
      Player player = (Player) sender;
      if (requests.getCached(player.getUniqueId()) != null) {
        RequestProfile profile = requests.getCached(player.getUniqueId());
        if (profile.getSponsorTokens() > 0) {
          return text()
              .append(BroadcastUtils.BROADCAST_DIV)
              .append(text("["))
              .append(RequestFeature.SPONSOR)
              .append(text("]"))
              .hoverEvent(HoverEvent.showText(text("Click to sponsor ", NamedTextColor.GRAY)
                  .append(map.getStyledName(MapNameStyle.COLOR))))
              .clickEvent(ClickEvent.runCommand("/sponsor request " + map.getName()))
              .color(NamedTextColor.GRAY)
              .build();
        }
      }
    }
    return empty();
  }
}
