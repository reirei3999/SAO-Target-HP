package dev.sao.targethp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JSON-backed settings. The file lives at {@code config/saotargethp.json} and is
 * rewritten with defaults the first time the game starts.
 */
public final class SaoConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("saotargethp.json");

    private static SaoConfig instance;

    public Display display = new Display();
    public MobBar mob = new MobBar();
    public BossBar boss = new BossBar();

    public static SaoConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static void reload() {
        instance = load();
    }

    public static Path path() {
        return PATH;
    }

    private static SaoConfig load() {
        boolean created = !Files.exists(PATH);
        SaoConfig cfg = null;
        if (!created) {
            try (BufferedReader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                cfg = GSON.fromJson(reader, SaoConfig.class);
            } catch (Exception ignored) {
                // A broken file falls back to defaults instead of crashing the HUD.
            }
        }
        if (cfg == null) {
            cfg = new SaoConfig();
        }
        if (cfg.mob == null) {
            cfg.mob = new MobBar();
        }
        if (cfg.display == null) {
            cfg.display = new Display();
        }
        if (cfg.boss == null) {
            cfg.boss = new BossBar();
        }
        cfg.normalize();
        // Only touch the disk when the file did not exist yet. The old code rewrote the
        // config on every load, which put a synchronous file write on the render thread
        // the first time the HUD was drawn.
        if (created) {
            cfg.save();
        }
        return cfg;
    }

    public void save() {
        normalize();
        try {
            Files.createDirectories(PATH.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception ignored) {
            // Nothing sensible to do if the config directory is not writable.
        }
    }

    public void resetToDefaults() {
        display = new Display();
        mob = new MobBar();
        boss = new BossBar();
        save();
    }

    /** Clamps every value into its supported range and refreshes the cached colours. */
    public void normalize() {
        if (display == null) {
            display = new Display();
        }
        mob.sanitize();
        boss.sanitize();
        mob.resolveColors();
        boss.resolveColors();
    }

    /** Parses {@code #RRGGBB} or {@code #AARRGGBB}; anything else yields {@code fallback}. */
    public static int parseColor(String raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        String value = raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() == 6 || value.length() == 8) {
            try {
                int parsed = Integer.parseUnsignedInt(value, 16);
                return value.length() == 6 ? (0xFF000000 | parsed) : parsed;
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    /** Formats an ARGB int as {@code #AARRGGBB} for the GUI text fields. */
    public static String formatColor(int argb) {
        return String.format("#%08X", argb);
    }

    /** True when {@code raw} is a parseable {@code #RRGGBB} or {@code #AARRGGBB} value. */
    public static boolean isValidColor(String raw) {
        if (raw == null) {
            return false;
        }
        String value = raw.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6 && value.length() != 8) {
            return false;
        }
        try {
            Integer.parseUnsignedInt(value, 16);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /** Master switches. Turning things off also skips the per-tick target search. */
    public static final class Display {
        /** Master switch for the whole HUD. */
        public boolean enabled = true;
        /** Draw the mob target gauge and pin. */
        public boolean mobBar = true;
        /** Draw the boss bars. */
        public boolean bossBar = true;
        /**
         * Keep the HUD visible while the vanilla HUD is hidden with F1.
         * When false, F1 hides this mod's gauges as well.
         */
        public boolean showWhenHudHidden = false;
    }

    public static final class MobBar {
        public String normalColor = "#58A83B";
        public String normalHighlightColor = "#83D25B";
        public String cautionColor = "#E0AF32";
        public String cautionHighlightColor = "#FFDD69";
        public String dangerColor = "#D9413A";
        public String dangerHighlightColor = "#FF7666";
        public String trailColor = "#8F6A2C";
        public String backgroundColor = "#162016";
        public String outlineColor = "#E8000000";

        /** Percentage of max HP where the gauge turns yellow. */
        public double cautionThresholdPercent = 40.0D;
        /** Percentage of max HP where the gauge turns red. */
        public double dangerThresholdPercent = 20.0D;

        /** Sideways offset of the gauge, in blocks, relative to the target pin. */
        public double offsetX = 0.0D;
        /** Vertical offset of the gauge, in blocks, relative to the target pin. */
        public double offsetY = 0.0D;

        public transient int normalArgb;
        public transient int normalHighlightArgb;
        public transient int cautionArgb;
        public transient int cautionHighlightArgb;
        public transient int dangerArgb;
        public transient int dangerHighlightArgb;
        public transient int trailArgb;
        public transient int backgroundArgb;
        public transient int outlineArgb;

        private void sanitize() {
            cautionThresholdPercent = clamp(cautionThresholdPercent, 0.0D, 100.0D);
            dangerThresholdPercent = clamp(dangerThresholdPercent, 0.0D, 100.0D);
            if (dangerThresholdPercent > cautionThresholdPercent) {
                double swap = dangerThresholdPercent;
                dangerThresholdPercent = cautionThresholdPercent;
                cautionThresholdPercent = swap;
            }
            offsetX = clamp(offsetX, -8.0D, 8.0D);
            offsetY = clamp(offsetY, -8.0D, 8.0D);
        }

        private void resolveColors() {
            normalArgb = parseColor(normalColor, 0xFF58A83B);
            normalHighlightArgb = parseColor(normalHighlightColor, 0xFF83D25B);
            cautionArgb = parseColor(cautionColor, 0xFFE0AF32);
            cautionHighlightArgb = parseColor(cautionHighlightColor, 0xFFFFDD69);
            dangerArgb = parseColor(dangerColor, 0xFFD9413A);
            dangerHighlightArgb = parseColor(dangerHighlightColor, 0xFFFF7666);
            trailArgb = parseColor(trailColor, 0xFF8F6A2C);
            backgroundArgb = parseColor(backgroundColor, 0xFF162016);
            outlineArgb = parseColor(outlineColor, 0xE8000000);
        }
    }

    public static final class BossBar {
        public String fillColor = "#55A83A";
        public String highlightColor = "#79C95A";
        public String backgroundColor = "#162016";
        public String rimColor = "#2D312A";
        public String outlineColor = "#0E0E0E";
        public String nameColor = "#F4F4F4";

        /** Horizontal offset from the screen centre, in GUI pixels. */
        public int offsetX = 0;
        /** Distance from the top of the screen to the first bar, in GUI pixels. */
        public int offsetY = 24;
        /** Vertical gap between stacked boss bars, in GUI pixels. */
        public int spacing = 28;
        /** Vertical offset of the boss name, relative to its bar. */
        public int nameOffsetY = -11;

        public transient int fillArgb;
        public transient int highlightArgb;
        public transient int backgroundArgb;
        public transient int rimArgb;
        public transient int outlineArgb;
        public transient int nameArgb;

        private void sanitize() {
            offsetX = (int) clamp(offsetX, -600.0D, 600.0D);
            offsetY = (int) clamp(offsetY, -100.0D, 600.0D);
            spacing = (int) clamp(spacing, 0.0D, 200.0D);
            nameOffsetY = (int) clamp(nameOffsetY, -100.0D, 100.0D);
        }

        private void resolveColors() {
            fillArgb = parseColor(fillColor, 0xFF55A83A);
            highlightArgb = parseColor(highlightColor, 0xFF79C95A);
            backgroundArgb = parseColor(backgroundColor, 0xFF162016);
            rimArgb = parseColor(rimColor, 0xFF2D312A);
            outlineArgb = parseColor(outlineColor, 0xFF0E0E0E);
            nameArgb = parseColor(nameColor, 0xFFF4F4F4);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
