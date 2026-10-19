package com.artist.king;

/** Pure navigation policy: vertical scrolling is preserved outside Home. */
final class GestureRoutes {
    static String destination(String page, float dx, float dy, boolean atTop, long durationMs) {
        if (page == null || durationMs > 650) return null;
        if (Math.abs(dx) >= 85 && Math.abs(dx) > Math.abs(dy) * 1.8f) {
            if (page.equals("Home")) return dx < 0 ? "Board" : "Focus";
            if (page.equals("Board")) return dx > 0 ? "Home" : null;
            if (page.equals("Focus")) return dx < 0 ? "Home" : null;
            if (dx > 0) return page.equals("Ritual") || page.equals("Tasks") || page.equals("Notes") ? "Board" : "Home";
        }
        if (Math.abs(dy) >= 85 && Math.abs(dy) > Math.abs(dx) * 1.8f) {
            if (page.equals("Home") && dy < 0) return "Apps";
            if (page.equals("Apps") && atTop && dy > 0) return "Home";
        }
        return null;
    }
}
