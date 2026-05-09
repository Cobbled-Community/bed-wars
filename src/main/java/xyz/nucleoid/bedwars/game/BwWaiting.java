package xyz.nucleoid.bedwars.game;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.bedwars.game.active.BwActive;
import xyz.nucleoid.bedwars.game.config.BwConfig;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public final class BwWaiting {
    private final ServerLevel level;
    private final GameSpace gameSpace;
    private final BwMap map;
    private final BwConfig config;

    private final BwSpawnLogic spawnLogic;

    private final TeamSelectionLobby teamSelection;

    private BwWaiting(ServerLevel level, GameSpace gameSpace, BwMap map, BwConfig config, TeamSelectionLobby teamSelection) {
        this.level = level;
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;
        this.teamSelection = teamSelection;

        this.spawnLogic = new BwSpawnLogic(level, map);
    }

    public static GameOpenProcedure open(GameOpenContext<BwConfig> context) {
        BwConfig config = context.config();
        BwMap map = new BwMapBuilder(config)
                .create(context.server());

        RuntimeLevelConfig levelConfig = new RuntimeLevelConfig()
                .setGenerator(map.getChunkGenerator())
                .setDimensionType(ResourceKey.create(Registries.DIMENSION_TYPE, config.dimension()));

        return context.openWithLevel(levelConfig, (activity, level) -> {
            GameWaitingLobby.addTo(activity, config.players());

            TeamSelectionLobby teamSelection = TeamSelectionLobby.addTo(activity, config.teams());

            BwWaiting waiting = new BwWaiting(level, activity.getGameSpace(), map, config, teamSelection);

            activity.allow(GameRuleType.INTERACTION);

            activity.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);

            activity.listen(GamePlayerEvents.ACCEPT, waiting::onPlayerOffer);
            activity.listen(PlayerDeathEvent.EVENT, waiting::onPlayerDeath);
            activity.listen(PlayerDamageEvent.EVENT, waiting::onPlayerDamage);
        });
    }

    private JoinAcceptorResult onPlayerOffer(JoinAcceptor offer) {
        return offer.teleport(this.level, this.map.getCenterSpawn())
                .thenRunForEach((player, intent) -> this.spawnLogic.respawnPlayer(player, GameType.ADVENTURE));
    }

    private GameResult requestStart() {
        Multimap<GameTeamKey, ServerPlayer> players = HashMultimap.create();
        this.teamSelection.allocate(this.gameSpace.getPlayers(), players::put);

        BwActive.open(this.level, this.gameSpace, this.map, this.config, players);

        return GameResult.ok();
    }

    private EventResult onPlayerDamage(ServerPlayer player, DamageSource source, float amount) {
        return EventResult.DENY;
    }

    private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
        this.spawnLogic.respawnPlayer(player, GameType.ADVENTURE);
        this.spawnLogic.spawnAtCenter(player);
        return EventResult.DENY;
    }
}
