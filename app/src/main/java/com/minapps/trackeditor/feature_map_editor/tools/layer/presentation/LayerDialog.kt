package com.minapps.trackeditor.feature_map_editor.tools.layer.presentation

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.minapps.trackeditor.R
import com.minapps.trackeditor.core.domain.tool.ToolDialog
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterParams
import com.minapps.trackeditor.feature_map_editor.tools.filter.domain.model.FilterType
import com.minapps.trackeditor.feature_map_editor.tools.layer.domain.model.LayerParams
import com.minapps.trackeditor.feature_map_editor.tools.layer.domain.model.LayerType
import com.minapps.trackeditor.feature_map_editor.tools.layer.presentation.util.UIUtils.createRoundedSpinner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


/**
 * FilterDialog implements ToolDialog<FilterParams> so it can be shown by ToolUiContext
 * and return the selected parameters from the user.
 */
class LayerDialog(val type: LayerType) : ToolDialog<LayerParams> {

    
    override val title: String = "Change Map Layer"
    private lateinit var toolDescription: TextView
    private lateinit var container: LinearLayout
    private lateinit var layerSpinner: Spinner
    private var resultParameters: LayerType? = null

    /**
     * Show dialog
     *
     * @param context
     * @return
     */
    override suspend fun show(context: Any): LayerParams? {
        val activity = context as? Activity ?: return null

        return suspendCancellableCoroutine { continuation ->

            toolDescription = TextView(activity).apply {
                textAlignment = TEXT_ALIGNMENT_CENTER
                setPadding(10, 10, 10, 20)
                setTextColor(ContextCompat.getColor(context, R.color.black))
            }

            layerSpinner = createRoundedSpinner(activity, LayerType.entries.map { it.label })

            container = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
                addView(layerSpinner)
                addView(toolDescription)
            }

            val titleView = TextView(context).apply {
                text = title
                setTextColor(ContextCompat.getColor(context, R.color.black))
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(40, 40, 40, 0)
            }

            val dialog = AlertDialog.Builder(activity)
                .setCustomTitle(titleView)
                .setView(container)
                .setPositiveButton("Apply") { dlg, _ ->
                    val selectedLayer = LayerType.entries[layerSpinner.selectedItemPosition]
                    if (!continuation.isCompleted) continuation.resume(
                        LayerParams(
                            resultParameters?:selectedLayer,
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
}


private fun styleDialogWindow(dialog: AlertDialog, context: Context) {
    dialog.window?.setBackgroundDrawable(
        GradientDrawable().apply {
            cornerRadius = 32f
            setColor(ContextCompat.getColor(context, R.color.exportDialog))
        }
    )
}

private fun styleDialogButtons(dialog: AlertDialog, context: Context) {
    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
        textSize = 18f
        setPadding(32, 16, 32, 16)
        setTextColor(ContextCompat.getColor(context, R.color.black))
    }
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
        textSize = 18f
        setPadding(32, 16, 32, 16)
        setTextColor(ContextCompat.getColor(context, R.color.black))
    }
}
