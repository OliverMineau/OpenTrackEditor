package com.minapps.trackeditor.feature_map_editor.tools.filter.presentation.util

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.minapps.trackeditor.R

object FilterUIUtils {

    fun createTextView(context: Context, paddingBottom: Int = 30): TextView {
        return TextView(context).apply {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(10, 10, 10, paddingBottom)
            setTextColor(ContextCompat.getColor(context, R.color.black))
        }
    }

    fun createRoundedSpinner(context: Context, items: List<String>): Spinner {
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
                setColor(ContextCompat.getColor(context, R.color.black))
                setStroke(3, ContextCompat.getColor(context, R.color.light_grey))
            }
            setPadding(16, 16, 16, 16)
        }
    }

}
