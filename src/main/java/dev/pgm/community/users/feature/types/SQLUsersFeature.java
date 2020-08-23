package dev.pgm.community.users.feature.types;

import dev.pgm.community.database.DatabaseConnection;
import dev.pgm.community.users.UserProfile;
import dev.pgm.community.users.UsersConfig;
import dev.pgm.community.users.feature.UsersFeatureBase;
import dev.pgm.community.users.services.AddressHistoryService;
import dev.pgm.community.users.services.SQLUserService;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SQLUsersFeature extends UsersFeatureBase {

  private SQLUserService service;
  private AddressHistoryService addresses;

  public SQLUsersFeature(Configuration config, Logger logger, DatabaseConnection database) {
    super(new UsersConfig(config), logger);
    this.service = new SQLUserService(database);
    this.addresses = new AddressHistoryService(database);
  }

  @Override
  public CompletableFuture<UserProfile> getStoredProfile(String query) {
    return service.query(query);
  }

  @Override
  public CompletableFuture<UserProfile> getStoredProfile(UUID id) {
    UserProfile cached = super.getProfile(id);
    if (cached == null) {
      return service
          .query(id.toString())
          .thenApplyAsync(
              profile -> {
                if (profile != null) {
                  profiles.put(id, profile); // Cache profile
                }
                return profile;
              });
    }
    return CompletableFuture.completedFuture(cached);
  }

  @Override
  public CompletableFuture<String> getStoredUsername(UUID id) {
    String cached = super.getUsername(id);

    if (cached == null) {
      return service
          .query(id.toString())
          .thenApplyAsync(
              profile -> {
                if (profile != null && profile.getUsername() != null) {
                  this.setName(id, profile.getUsername());
                }
                return profile.getUsername();
              });
    }

    return CompletableFuture.completedFuture(cached);
  }

  @Override
  public CompletableFuture<Optional<UUID>> getStoredId(String username) {
    Optional<UUID> cached = super.getId(username);
    if (!cached.isPresent()) {
      return service
          .query(username)
          .thenApplyAsync(
              profile -> {
                UUID id = null;
                if (profile != null && profile.getId() != null) {
                  this.setName(profile.getId(), profile.getUsername());
                  id = profile.getId();
                }
                return Optional.ofNullable(id);
              });
    }

    return CompletableFuture.completedFuture(cached);
  }

  @Override
  public CompletableFuture<Set<String>> getKnownIPs(UUID playerId) {
    return addresses.getKnownIps(playerId);
  }

  @Override
  public CompletableFuture<Set<UUID>> getAlternateAccounts(UUID playerId) {
    return addresses.getAlternateAccounts(playerId);
  }

  @Override
  public void onLogin(AsyncPlayerPreLoginEvent login) {
    final UUID id = login.getUniqueId();
    final String name = login.getName();
    final String address = login.getAddress().getHostAddress();
    setName(id, name); // Check for username update

    profiles.invalidate(
        login.getUniqueId()); // Removed cached profile upon every login, so we get up to date info
    service
        .login(id, name, address)
        .thenAcceptAsync(profile -> profiles.put(id, profile)); // Login save
    addresses.trackIp(id, address); // Track IP
  }

  @Override
  public void onLogout(PlayerQuitEvent event) {
    service.logout(event.getPlayer().getUniqueId());
  }
}
