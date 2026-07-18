package Glaxium.Minema.ui;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.settings.ui.UIVideoSettingsOverlayPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIReplaysOverlayPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utility.UIUtilityOverlayPanel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/**
 * Two entry points, matching the two ways Shift+F4 can reach this addon:
 *
 * <p>{@link #openFromWorld()} -- from BBSMinema's own vanilla KeyBinding
 * check in onClientTick, which only ever fires while BBS's dashboard is
 * NOT the current screen (BBS's own UI input handling swallows raw key
 * events before vanilla KeyBinding polling sees them once its UIScreen has
 * focus -- see MinemaSettingsUIScreenKeyMixin for the other half of this).
 * Opens BBS's dashboard first if it isn't already open, then layers Minema
 * Settings on top of it -- pressing the key "outside" BBS UI now always
 * brings BOTH up together, rather than a standalone screen with no BBS UI
 * underneath at all.
 *
 * <p>{@link #openDirect()} -- from MinemaSettingsUIScreenKeyMixin, which
 * fires from inside BBS's own key handling while its UIScreen already has
 * focus. BBS UI is already open in this case, so this just layers Minema
 * Settings on top directly, no dashboard-opening step needed.
 */
public final class MinemaSettingsOpener
{
    private MinemaSettingsOpener()
    {
    }

    public static void openFromWorld()
    {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!(client.currentScreen instanceof UIScreen))
        {
            UIScreen.open(BBSModClient.getDashboard());
        }

        openDirect();
    }

    public static void openDirect()
    {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen current = client.currentScreen;

        // Already open -- don't stack a second copy on top of itself.
        if (current instanceof MinemaSettingsScreen)
        {
            return;
        }

        if (isBlockedByDashboardOverlay())
        {
            return;
        }

        client.setScreen(new MinemaSettingsScreen(current));
    }

    /**
     * True only when BBS's own dashboard (UIScreen) is the current screen
     * AND one of its "busy" overlay panels is on top -- Edit Settings
     * (UIVideoSettingsOverlayPanel), the Replay panel
     * (UIReplaysOverlayPanel), or the Utility panel
     * (UIUtilityOverlayPanel). BBS's own Settings/Keybinds overlay
     * (UISettingsOverlayPanel) is deliberately NOT in this list -- it's the
     * one explicit exception the dashboard can still have open when Minema
     * Settings is requested. No overlay open at all is also fine.
     */
    private static boolean isBlockedByDashboardOverlay()
    {
        UIBaseMenu menu = UIScreen.getCurrentMenu();

        if (menu == null)
        {
            return false;
        }

        for (UIOverlayPanel overlay : menu.getRoot().getChildren(UIOverlayPanel.class))
        {
            if (overlay instanceof UIVideoSettingsOverlayPanel
                    || overlay instanceof UIReplaysOverlayPanel
                    || overlay instanceof UIUtilityOverlayPanel)
            {
                return true;
            }
        }

        return false;
    }
}
