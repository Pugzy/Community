package dev.pgm.community;

public interface CommunityPermissions {

  // TODO: Copy PGM format and style, register groups of permission nodes like moderator/admin/dev
  // etc

  // Root permission node
  String ROOT = "community";

  // Admin - Reserved for restricted features
  String ADMIN = ROOT + ".admin";

  // Moderation:

  // Punishment Types
  String KICK = ROOT + ".kick"; // Access to the /kick command
  String WARN = ROOT + ".warn"; // Access to the /warn command
  String MUTE = ROOT + ".mute"; // Access to the /mute command
  String BAN = ROOT + ".ban"; // Access to the /ban command

  // Punishment related commands
  String LOOKUP = ROOT + ".lookup"; // Access to view own record with /lookup
  String LOOKUP_OTHERS = LOOKUP + ".others"; // Access to lookup other players
  String UNBAN = ROOT + ".pardon"; // Access to the /unban command
  String PUNISH = ROOT + ".punish"; // Access to punishment commands (/rp, /ph)
  String PUNISHMENT_BROADCASTS =
      ROOT + ".view-punishments"; // Access to view when punishments are broadcast silently

  // Sign Logger
  String SIGN_LOG_BROADCASTS = ROOT + ".view-sign-logs"; // Access to view when signs are placed

  // Reports
  String REPORTS = ROOT + ".reports"; // Access to view report broadcast & report history
  String REPORT_BROADCASTS = REPORTS + ".view-broadcasts"; // Access to view report broadcasts

  // Staff
  String RELOAD = ROOT + ".reload";
  String RESTRICTED = ROOT + ".restricted"; // Access to restricted info (e.g IP addresses)

  // Sessions
  String FIND = ROOT + ".find"; // Access to /find friends command
  String FIND_ANYONE = FIND + ".anyone"; // Access to /find anyone

  // Teleports
  String TELEPORT = ROOT + ".teleport"; // Access to teleport to another player
  String TELEPORT_OTHERS = TELEPORT + ".others"; // Access to teleport other players
  String TELEPORT_LOCATION =
      TELEPORT + ".location"; // Access to teleport to a world location (coords)
  String TELEPORT_ALL = TELEPORT + ".all"; // Access to teleport everyone

  // Chat Management
  String CHAT_MANAGEMENT =
      ROOT + ".chat"; // Able to use /chat commands, and exempt during lock & slowmode

  // Network chat
  String CROSS_NETWORK_CHAT = ROOT + ".network-chat";

  // Friends
  String FRIENDSHIP = ROOT + ".friendship"; // Access to /friend commands

  // Nicknames
  String NICKNAME = ROOT + ".nick"; // Access to /nick (random)
  String NICKNAME_SET = NICKNAME + ".set"; // Access to /nick set
  String NICKNAME_OTHER = NICKNAME_SET + ".other"; // Access to /nick setother
  String NICKNAME_CLEAR = NICKNAME_SET + ".clear"; // Access to /nick clear <username>
  String NICKNAME_VIEW = NICKNAME + ".view-skins"; // Access to view normal skins

  // General Staff
  String STAFF =
      ROOT + ".staff"; // Receive staff broadcasts and see disguised players (maybe add a different
  // node later)

  // Freeze
  String FREEZE = ROOT + ".freeze";
  String FREEZE_EXEMPT = FREEZE + ".exempt";
  String FREEZE_FORCE = FREEZE + ".force";

  // Match History
  String MATCH_HISTORY = ROOT + ".match-history";

  // Mutations
  String MUTATION = ROOT + ".mutation"; // Access to /mutate

  // Requests
  String REQUEST = ROOT + ".request"; // Access to /request
  String REQUEST_SPONSOR = REQUEST + ".sponsor"; // Access to /sponsor
  String REQUEST_STAFF = REQUEST + ".staff"; // Access to /requests
  String REQUEST_REFUND = REQUEST + ".refund"; // Receive token refunds when applicable

  String TOKEN = ROOT + ".token"; // Access to view /token
  String TOKEN_DAILY = TOKEN + ".daily"; // Receives token refresh daily
  String TOKEN_WEEKLY = TOKEN + ".weekly"; // Receives token refresh weekly
  String TOKEN_BALANCE = TOKEN + ".view-others"; // Access to view other token balances

  String SPONSOR_COOLDOWN_CUSTOM = "sponsor.cooldown.";

  String VIEW_MAP_COOLDOWNS = ROOT + ".view-map-cooldown";

  // Super Votes
  String SUPER_VOTE = REQUEST + ".super-vote";
  String SUPER_VOTE_BALANCE = SUPER_VOTE + ".balance";

  // Translations
  String TRANSLATE = ROOT + ".translate"; // Access to /translate

  // Events
  String PARTY = ROOT + ".event"; // Access to /event creation
  String PARTY_HOST = PARTY + ".host"; // Given to those who are currently hosting an event
  String PARTY_ADMIN = PARTY + ".admin"; // Administrative event permission

  // Polls
  String POLL = ROOT + ".poll";

  // Squads
  String SQUAD = ROOT + ".squad"; // Access to squad commands
  String SQUAD_CREATE = SQUAD + ".create"; // Can create a squad

  // General Commands
  String FLIGHT = ROOT + ".fly";
  String FLIGHT_SPEED = FLIGHT + ".speed";
  String GAMEMODE = ROOT + ".gamemode";
  String BROADCAST = ROOT + ".broadcast";
  String CONTAINER = ROOT + ".container";
  String MOB_SPAWN = ROOT + ".mob-spawn";

  // Player Selectors
  String SELECTOR = ROOT + ".selector"; // Allow access to targeting more than 1 player
  String ALL_SELECTOR = SELECTOR + ".all"; // * - select everyone
  String RANDOM_SELECTOR = SELECTOR + ".random"; // ? - select a random player
  String TEAM_SELECTOR = SELECTOR + ".team"; // team='Name' - select a match's team

  String VIEW_VANISHED = ROOT + ".vanish.view";

  String OVERRIDE = "pgm.admin"; // Access to override
  // TODO Setup different groups like moderation

}
