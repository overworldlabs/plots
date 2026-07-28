package dev.stoshe.plots.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
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
import dev.stoshe.plots.api.IPlotManager;
import dev.stoshe.plots.util.PermissionUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal listing the plot commands (player section, plus an admin section for
 * staff). Opened from {@code /plot help} or {@code /plot} with no subcommand.
 */
public class PlotHelpPage extends InteractiveCustomUIPage<PlotHelpPage.PageData> {

    /** Number of `#HpRow{i}` slots defined in PlotHelp.ui. */
    private static final int ROWS = 24;

    private final PlayerRef playerRef;
    private final World sourceWorld;
    private final Plots plugin;

    private PlotHelpPage(@Nonnull PlayerRef playerRef, @Nonnull World sourceWorld, @Nonnull Plots plugin) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.sourceWorld = sourceWorld;
        this.plugin = plugin;
    }

    /**
     * Opens the command-help modal for a player.
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
                (CustomUIPage) new PlotHelpPage(playerRef, sourceWorld, plugin));
    }

    /**
     * Builds the help lines, listing only the commands the viewer is allowed to
     * use (admins see everything, including the admin section).
     *
     * @param viewer the viewing player
     * @param admin  whether the viewer has admin permission
     * @return the ordered help lines
     */
    private List<String> buildLines(PlayerRef viewer, boolean admin) {
        List<String> playerLines = new ArrayList<>();
        addIfPermitted(playerLines, viewer, admin, IPlotManager.PERM_CLAIM,
                "/plot claim — Claim the plot you're standing on");
        addIfPermitted(playerLines, viewer, admin, IPlotManager.PERM_PLOT,
                "/plot auto — Auto-claim the nearest free plot");
        addIfPermitted(playerLines, viewer, admin, IPlotManager.PERM_INFO,
                "/plot info — Show info about the current plot");
        addIfPermitted(playerLines, viewer, admin, IPlotManager.PERM_LIST,
                "/plot list — List the plots you own");
        addIfPermitted(playerLines, viewer, admin, IPlotManager.PERM_PLOT,
                "/plot menu — Open the plot management menu");
        addIfPermitted(playerLines, viewer, admin, IPlotManager.PERM_PLOT,
                "/plot worlds — Browse and teleport between plot worlds");
        addIfPermitted(playerLines, viewer, admin, IPlotManager.PERM_SPAWN,
                "/plot spawn [world] — Teleport to a plot world spawn");
        addIfPermitted(playerLines, viewer, admin, IPlotManager.PERM_PLOT,
                "/plot warps — Browse public plot warps");
        addIfPermitted(playerLines, viewer, admin, IPlotManager.PERM_RENAME,
                "/plot rename <name> — Rename your plot");
        addIfPermitted(playerLines, viewer, admin, IPlotManager.PERM_TRUST,
                "/plot trust/untrust <player> — Manage trusted players");
        addIfPermitted(playerLines, viewer, admin, IPlotManager.PERM_TRANSFER,
                "/plot transfer <player> — Transfer your plot");
        addIfPermitted(playerLines, viewer, admin, IPlotManager.PERM_FLAG,
                "/plot flag <set|remove|list> — Manage plot flags");
        addIfPermitted(playerLines, viewer, admin, IPlotManager.PERM_MERGE,
                "/plot merge/unmerge <direction> — Merge or unmerge plots");
        addIfPermitted(playerLines, viewer, admin, IPlotManager.PERM_DELETE,
                "/plot delete — Delete your plot");

        List<String> lines = new ArrayList<>();
        if (!playerLines.isEmpty()) {
            lines.add("— Player —");
            lines.addAll(playerLines);
        }
        if (admin) {
            lines.add("");
            lines.add("— Admin —");
            lines.add("/plot admin — Open the admin dashboard");
            lines.add("/plot setspawn — Set this world's spawn point");
            lines.add("/plot refresh — Reload the plugin config");
            lines.add("/plot debug bridge — Show bridge/mixin diagnostics");
        }
        return lines;
    }

    /** Adds a help line only when the viewer is an admin or holds {@code node}. */
    private void addIfPermitted(List<String> lines, PlayerRef viewer, boolean admin, String node, String label) {
        if (admin || viewer.hasPermission(node)) {
            lines.add(label);
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PlotHelp.ui");

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                EventData.of("Action", "Close"), false);

        boolean admin = PermissionUtil.hasAdminPermission(this.playerRef.getUuid());
        List<String> lines = buildLines(this.playerRef, admin);

        for (int i = 0; i < ROWS; i++) {
            if (i < lines.size()) {
                commandBuilder.set("#HpRow" + i + ".Visible", true);
                commandBuilder.set("#HpRowText" + i + ".Text", lines.get(i));
            } else {
                commandBuilder.set("#HpRow" + i + ".Visible", false);
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        String action = data.action == null ? "" : data.action.trim();
        if ("Close".equals(action)) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }

    /** Codec-backed payload for help-modal button events. */
    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .build();
        public String action;
    }
}
