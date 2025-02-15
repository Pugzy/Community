package dev.pgm.community.requests.feature;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.feature.Feature;
import dev.pgm.community.requests.MapCooldown;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.SponsorRequest;
import dev.pgm.community.utils.PGMUtils.MapSizeBounds;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.map.MapInfo;

public interface RequestFeature extends Feature {

  static final String TOKEN_SYMBOL = "✪";
  static final Component TOKEN = text(TOKEN_SYMBOL, NamedTextColor.GOLD);
  static final Component SPONSOR = text("+", NamedTextColor.GREEN, TextDecoration.BOLD);

  /**
   * Submit a map request
   *
   * @param player the requester
   * @param map the requested map
   */
  void request(Player player, MapInfo map); // For requesting a map

  /**
   * Submit a sponsor request
   *
   * @param player the sponsor
   * @param map the requested map to sponsor
   */
  void sponsor(Player player, MapInfo map); // For premium sponsor auto-add

  /**
   * Activate a super vote for the current map vote
   *
   * @param player the supervoter
   */
  void superVote(Player player);

  /**
   * Get whether the provided playerId can submit a request
   *
   * @param playerId {@link UUID} of the requester
   * @return true if player can submit a request, false if not
   */
  boolean canRequest(UUID playerId); // If player has cooldown

  /**
   * Get whether the provided player can submit a sponsor request
   *
   * @param player
   * @return true if player can submit a sponsor request, false if not
   */
  boolean canSponsor(Player player); // If player can sponsor

  /**
   * Get whether the provided player can activate a super vote
   *
   * @param player
   * @return true if the player can activate a super vote, false if not
   */
  boolean canSuperVote(Player player);

  /**
   * Get whether the provided player has activated a super vote
   *
   * @param player
   * @return true if the player has activated a super vote, false if not
   */
  boolean isSuperVoteActive(Player player);

  /**
   * Submit a database update to the provided {@link RequestProfile}
   *
   * @param profile the request profile
   */
  void update(RequestProfile profile);

  /**
   * Get the request counts for each submitted map
   *
   * @return map of {@link MapInfo} and how many requests each has
   */
  Map<MapInfo, Integer> getRequests();

  /**
   * Get the {@link RequestProfile} associated with the given {@link UUID}
   *
   * @param playerId The player id
   * @return the {@link RequestProfile}
   */
  CompletableFuture<RequestProfile> getRequestProfile(UUID playerId);

  /**
   * Remove all requests for the provided {@link MapInfo}
   *
   * @param mapName The map to clear requests for
   * @return the total number of requests removed
   */
  int clearRequests(MapInfo mapName);

  /** Remove all player requests */
  void clearAllRequests();

  /** Toggle requests */
  boolean isAccepting();

  void toggleAccepting();

  /**
   * Get a set of {@link UUID}s who have requested the provided {@link MapInfo}
   *
   * @param map The map
   * @return A set of {@link UUID}s
   */
  Set<UUID> getRequesters(MapInfo map);

  /**
   * Get the cached {@link RequestProfile} of the provided {@link UUID} or null if not found
   *
   * @param playerId the player id
   * @return A {@link RequestProfile} or null
   */
  RequestProfile getCached(UUID playerId);

  CompletableFuture<RequestProfile> onLogin(PlayerJoinEvent event);

  /**
   * Get the current {@link SponsorRequest} or null if none
   *
   * @return the current {@link SponsorRequest} or null if none
   */
  @Nullable
  SponsorRequest getCurrentSponsor();

  /**
   * Get a queue of submitted {@link SponsorRequest}
   *
   * @return a {@link Queue}
   */
  Queue<SponsorRequest> getSponsorQueue();

  /**
   * Removes any sponsor requests for the given {@link UUID}
   *
   * @param playerId UUID of the player
   * @return Whether a sponsor request was found and removed
   */
  boolean cancelSponsorRequest(UUID playerId);

  Optional<SponsorRequest> getPendingSponsor(UUID playerId);

  int queueIndex(SponsorRequest request);

  boolean hasMapCooldown(MapInfo map);

  Map<MapInfo, MapCooldown> getMapCooldowns();

  void openMenu(Player player);

  List<MapInfo> getAvailableSponsorMaps();

  MapSizeBounds getCurrentMapSizeBounds();

  int getStandardExtraVoteLevel(Player player);

  int getMultipliedExtraVoteLevel(Player player);
}
