package com.minapps.trackeditor.core.domain.type

import com.minapps.trackeditor.R
import com.minapps.trackeditor.core.domain.tool.EditorTool
import com.minapps.trackeditor.core.domain.util.SelectionCount
import com.minapps.trackeditor.core.domain.util.ToolGroup

/**
 * Type of action/tool
 *
 * @property icon
 * @property label
 * @property selectionCount
 * @property group
 */
enum class ActionType(
    val icon: Int?,
    val label: String?,
    val selectionCount: SelectionCount?,
    val group: ToolGroup? = ToolGroup.NONE,
    // Deselect tracks and points when clicked
    val deselect: Boolean = false,
) {
    // No action
    NONE(null, "None", null, deselect = true),
    HAND(R.drawable.hand_24, "Hand", SelectionCount.ONE, ToolGroup.ALL, true),
    SPACER(null, null, null),

    // File & system actions
    EXPORT(R.drawable.file_export_24, "Export", SelectionCount.NONE, ToolGroup.FILE_SYSTEM),
    SCREENSHOT(
        R.drawable.mode_landscape_24,
        "Screenshot",
        SelectionCount.NONE,
        ToolGroup.FILE_SYSTEM,
        true
    ),
    DELETE(R.drawable.trash_24, "Delete", SelectionCount.NONE, ToolGroup.FILE_SYSTEM, true),

    // Visual tools
    ELEVATION(R.drawable.curve_arrow_24, "Elevation", SelectionCount.MULTIPLE),
    LAYERS(R.drawable.land_layers_24, "Layers", SelectionCount.MULTIPLE),

    // Editing tools
    REVERSE(R.drawable.rotate_reverse_24, "Reverse", SelectionCount.NONE, ToolGroup.TRACK_EDITING),
    REMOVE_DUPS(
        R.drawable.circle_overlap_24,
        "Remove dups",
        SelectionCount.NONE,
        ToolGroup.TRACK_EDITING
    ),
    REMOVE_BUGS(
        R.drawable.bug_slash_24,
        "Remove bugs",
        SelectionCount.NONE,
        ToolGroup.TRACK_EDITING
    ),
    CUT(R.drawable.scissors_24, "Cut", SelectionCount.NONE, ToolGroup.TRACK_EDITING),
    JOIN(R.drawable.link_alt_24, "Join", SelectionCount.NONE, ToolGroup.TRACK_EDITING),
    REDUCE_NOISE(
        R.drawable.noise_cancelling_headphones_24,
        "Reduce noise",
        SelectionCount.NONE,
        ToolGroup.TRACK_EDITING
    ),
    FILTER(R.drawable.filter_24, "Filter", SelectionCount.NONE, ToolGroup.TRACK_EDITING),
    MAGIC_FILTER(R.drawable.sweep_24, "Magic filter", SelectionCount.NONE, ToolGroup.TRACK_EDITING),

    // Edit Bottom Navigation
    ADD(R.drawable.map_marker_plus_24, "Add", SelectionCount.ONE, ToolGroup.TRACK_EDITING, true),
    REMOVE(R.drawable.map_marker_cross_24, "Remove", SelectionCount.ONE, ToolGroup.TRACK_EDITING, true),
    SELECT(R.drawable.map_location_track_24, "Select", SelectionCount.MULTIPLE, ToolGroup.ALL),


    // Main Bottom Navigation
    VIEW(R.drawable.map_marker_24, "View", SelectionCount.ONE, ToolGroup.TRACK_EDITING, true),
    EDIT(R.drawable.file_edit_24, "Edit", SelectionCount.MULTIPLE, ToolGroup.TRACK_EDITING),
    TOOLBOX(R.drawable.tools_24, "Toolbox", SelectionCount.MULTIPLE, ToolGroup.TRACK_EDITING),
}