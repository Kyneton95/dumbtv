package tv.dumbtv.home;

/** Small host-side checks for the pixel path, without a device or test framework. */
public final class FocusColorTest {
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
    }

    public static void main(String[] args) {
        int[] transparent = new int[100];
        check(FocusColor.representative(transparent) == FocusColor.FALLBACK, "transparent fallback");

        int[] icon = new int[100];
        for (int i = 0; i < icon.length; i++) icon[i] = i < 75 ? 0xFFFFFFFF : 0xFFFF3020;
        int color = FocusColor.representative(icon);
        int red = color >> 16 & 255, green = color >> 8 & 255, blue = color & 255;
        check(red > green * 2 && red > blue * 2, "colored logo beats white backdrop");
        check(red < 200, "bright art darkened");

        int[] blueIcon = new int[100];
        for (int i = 0; i < blueIcon.length; i++) blueIcon[i] = 0xFF2050E0;
        color = FocusColor.representative(blueIcon);
        check((color & 255) > (color >> 16 & 255), "blue stays blue");
        check((FocusColor.tint(color) >>> 24) == 61, "tint is 24 percent opaque");
        check((FocusColor.tint(color) & 0xFFFFFF) == (color & 0xFFFFFF), "tint keeps color");
        check("auto".equals(Config.BACKGROUND.fallback), "fork default background is Auto");
        check("Adaptive wallpaper tint".equals(Config.BACKGROUND.name("adaptive-tint")),
                "tint is an Options choice");
        check("Amber".equals(Config.ACCENT.name("amber")), "amber accent unchanged");

        // Auto scene boundaries, exactly as documented in Options.
        check("aurora".equals(Config.autoScene(0)), "00:00 aurora");
        check("aurora".equals(Config.autoScene(4)), "04:59 aurora");
        check("dunes".equals(Config.autoScene(5)), "05:00 dunes");
        check("dunes".equals(Config.autoScene(8)), "08:59 dunes");
        check("day".equals(Config.autoScene(9)), "09:00 daylight");
        check("day".equals(Config.autoScene(16)), "16:59 daylight");
        check("dunes".equals(Config.autoScene(17)), "17:00 dunes");
        check("dunes".equals(Config.autoScene(19)), "19:59 dunes");
        check("coast".equals(Config.autoScene(20)), "20:00 coast");
        check("coast".equals(Config.autoScene(23)), "23:59 coast");
        check("coast".equals(Config.autoScene(-1)), "wrapped hour");

        // Next background starts from Auto's resolved scene and then becomes explicit.
        check("coast".equals(Config.nextSceneValue("auto", 18)), "auto dusk -> coast");
        check("aurora".equals(Config.nextSceneValue("coast", 18)), "coast -> aurora");
        check("day".equals(Config.nextSceneValue("aurora", 18)), "aurora -> day");
        check("dunes".equals(Config.nextSceneValue("day", 18)), "day -> dunes");
        check(Config.nextSceneValue("black", 18) == null, "solid background has no next scene");

        System.out.println("FocusColorTest passed");
    }
}
