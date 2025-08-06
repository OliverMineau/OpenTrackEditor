import android.animation.ValueAnimator
import android.graphics.drawable.AnimatedVectorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import androidx.core.view.isEmpty
import com.minapps.trackeditor.R

class ToolboxPopup_back(
    private val popupContainer: FrameLayout,
    private val inflater: LayoutInflater
) {

    private var isUnfolded = false
    private var isVisibleTop = false
    private var isAnimatingTop = false
    private var isAnimatingLeft = false

    private var collapsedWidth = 0
    private lateinit var unfoldButton: ImageView
    private lateinit var separator: View

    fun show() {
        if (isAnimatingTop || isAnimatingLeft) return

        if (isVisibleTop) {
            hide()
            return
        }

        isVisibleTop = true

        if (popupContainer.isEmpty()) {
            val popupView = inflater.inflate(R.layout.popup_toolbox_menu, popupContainer, false)
            popupContainer.addView(popupView)
            setupUI(popupView as ViewGroup)
        }

        if (isUnfolded) {
            resetToCollapsed(popupContainer.getChildAt(0) as ViewGroup)
            isUnfolded = false
        }

        popupContainer.visibility = View.VISIBLE
        popupContainer.post {
            animatePopupShow()
        }
    }

    fun hide() {
        if (!isVisibleTop || isAnimatingTop || isAnimatingLeft) return

        isVisibleTop = false
        isAnimatingTop = true
        popupContainer.animate()
            .translationY(popupContainer.height.toFloat())
            .setDuration(300)
            .withEndAction {
                popupContainer.visibility = GONE
                isAnimatingTop = false
            }
            .start()
    }

    private fun setupUI(menuLayout: ViewGroup) {
        unfoldButton = menuLayout.findViewById(R.id.unfold_button)
        separator = menuLayout.findViewById(R.id.separator)

        unfoldButton.setOnClickListener {
            if (isAnimatingLeft) return@setOnClickListener
            isAnimatingLeft = true
            toggleExpand(menuLayout)
        }

        populateTools(menuLayout)
    }

    private fun toggleExpand(menuLayout: ViewGroup) {
        if (collapsedWidth == 0) {
            collapsedWidth = menuLayout.width
        }

        val expandedWidth = measureExpandedWidth(menuLayout)
        val startWidth = menuLayout.width
        val endWidth = if (isUnfolded) collapsedWidth else expandedWidth

        ValueAnimator.ofInt(startWidth, endWidth).apply {
            duration = 300
            addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                menuLayout.layoutParams.width = value
                menuLayout.layoutParams = menuLayout.layoutParams
            }
            start()
        }

        val toolboxChildren = (menuLayout.findViewById<LinearLayout>(R.id.toolbox_menu)).children

        toolboxChildren.forEach { child ->
            val label = (child as? LinearLayout)?.children?.filterIsInstance<TextView>()?.firstOrNull()
            label?.let {
                if (!isUnfolded) animateLabelExpand(it, child, menuLayout)
                else animateLabelCollapse(it, child, menuLayout)
            }
        }

        val newIcon = if (isUnfolded) R.drawable.angle_double_small_left_24
        else R.drawable.angle_double_small_right_24
        unfoldButton.setImageResource(newIcon)

        isUnfolded = !isUnfolded
    }

    private fun animateLabelExpand(label: TextView, child: ViewGroup, menuLayout: ViewGroup) {
        label.visibility = VISIBLE
        label.alpha = 0f
        label.animate()
            .alpha(1f)
            .setDuration(300)
            .withStartAction {
                child.findViewById<View>(R.id.separator)?.apply {
                    layoutParams.width = label.layoutParams.width
                    requestLayout()
                }
            }
            .withEndAction {
                onLabelAnimationEnd(menuLayout, R.drawable.boing_double_arrow_right)
            }
            .start()
    }

    private fun animateLabelCollapse(label: TextView, child: ViewGroup, menuLayout: ViewGroup) {
        label.animate()
            .alpha(0f)
            .setDuration(150)
            .withStartAction {
                child.findViewById<View>(R.id.separator)?.apply {
                    layoutParams.width = label.layoutParams.width // or fixed dpToPx(24)
                    requestLayout()
                }
            }
            .withEndAction {
                label.visibility = GONE
                onLabelAnimationEnd(menuLayout, R.drawable.boing_double_arrow_left)
            }
            .start()
    }

    private fun onLabelAnimationEnd(menuLayout: ViewGroup, iconRes: Int) {
        isAnimatingLeft = false
        unfoldButton.setImageResource(iconRes)

        (unfoldButton.drawable as? AnimatedVectorDrawable)?.let { drawable ->
            startAnimatedDrawableRepeat(drawable, unfoldButton)
        }
    }

    private fun startAnimatedDrawableRepeat(drawable: AnimatedVectorDrawable, imageView: ImageView) {
        val intervalMillis = 3000L

        fun startAndRepeat() {
            drawable.start()
            imageView.postDelayed({ startAndRepeat() }, intervalMillis)
        }
        startAndRepeat()
    }

    private fun animatePopupShow() {
        isAnimatingTop = true
        popupContainer.translationY = popupContainer.height.toFloat()
        popupContainer.animate()
            .translationY(0f)
            .setDuration(300)
            .withEndAction {
                isAnimatingTop = false
                val menuLayout = popupContainer.getChildAt(0) as ViewGroup

                /* menuLayout.findViewById<ScrollView>(R.id.scroll)?.post {
                     it.fullScroll(View.FOCUS_DOWN)
                 }*/

                val scrollView = menuLayout.findViewById<ScrollView>(R.id.scroll)
                scrollView.post {
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }

                val imageView = menuLayout.findViewById<ImageView>(R.id.unfold_button)
                imageView.setImageResource(R.drawable.boing_double_arrow_left)

                (imageView.drawable as? AnimatedVectorDrawable)?.let { drawable ->
                    startAnimatedDrawableRepeat(drawable, imageView)
                }
            }
            .start()
    }

    private fun resetToCollapsed(menuLayout: ViewGroup) {
        if (collapsedWidth > 0) {
            menuLayout.layoutParams.width = collapsedWidth
            menuLayout.layoutParams = menuLayout.layoutParams
        }

        val toolboxChildren = (menuLayout.findViewById<LinearLayout>(R.id.toolbox_menu)).children
        toolboxChildren.forEach { child ->
            val label = (child as? LinearLayout)?.children?.filterIsInstance<TextView>()?.firstOrNull()
            label?.apply {
                alpha = 0f
                visibility = GONE
            }
        }

        unfoldButton.setImageResource(R.drawable.angle_double_small_left_24)
    }

    private fun measureExpandedWidth(menuLayout: ViewGroup): Int {
        val toolboxChildren = (menuLayout.findViewById<LinearLayout>(R.id.toolbox_menu)).children
        toolboxChildren.forEach { child ->
            val label = (child as? LinearLayout)?.children?.filterIsInstance<TextView>()?.firstOrNull()
            label?.visibility = VISIBLE
        }

        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(menuLayout.height, View.MeasureSpec.AT_MOST)
        menuLayout.measure(widthSpec, heightSpec)

        return menuLayout.measuredWidth
    }

    private fun populateTools(menuLayout: ViewGroup) {
        val toolboxMenu = menuLayout.findViewById<LinearLayout>(R.id.toolbox_menu)

        val menuItems = listOf(
            R.drawable.file_export_24 to "Export",
            null to null,
            R.drawable.trash_24 to "Clear",
            null to null,
            R.drawable.map_marker_cross_24 to "Filter",
            R.drawable.map_marker_edit_24 to "Reverse",
            R.drawable.map_marker_plus_24 to "Join",
            R.drawable.map_marker_minus_24 to "Remove",
            null to null,
            R.drawable.layers_24 to "Layers",
            null to null,
        )



        menuItems.reversed().forEach { (iconRes, labelText) ->
            val itemView = inflater.inflate(R.layout.tool_item, toolboxMenu, false)
            if (iconRes == null) {
                itemView.findViewById<View>(R.id.separator).visibility = VISIBLE
                itemView.findViewById<ImageView>(R.id.icon).visibility = GONE
                itemView.findViewById<TextView>(R.id.label).visibility = GONE
            } else {
                itemView.findViewById<ImageView>(R.id.icon).setImageResource(iconRes)
                itemView.findViewById<TextView>(R.id.label).text = labelText
            }
            toolboxMenu.addView(itemView, 0)
        }
    }
}
