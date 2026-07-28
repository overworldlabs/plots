package dev.stoshe.plots.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.model.Plot;
import dev.stoshe.plots.util.ChatUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Player picker popup. Lists online players and allows typing a name to either
 * trust the player or transfer plot ownership to them.
 */
public class PlotPlayerPickerPage extends InteractiveCustomUIPage<PlotPlayerPickerPage.PageData> {

    private static final int MAX_ROWS = 10;

    public enum Mode {
        TRUST, TRANSFER
    }

    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Plots plugin;
    private final Mode mode;
    private String searchInput;
    private String error;
    private List<PlayerEntry> displayed = new ArrayList<>();

    private PlotPlayerPickerPage(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld, @Nonnull Plots plugin,
            @Nonnull Mode mode, @Nonnull String searchInput, String error) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.mode = mode;
        this.searchInput = safe(searchInput);
        this.error = error;
    }

    public static PlotPlayerPickerPage create(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld,
            @Nonnull Plots plugin, @Nonnull Mode mode) {
        return new PlotPlayerPickerPage(playerRef, sourceWorld, plugin, mode, "", null);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PlotPlayerPicker.ui");
        commandBuilder.set("#PickerTitle.Text", this.mode == Mode.TRUST ? "TRUST PLAYER" : "TRANSFER PLOT");
        commandBuilder.set("#PickerSubtitle.Text", this.mode == Mode.TRUST
                ? "Pick a player to trust, or type a name and submit."
                : "Pick the new owner, or type a name and submit.");
        commandBuilder.set("#PickerSearchField.Value", this.searchInput);
        commandBuilder.set("#PickerSubmitButton.Text", "Submit");
        commandBuilder.set("#PickerBackButton.Text", "Back");

        boolean hasError = this.error != null && !this.error.isBlank();
        commandBuilder.set("#PickerError.Visible", hasError);
        commandBuilder.set("#PickerError.Text", hasError ? this.error : "");

        this.displayed = listOnlinePlayers();
        commandBuilder.set("#PickerCountLabel.Text", "Online players: " + this.displayed.size());
        commandBuilder.set("#PickerEmptyLabel.Visible", this.displayed.isEmpty());

        String actionLabel = this.mode == Mode.TRUST ? "Trust" : "Transfer";
        for (int i = 0; i < MAX_ROWS; i++) {
            if (i < this.displayed.size()) {
                PlayerEntry entry = this.displayed.get(i);
                commandBuilder.set("#PickerRow" + i + ".Visible", true);
                commandBuilder.set("#PickerName" + i + ".Text", entry.name());
                commandBuilder.set("#PickerAction" + i + ".Text", actionLabel);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#PickerAction" + i,
                        EventData.of("Action", "Select").append("Param", String.valueOf(i)), false);
            } else {
                commandBuilder.set("#PickerRow" + i + ".Visible", false);
            }
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#PickerSearchField",
                EventData.of("@SearchInput", "#PickerSearchField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#PickerSubmitButton",
                EventData.of("Action", "Submit").append("@SearchInput", "#PickerSearchField.Value"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#PickerBackButton",
                EventData.of("Action", "Back"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        if (data.searchInput != null) {
            this.searchInput = data.searchInput;
        }
        String action = safe(data.action).trim();
        World world = resolveWorld(player);
        switch (action) {
            case "Back" -> PlotMenuPage.open(player, ref, store, this.playerRef, world, this.plugin);
            case "Submit" -> {
                String typed = safe(this.searchInput).trim();
                if (typed.isEmpty()) {
                    reopenWithError(player, ref, store, "Enter a player name.");
                    return;
                }
                PlayerRef target = Universe.get().getPlayerByUsername(typed, NameMatching.EXACT);
                if (target != null) {
                    perform(player, ref, store, world, target.getUuid(), target.getUsername());
                    return;
                }
                // Fall back to the known-players directory for offline players.
                UUID offlineUuid = this.plugin.getKnownPlayers() != null
                        ? this.plugin.getKnownPlayers().resolveUuid(typed)
                        : null;
                if (offlineUuid != null) {
                    String offlineName = this.plugin.getKnownPlayers().getName(offlineUuid);
                    perform(player, ref, store, world, offlineUuid, offlineName != null ? offlineName : typed);
                    return;
                }
                reopenWithError(player, ref, store, "Player '" + typed + "' not found.");
            }
            case "Select" -> {
                int index;
                try {
                    index = Integer.parseInt(safe(data.param).trim());
                } catch (NumberFormatException ex) {
                    reopen(player, ref, store);
                    return;
                }
                if (index < 0 || index >= this.displayed.size()) {
                    reopen(player, ref, store);
                    return;
                }
                PlayerEntry entry = this.displayed.get(index);
                perform(player, ref, store, world, entry.uuid(), entry.name());
            }
            default -> reopen(player, ref, store);
        }
    }

    private void perform(Player player, Ref<EntityStore> ref, Store<EntityStore> store, World world, UUID targetUuid,
            String targetName) {
        Plot plot = resolvePlot(ref, store);
        if (plot == null) {
            player.getPlayerRef().sendMessage(ChatUtil.error("You are not standing on a plot."));
            PlotMenuPage.open(player, ref, store, this.playerRef, world, this.plugin);
            return;
        }
        if (targetUuid == null) {
            reopenWithError(player, ref, store, "Could not resolve player.");
            return;
        }
        if (targetUuid.equals(this.playerRef.getUuid())) {
            reopenWithError(player, ref, store, this.mode == Mode.TRUST
                    ? "You cannot trust yourself." : "You already own this plot.");
            return;
        }
        if (this.mode == Mode.TRUST) {
            if (targetUuid.equals(plot.getOwner())) {
                reopenWithError(player, ref, store, "That player is the owner.");
                return;
            }
            if (plot.isTrusted(targetUuid)) {
                reopenWithError(player, ref, store, safe(targetName) + " is already trusted.");
                return;
            }
            plot.addTrustedPlayer(targetUuid);
            this.plugin.getPlotManager().savePlots();
            player.getPlayerRef().sendMessage(ChatUtil.success("Trusted " + safe(targetName) + "."));
            PlotMenuPage.open(player, ref, store, this.playerRef, world, this.plugin);
        } else {
            player.getPageManager().openCustomPage(ref, store,
                    (CustomUIPage) PlotConfirmPopupPage.forTransfer(this.playerRef, world, this.plugin,
                            plot.getGridX(), plot.getGridZ(), safe(plot.getName()), targetUuid, safe(targetName)));
        }
    }

    private List<PlayerEntry> listOnlinePlayers() {
        List<PlayerEntry> result = new ArrayList<>();
        try {
            String filter = safe(this.searchInput).trim().toLowerCase(Locale.ROOT);
            for (PlayerRef p : Universe.get().getPlayers()) {
                if (p == null || p.getUuid() == null) {
                    continue;
                }
                String name = p.getUsername();
                if (name == null || name.isBlank()) {
                    continue;
                }
                if (!filter.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(filter)) {
                    continue;
                }
                result.add(new PlayerEntry(p.getUuid(), name));
                if (result.size() >= MAX_ROWS) {
                    break;
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private void reopen(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
        player.getPageManager().openCustomPage(ref, store, (CustomUIPage) new PlotPlayerPickerPage(this.playerRef,
                resolveWorld(player), this.plugin, this.mode, safe(this.searchInput), null));
    }

    private void reopenWithError(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String err) {
        player.getPageManager().openCustomPage(ref, store, (CustomUIPage) new PlotPlayerPickerPage(this.playerRef,
                resolveWorld(player), this.plugin, this.mode, safe(this.searchInput), err));
    }

    private Plot resolvePlot(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            int x;
            int z;
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform != null && transform.getPosition() != null) {
                x = (int) Math.floor(transform.getPosition().x);
                z = (int) Math.floor(transform.getPosition().z);
            } else {
                Transform t = this.playerRef.getTransform();
                if (t == null || t.getPosition() == null) {
                    return null;
                }
                x = (int) Math.floor(t.getPosition().x);
                z = (int) Math.floor(t.getPosition().z);
            }
            return this.plugin.getPlotManager().getPlotAt(this.sourceWorld.getName(), x, z);
        } catch (Exception ex) {
            return null;
        }
    }

    private World resolveWorld(Player player) {
        try {
            if (player != null && player.getWorld() != null) {
                return player.getWorld();
            }
        } catch (Exception ignored) {
        }
        return this.sourceWorld;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("Param", Codec.STRING), (o, v) -> o.param = v, o -> o.param).add()
                .append(new KeyedCodec<>("@SearchInput", Codec.STRING), (o, v) -> o.searchInput = v, o -> o.searchInput)
                .add()
                .build();
        public String action;
        public String param;
        public String searchInput;
    }

    private record PlayerEntry(UUID uuid, String name) {
    }
}
