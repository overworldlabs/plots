package dev.stoshe.plots.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.config.PlotConfig;
import dev.stoshe.plots.util.ChatUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Popup that creates a new plot world from a name. The world is generated with
 * default sizes/blocks; per-world tuning can be done afterwards in config.json.
 */
public class PlotWorldCreatePopupPage extends InteractiveCustomUIPage<PlotWorldCreatePopupPage.PageData> {

    private static final int MAX_NAME_LENGTH = 48;

    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Plots plugin;
    private final String error;

    private PlotWorldCreatePopupPage(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld, @Nonnull Plots plugin,
            @Nullable String error) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
        this.error = error;
    }

    /**
     * Opens the create-world popup.
     *
     * @param player      the viewing player
     * @param ref         the player entity reference
     * @param store       the entity store
     * @param playerRef   the player ref
     * @param sourceWorld the world the player is currently in
     * @param plugin      the plugin instance
     */
    public static void open(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef,
            World sourceWorld, Plots plugin) {
        if (player == null || ref == null || store == null || playerRef == null || sourceWorld == null
                || plugin == null) {
            return;
        }
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) new PlotWorldCreatePopupPage(playerRef, sourceWorld, plugin, null));
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PlotWorldCreatePopup.ui");
        commandBuilder.set("#WorldCreateInput.Value", "");
        boolean hasError = this.error != null && !this.error.isBlank();
        commandBuilder.set("#WorldCreateError.Visible", hasError);
        commandBuilder.set("#WorldCreateError.Text", hasError ? this.error : "");

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnWorldCreateCancel",
                EventData.of("Action", "Cancel"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnWorldCreateConfirm",
                EventData.of("Action", "Create").append("@NameInput", "#WorldCreateInput.Value"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        String action = data.action == null ? "" : data.action.trim();
        World world = resolveWorld(player);

        if ("Cancel".equals(action)) {
            PlotAdminWorldsPage.open(player, ref, store, this.playerRef, world, this.plugin);
            return;
        }
        if (!"Create".equals(action)) {
            return;
        }

        String name = data.nameInput == null ? "" : data.nameInput.trim();
        String validationError = validateName(name);
        if (validationError != null) {
            reopenWithError(player, ref, store, validationError);
            return;
        }

        createWorld(name);
        player.getPlayerRef().sendMessage(ChatUtil.success("Creating plot world '" + name + "'..."));
        PlotAdminWorldsPage.open(player, ref, store, this.playerRef, world, this.plugin);
    }

    /**
     * Validates a requested world name.
     *
     * @param name the requested name
     * @return an error message, or {@code null} when the name is valid
     */
    @Nullable
    private String validateName(String name) {
        if (name.isBlank()) {
            return "Name cannot be empty.";
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return "Name is too long (max " + MAX_NAME_LENGTH + ").";
        }
        if (!name.matches("[A-Za-z0-9_]+")) {
            return "Use only letters, numbers and underscores.";
        }
        if (this.plugin.getConfig().isManagedWorld(name)) {
            return "A plot world with that name already exists.";
        }
        return null;
    }

    /** Adds the world to config, persists it, and triggers async generation. */
    private void createWorld(String name) {
        PlotConfig config = this.plugin.getConfig();
        PlotConfig.WorldEntry entry = new PlotConfig.WorldEntry();
        config.putWorld(name, entry);
        this.plugin.saveConfig(config);
        this.plugin.getWorldManager().createWorld(name, config.world(name));
    }

    private void reopenWithError(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String err) {
        player.getPageManager().openCustomPage(ref, store,
                (CustomUIPage) new PlotWorldCreatePopupPage(this.playerRef, resolveWorld(player), this.plugin, err));
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

    /** Codec-backed payload for the create-world popup. */
    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .append(new KeyedCodec<>("@NameInput", Codec.STRING), (o, v) -> o.nameInput = v, o -> o.nameInput)
                .add()
                .build();
        public String action;
        public String nameInput;
    }
}
