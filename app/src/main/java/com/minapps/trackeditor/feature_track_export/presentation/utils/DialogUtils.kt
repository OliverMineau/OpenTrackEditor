package com.minapps.trackeditor.feature_track_export.presentation.utils

import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

fun showSaveFileDialog(context: Context, onFileNameEntered: (String) -> Unit) {
    val input = EditText(context).apply {
        hint = "Enter file name (without extension)"
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    AlertDialog.Builder(context)
        .setTitle("Save Track")
        .setMessage("Enter the file name")
        .setView(input)
        .setPositiveButton("Save") { dialog, _ ->
            val fileName = input.text.toString().trim()
            if (fileName.isNotEmpty()) {
                // Append extension here, e.g. ".gpx"
                onFileNameEntered("$fileName.gpx")
            } else {
                Toast.makeText(context, "File name cannot be empty", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        .show()
}
