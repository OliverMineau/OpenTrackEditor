package com.minapps.trackeditor.feature_map_editor.tools.filter.presentation

import android.app.Activity
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.google.android.material.slider.Slider
import com.minapps.trackeditor.R
import com.minapps.trackeditor.core.domain.tool.ToolDialog
import com.minapps.trackeditor.core.domain.tool.ToolUiContext
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterParams
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterType
import com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.FilterUIs.DistanceBasedUI
import com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.FilterUIs.EvenIntervalDecimation
import com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.FilterUIs.KalmanUI
import com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.FilterUIs.MovingAverageUI
import com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.FilterUIs.RamerDouglasPeuckerUI
import com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.util.FilterUI
import com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.util.FilterUIUtils.createRoundedSpinner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


/**
 * FilterDialog implements ToolDialog<FilterParams> so it can be shown by ToolUiContext
 * and return the selected parameters from the user.
 */
class FilterDialog(val waypointCount: Int) : ToolDialog<FilterParams> {

    //      Waypoint   Trackpoint
    //GPX	80–150 B	90–200 B	Verbose, every <trkpt> has full XML tags
    //KML	100–150 B	20–30 B	    Uses single <coordinates> block for all points


    override val title: String = "Filter Track"

    // Make these class-level so displayFilterUI can access them
    private lateinit var toolDescription: TextView
    private lateinit var container: LinearLayout
    private lateinit var filterSpinner: Spinner
    private var resultParameters: FilterType? = null

    override suspend fun show(context: Any): FilterParams? {
        val activity = context as? Activity ?: return null

        return suspendCancellableCoroutine { continuation ->

            toolDescription = TextView(activity).apply {
                textAlignment = TEXT_ALIGNMENT_CENTER
                setPadding(10, 10, 10, 20)
            }

            filterSpinner = createRoundedSpinner(activity, FilterType.entries.map { it.label })
            filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedType = FilterType.entries[position]
                    displayFilterUI(activity, selectedType)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

            container = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
                addView(filterSpinner)
                addView(toolDescription)
            }

            val dialog = AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(container)
                .setPositiveButton("Apply") { dlg, _ ->
                    val selectedFilter = FilterType.entries[filterSpinner.selectedItemPosition]

                    if (!continuation.isCompleted) continuation.resume(
                        FilterParams(
                            resultParameters?:selectedFilter,
                        )
                    )
                    dlg.dismiss()
                }
                .setNegativeButton("Cancel") { dlg, _ ->
                    if (!continuation.isCompleted) continuation.resume(null)
                    dlg.dismiss()
                }
                .create()

            dialog.setOnShowListener {
                styleDialogWindow(dialog, activity)
                styleDialogButtons(dialog, activity)
            }

            dialog.show()
            continuation.invokeOnCancellation { dialog.dismiss() }
        }
    }

    private fun displayFilterUI(context: Context, filter: FilterType) {
        toolDescription.text = filter.description
        val ui = getFilterUI(filter)
        val filterView = ui.createView(context, waypointCount) { value ->
            Log.d("FilterDialog", "Value changed: $value")
            resultParameters = value
        }

        // Remove previous filter view if exists
        if (container.childCount > 2) {
            container.removeViewAt(2)
        }

        container.addView(filterView, 2)
    }

    private fun getFilterUI(filter: FilterType): FilterUI = when (filter) {
        is FilterType.RAMER_DOUGLAS_PEUCKER -> RamerDouglasPeuckerUI()
        is FilterType.DISTANCE_BASED -> DistanceBasedUI()
        is FilterType.MOVING_AVERAGE -> MovingAverageUI()
        is FilterType.KALMAN -> KalmanUI()
        is FilterType.EVEN_INTERVAL_DECIMATION -> EvenIntervalDecimation()
    }


}


private fun styleDialogWindow(dialog: AlertDialog, context: Context) {
    dialog.window?.setBackgroundDrawable(
        GradientDrawable().apply {
            cornerRadius = 32f
            setColor(ContextCompat.getColor(context, R.color.primary_blue))
        }
    )
}

private fun styleDialogButtons(dialog: AlertDialog, context: Context) {
    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
        textSize = 18f
        setPadding(32, 16, 32, 16)
        setTextColor(ContextCompat.getColor(context, R.color.apply))
    }
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
        textSize = 18f
        setPadding(32, 16, 32, 16)
        setTextColor(ContextCompat.getColor(context, R.color.cancel))
    }
}
