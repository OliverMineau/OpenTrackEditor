package com.minapps.trackeditor.feature_track_export.presentation.util

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.minapps.trackeditor.R
import com.minapps.trackeditor.feature_track_export.domain.model.ExportFormat
fun showSaveFileDialog(
    context: Context,
    onFileNameEntered: (String, ExportFormat, Boolean) -> Unit
) {
    val input = EditText(context).apply {
        hint = "Enter file name"
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        setTextColor(ContextCompat.getColor(context, R.color.black))
        setHintTextColor(ContextCompat.getColor(context, R.color.light_grey))
    }

    val spinner = createRoundedSpinner(context, ExportFormat.entries.map { it.name })

    //val checkboxEAT = CheckBox(context).apply { text = "Export all tracks" }

    val messageView = TextView(context).apply {
        text = "Enter file name and choose export type"
        setTextColor(ContextCompat.getColor(context, R.color.black))
        setPadding(0, 0, 0, 20)
    }

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(50, 40, 50, 10)
        addView(messageView)   // <-- add styled message first
        addView(input)
        addView(spinner)
        //addView(checkboxEAT)
    }

    val titleView = TextView(context).apply {
        text = "Export Track"
        setTextColor(ContextCompat.getColor(context, R.color.black))
        textSize = 20f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(40, 40, 40, 0)
    }

    val dialog = AlertDialog.Builder(context)
        .setCustomTitle(titleView)
        .setView(container)  // message is inside now
        .setPositiveButton("Export") { d, _ ->
            val fileName = input.text.toString().trim()
            val selectedName = spinner.selectedItem.toString()
            val format = ExportFormat.valueOf(selectedName)

            if (fileName.isNotEmpty() && isAllowed(fileName)) {
                onFileNameEntered("$fileName.${format.name.lowercase()}", format, false) //checkboxEAT.isChecked
            } else {
                val msg = if (fileName.isEmpty())
                    "File name cannot be empty"
                else "Illegal characters in file name.\nOnly use alphanumeric characters."
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
            d.dismiss()
        }
        .setNegativeButton("Cancel") { d, _ -> d.cancel() }
        .create()

    dialog.setOnShowListener {
        styleDialogWindow(dialog, context)
        styleDialogButtons(dialog, context)
    }

    dialog.show()
}

private fun createRoundedSpinner(context: Context, items: List<String>): Spinner {
    val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return super.getView(position, convertView, parent).apply {
                (this as? TextView)?.apply {
                    gravity = Gravity.CENTER
                    setTextColor(ContextCompat.getColor(context, R.color.exportDialog))
                    setBackgroundColor(ContextCompat.getColor(context, R.color.black))
                    setTypeface(typeface, Typeface.BOLD)
                }
            }
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return super.getDropDownView(position, convertView, parent).apply {
                (this as? TextView)?.apply {
                    gravity = Gravity.CENTER
                    setTextColor(ContextCompat.getColor(context, R.color.exportDialog))
                    setBackgroundColor(ContextCompat.getColor(context, R.color.black))
                    setTypeface(typeface, Typeface.BOLD)
                }
            }
        }
    }.also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

    return Spinner(context).apply {
        this.adapter = adapter
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setStroke(3, ContextCompat.getColor(context, R.color.light_grey))
            setColor(Color.BLACK)
        }
        setPadding(16, 16, 16, 16)
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
        setTextColor(Color.BLACK)
    }
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
        textSize = 18f
        setPadding(32, 16, 32, 16)
        setTextColor(Color.BLACK)
    }
}

fun isAllowed(fileName: String): Boolean = Regex("[\\w._-]+").matches(fileName)
