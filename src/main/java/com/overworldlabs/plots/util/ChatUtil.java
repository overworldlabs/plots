package com.overworldlabs.plots.util;

import com.hypixel.hytale.server.core.Message;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for advanced chat formatting, hex colors, and standardized messaging.
 * <p>
 * This class handles the conversion of strings containing hex color tags
 * in the format {@code {#RRGGBB}} into Hytale {@link Message} objects.
 * It ensures color persistence across segments until a new color is defined.
 * </p>
 */
public final class ChatUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("\\{#([A-Fa-f0-9]{6})\\}");

    /**
     * The default plugin prefix used for all chat messages.
     */
    public static final String PREFIX = ColorConstants.PRIMARY + "[Plots] " + ColorConstants.SECONDARY + "» ";

    private ChatUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Parses a string with hex color tags and returns a Hytale Message.
     * Supports {#RRGGBB} tags which persist until the next color tag.
     *
     * @param text The text to process (e.g., "{#FF0000}Hello {#00FF00}World")
     * @return A {@link Message} object with formatted colors
     */
    @Nonnull
    public static Message colorize(@Nonnull String text) {
        if (text.isEmpty()) {
            return Message.raw("");
        }

        Matcher matcher = HEX_PATTERN.matcher(text);
        @Nonnull
        List<Message> parts = new ArrayList<>();
        int lastEnd = 0;
        String currentColor = null;

        while (matcher.find()) {
            // Add segment before the color tag
            String segment = text.substring(lastEnd, matcher.start());
            if (!segment.isEmpty()) {
                Message part = Message.raw(segment);
                if (currentColor != null) {
                    part.color(currentColor);
                }
                parts.add(part);
            }

            // Update color for subsequent segments
            currentColor = "#" + matcher.group(1);
            lastEnd = matcher.end();
        }

        // Add remaining segment
        String remaining = text.substring(lastEnd);
        if (!remaining.isEmpty()) {
            Message part = Message.raw(remaining);
            if (currentColor != null) {
                part.color(currentColor);
            }
            parts.add(part);
        }

        if (parts.isEmpty()) {
            return Message.raw("");
        }

        if (parts.size() == 1) {
            return parts.get(0);
        }

        return Message.join(parts.toArray(new Message[0]));
    }

    /**
     * Shortcut for {@link #colorize(String)}.
     * Legacy support for existing codebase.
     * 
     * @param text The text to process
     * @return A colored {@link Message}
     */
    @Nonnull
    public static Message raw(@Nonnull String text) {
        return colorize(text);
    }

    /**
     * Processes formatted text with arguments and applies colors.
     * Uses {@link String#format(String, Object...)} internally.
     *
     * @param text The format string
     * @param args The arguments
     * @return A colored {@link Message}
     */
    @Nonnull
    public static Message raw(@Nonnull String text, Object... args) {
        String formatted = String.format(text, args);
        return colorize(formatted != null ? formatted : "");
    }

    /**
     * Creates a standardized informational message with the plugin prefix.
     *
     * @param text The message text
     * @return A colored {@link Message} with prefix and info color
     */
    @Nonnull
    public static Message info(@Nonnull String text) {
        return colorize(PREFIX + ColorConstants.INFO + text);
    }

    /**
     * Creates a standardized success message with the plugin prefix.
     *
     * @param text The message text
     * @return A colored {@link Message} with prefix and success color
     */
    @Nonnull
    public static Message success(@Nonnull String text) {
        return colorize(PREFIX + ColorConstants.SUCCESS + text);
    }

    /**
     * Creates a standardized error message with the plugin prefix.
     *
     * @param text The message text
     * @return A colored {@link Message} with prefix and error color
     */
    @Nonnull
    public static Message error(@Nonnull String text) {
        return colorize(PREFIX + ColorConstants.ERROR + text);
    }

    /**
     * Creates a standardized warning message with the plugin prefix.
     *
     * @param text The message text
     * @return A colored {@link Message} with prefix and warning color
     */
    @Nonnull
    public static Message warning(@Nonnull String text) {
        return colorize(PREFIX + ColorConstants.WARNING + text);
    }

    /**
     * Strips all hex color tags from the string.
     *
     * @param text The text to strip
     * @return The plain text without color tags
     */
    @Nonnull
    public static String stripColor(@Nonnull String text) {
        String stripped = HEX_PATTERN.matcher(text).replaceAll("");
        return stripped != null ? stripped : "";
    }

    /**
     * Creates a new MessageBuilder instance for structured message creation.
     * 
     * @return A new {@link MessageBuilder}
     */
    @Nonnull
    public static MessageBuilder builder() {
        return new MessageBuilder();
    }

    /**
     * Builder class for creating complex messages with ease.
     */
    public static class MessageBuilder {
        private final StringBuilder content = new StringBuilder();

        public MessageBuilder append(@Nonnull String text) {
            content.append(text);
            return this;
        }

        public MessageBuilder color(@Nonnull String hexColor) {
            if (hexColor.startsWith("#")) {
                content.append("{").append(hexColor).append("}");
            } else if (hexColor.startsWith("{#")) {
                content.append(hexColor);
            } else {
                content.append("{#").append(hexColor).append("}");
            }
            return this;
        }

        public MessageBuilder nextLine() {
            content.append("\n");
            return this;
        }

        @Nonnull
        public Message build() {
            return colorize(content.toString());
        }
    }
}
