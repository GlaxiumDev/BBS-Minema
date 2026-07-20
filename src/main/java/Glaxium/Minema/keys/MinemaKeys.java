package Glaxium.Minema.keys;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;
import mchorse.bbs_mod.ui.utils.keys.KeybindSettings;
import org.lwjgl.glfw.GLFW;

/**
 * BBS-internal keybind for opening Minema Settings -- this is a BBS mod
 * {@link KeyCombo}, not a vanilla {@code KeyBinding}. It only ever fires
 * while BBS's own UIScreen has keyboard focus (dashboard open), the exact
 * same mechanism the built-in F6 "Utility panel" key uses (see
 * {@code mchorse.bbs_mod.ui.Keys#OPEN_UTILITY_PANEL}). It intentionally
 * does NOT show up in Minecraft's Controls menu at all, and doesn't fire
 * while playing normally with no BBS UI open -- unlike the old vanilla
 * KeyBinding this replaces.
 */
public final class MinemaKeys
{
    public static final KeyCombo OPEN_SETTINGS = new KeyCombo(
        "open_minema_settings",
        IKey.raw("Open Minema Settings"),
        GLFW.GLFW_KEY_J
    ).categoryKey("minema");

    private MinemaKeys() {}

    /**
     * Makes {@link #OPEN_SETTINGS} show up, and be rebindable, in BBS's own
     * Settings > Keybinds tab (the one backed by BBS mod's own
     * {@code keybinds.json} -- separate from Minecraft's options.txt),
     * grouped under its own "minema" heading alongside the built-in
     * "camera", "flight", "dashboard", etc. sections.
     *
     * BBS mod doesn't currently expose a public API for an addon to add its
     * own class to that scan list ({@link KeybindSettings} only scans its
     * own {@code Keys.class} by default), so this reaches into that one
     * private field via reflection. This only ever adds an entry to a list
     * that gets read later when the player actually opens that settings
     * tab -- it doesn't matter whether BBS-Minema or BBS mod itself
     * finishes initializing first. If a future BBS mod version renames or
     * removes that field, this silently no-ops (logged once) rather than
     * crashing -- the J keybind itself does not depend on this succeeding,
     * it just won't be listed/rebindable from that particular tab.
     */
    public static void registerInKeybindsTab()
    {
        try
        {
            java.lang.reflect.Field classesField = KeybindSettings.class.getDeclaredField("classes");

            classesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            java.util.List<Class> classes = (java.util.List<Class>) classesField.get(null);

            if (!classes.contains(MinemaKeys.class))
            {
                classes.add(MinemaKeys.class);
            }
        }
        catch (Exception e)
        {
            System.out.println("[BBS-Minema] Could not register Minema keybinds into BBS's Keybinds tab (BBS mod internals may have changed) -- the J keybind itself still works, it just won't be listed there. " + e);
        }
    }
}
