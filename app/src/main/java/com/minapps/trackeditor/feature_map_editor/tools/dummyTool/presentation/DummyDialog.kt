package com.minapps.trackeditor.feature_map_editor.tools.dummyTool.presentation

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
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
import com.minapps.trackeditor.feature_map_editor.domain.model.EditState
import com.minapps.trackeditor.feature_map_editor.tools.dummyTool.domain.model.DummyParams
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
 * DummyDialog implements ToolDialog<DummyParams> so it can be shown by ToolUiContext
 * and return the parameters selected by the user.
 */
class DummyDialog(val dummyData: EditState) : ToolDialog<DummyParams> {

    // Set the tool name
    override val title: String = "Dummy Track"

    /**
     * Show dialog
     *
     * @param context
     * @return
     */
    override suspend fun show(context: Any): DummyParams? {

        val dummySelection = DummyParams(5, true)

        return dummySelection
    }




}

