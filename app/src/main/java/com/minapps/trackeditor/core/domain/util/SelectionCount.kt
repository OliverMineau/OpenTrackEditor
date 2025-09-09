package com.minapps.trackeditor.core.domain.util

/**
 * Defines how the selection works
 *
 */
enum class SelectionCount {
    /**
     * NONE:
     * - The tool runs instantly when clicked, then immediately goes back to "inactive".
     * - The icon never stays highlighted/selected.
     *
     * Use this for "one-shot" actions that don’t need to remain active.
     *
     * Example: Exporting a track, merging two tracks, centering the map, importing a file.
     */
    NONE,

    /**
     * ONE:
     * - Clicking the tool activates it (icon stays selected).
     * - Only one tool from the same group can be active at a time.
     * - Activating this tool automatically deactivates any other tool in the group.
     *
     * Use this when the user should work in a single mode at a time.
     *
     * Example: "Delete Waypoint" mode — you stay in this mode until you deactivate it
     * or switch to another track-editing tool.
     */
    ONE,

    /**
     * MULTIPLE:
     * - Clicking the tool activates it (icon stays selected).
     * - Multiple tools from the same group can be active at once.
     * - Activating this tool does NOT deactivate others.
     *
     * Use this for tools that can be combined and stay active together.
     *
     * Example: Enabling both "Snap to Grid" and "Show Elevation" tools at the same time.
     */
    MULTIPLE,
}
