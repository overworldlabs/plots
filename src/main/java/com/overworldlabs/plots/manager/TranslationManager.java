package com.overworldlabs.plots.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.overworldlabs.plots.util.ConsoleColors;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages plugin translations
 */
public class TranslationManager {
    private final Map<String, String> translations = new HashMap<>();
    private final String language;
    private final Gson gson = new Gson();
    private final File langDir;

    public TranslationManager(@Nonnull File dataDir, @Nonnull String language) {
        this.langDir = new File(dataDir, "lang");
        this.language = language.toLowerCase();

        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        exportDefaultLanguages();
        load();
    }

    private void load() {
        // Load default English first
        loadLanguage("en_us");

        // Load target language if different
        if (!language.equals("en_us")) {
            loadLanguage(language);
        }
    }

    private void exportDefaultLanguages() {
        exportResource("en_us.json");
        exportResource("pt_br.json");
    }

    private void exportResource(String name) {
        File target = new File(langDir, name);
        if (target.exists()) {
            return;
        }

        try (InputStream is = getClass().getResourceAsStream("/lang/" + name)) {
            if (is != null) {
                Files.copy(is, target.toPath());
            }
        } catch (Exception e) {
            ConsoleColors.error("Failed to export " + name + ": " + e.getMessage());
        }
    }

    private void loadLanguage(String lang) {
        String fileName = lang + ".json";
        File file = new File(langDir, fileName);

        InputStream is = null;
        try {
            if (file.exists()) {
                is = new FileInputStream(file);
            } else {
                is = getClass().getResourceAsStream("/lang/" + fileName);
            }

            if (is == null) {
                ConsoleColors.error("Could not find language file: " + fileName);
                return;
            }

            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();

            Map<String, Object> loaded = gson.fromJson(new InputStreamReader(is), type);

            if (loaded != null) {
                flattenAndPut("", loaded);
            }
        } catch (Exception e) {
            ConsoleColors.error("Failed to load language " + lang + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void flattenAndPut(String prefix, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                flattenAndPut(key, (Map<String, Object>) value);
            } else {
                translations.put(key, String.valueOf(value));
            }
        }
    }

    @Nonnull
    public String get(@Nonnull String key) {
        String value = translations.get(key);
        return value != null ? value : key;
    }

    @Nonnull
    public String get(@Nonnull String key, Object... args) {
        String text = get(key);

        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 < args.length) {
                String placeholder = "%" + args[i] + "%";
                Object argValue = args[i + 1];
                String value = argValue != null ? String.valueOf(argValue) : "null";
                text = text.replace(placeholder, value);
            }
        }

        return text;
    }
}
