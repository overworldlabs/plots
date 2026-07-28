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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.stoshe.plots.Plots;
import dev.stoshe.plots.util.ChatUtil;
import dev.stoshe.plots.util.Tr;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Admin-only "what's new" popup showing the latest GitHub release notes. Two actions: Close (shows
 * again next join) and Don't-show-again (dismisses this version until a newer one ships).
 */
public class PlotChangelogPage extends InteractiveCustomUIPage<PlotChangelogPage.PageData> {
    /** The body scrolls, so the cap is generous — it only guards against a pathologically long release. */
    private static final int MAX_BODY = 6000;

    private static final Pattern MD_HEADING = Pattern.compile("^#{1,6}\\s*");
    private static final Pattern MD_BULLET = Pattern.compile("^[-*+]\\s+");
    private static final Pattern MD_LINK = Pattern.compile("\\[([^\\]]+)\\]\\([^)]*\\)");

    private final PlayerRef playerRef;
    private final Plots plugin;
    private boolean handled;

    private PlotChangelogPage(@Nonnull PlayerRef playerRef, @Nonnull Plots plugin) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.playerRef = playerRef;
        this.plugin = plugin;
    }

    public static void open(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
            PlayerRef playerRef, Plots plugin) {
        player.getPageManager().openCustomPage(ref, store, (CustomUIPage) new PlotChangelogPage(playerRef, plugin));
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/PlotChangelog.ui");

        String version = plugin.getChangelogManager().version();
        commandBuilder.set("#ChangelogTitle.Text", Tr.t("changelog.title"));
        commandBuilder.set("#ChangelogSubtitle.Text", Tr.t("changelog.version", "version", version));
        commandBuilder.set("#ChangelogBody.Text", format(plugin.getChangelogManager().notes()));
        commandBuilder.set("#BtnChangelogDismiss.Text", Tr.t("changelog.btn_dismiss"));
        commandBuilder.set("#BtnChangelogClose.Text", Tr.t("changelog.btn_close"));

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnChangelogClose",
                EventData.of("Action", "Close"), false);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BtnChangelogDismiss",
                EventData.of("Action", "Dismiss"), false);
    }

    /**
     * Release notes on GitHub are markdown that can also embed raw HTML ({@code <h2>}, {@code <ul>},
     * {@code <li>}, {@code <hr>}, {@code <sub>}, {@code <a>}, …). UI labels render none of it, so both the
     * HTML tags and the markdown markers show up literally. This converts both to a clean, readable plain-
     * text layout (uppercase section headers, {@code •}/{@code ◦} bullets, dividers) that fits a single
     * scrolling label.
     */
    private String format(String notes) {
        String raw = htmlToText(notes == null ? "" : notes);

        StringBuilder out = new StringBuilder();
        for (String line : raw.split("\n", -1)) {
            String detabbed = line.replace("\t", "  ");
            int indent = detabbed.length() - detabbed.stripLeading().length();
            String s = detabbed.strip();

            if (s.isEmpty()) {
                out.append('\n');
                continue;
            }
            if (s.matches("([-*_])\\1{2,}")) { // markdown horizontal rule (---, ***, ___)
                out.append("──────────────\n");
                continue;
            }

            Matcher heading = MD_HEADING.matcher(s);
            if (heading.find()) {
                out.append('\n').append(inlineMd(s.substring(heading.end())).toUpperCase(Locale.ROOT)).append('\n');
                continue;
            }

            Matcher bullet = MD_BULLET.matcher(s);
            if (bullet.find()) {
                out.append(indent >= 2 ? "    ◦ " : "• ").append(inlineMd(s.substring(bullet.end()))).append('\n');
                continue;
            }

            out.append(inlineMd(s)).append('\n');
        }

        String text = out.toString().replaceAll("\\n{3,}", "\n\n").trim();
        if (text.length() > MAX_BODY) {
            text = text.substring(0, MAX_BODY).trim() + "…\n\n" + Tr.t("changelog.more");
        }

        return ChatUtil.stripColor(text);
    }

    /** Turns block-level HTML into text structure (markdown markers / newlines), strips the rest, decodes entities. */
    private static String htmlToText(String s) {
        s = s.replaceAll("(?i)<\\s*hr\\s*/?\\s*>", "\n---\n");
        s = s.replaceAll("(?i)<\\s*br\\s*/?\\s*>", "\n");
        s = s.replaceAll("(?i)<\\s*li[^>]*>", "\n- ");
        s = s.replaceAll("(?i)<\\s*h[1-6][^>]*>", "\n# ");
        s = s.replaceAll("(?i)</\\s*(p|div|ul|ol|li|h[1-6]|tr|table|blockquote|section)\\s*>", "\n");
        s = s.replaceAll("(?i)<\\s*(p|div|ul|ol|tr|table|blockquote|section)[^>]*>", "\n");
        s = s.replaceAll("(?is)<[^>]+>", ""); // drop any remaining inline tags (a, sub, span, img, b, ...), keep their text
        s = s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ");
        return s;
    }

    /** Removes inline markdown emphasis/code and rewrites {@code [text](url)} links to just their text. */
    private static String inlineMd(String s) {
        s = s.replace("**", "").replace("__", "");
        s = MD_LINK.matcher(s).replaceAll("$1");
        s = s.replace("`", "");
        return s.strip();
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PageData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        if (this.handled) {
            return; // ignore the duplicate press/release event
        }
        this.handled = true;

        if ("Dismiss".equals(safe(data.action))) {
            plugin.getChangelogManager().dismiss(this.playerRef.getUuid());
        }

        player.getPageManager().setPage(ref, store, Page.None);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action).add()
                .build();
        public String action;
    }
}
