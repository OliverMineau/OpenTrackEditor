import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import com.minapps.trackeditor.R
import androidx.core.view.isEmpty

class ToolboxPopup(
    private val popupContainer: FrameLayout,
    private val inflater: LayoutInflater
) {

    private var isUnfolded = false
    private var isVisible = false
    private val expandSize = 200
    private lateinit var unfoldButton: ImageView

    fun show() {

        if(isVisible){
            hide()
            return
        }
        isVisible=true

        if (popupContainer.isEmpty()) {
            val popupView = inflater.inflate(R.layout.popup_toolbox_menu, popupContainer, false)
            popupContainer.addView(popupView)
            populateTools(popupView as ViewGroup)
            setupUnfoldButton(popupView as ViewGroup)
        }

        // If already unfolded, reset immediately to collapsed state (no animation)
        if (isUnfolded) {
            val menuLayout = popupContainer.getChildAt(0) as ViewGroup
            resetToCollapsed(menuLayout)
            isUnfolded = false
        }

        popupContainer.visibility = android.view.View.VISIBLE

        popupContainer.post {
            popupContainer.translationY = popupContainer.height.toFloat()
            popupContainer.animate()
                .translationY(0f)
                .setDuration(300)
                .start()
        }
    }

    fun hide() {

        if(!isVisible) return
        isVisible = false

        popupContainer.animate()
            .translationY(popupContainer.height.toFloat())
            .setDuration(300)
            .withEndAction {
                popupContainer.visibility = android.view.View.GONE
            }
            .start()
    }

    private fun setupUnfoldButton(menuLayout: ViewGroup) {
        unfoldButton = menuLayout.findViewById<ImageView>(R.id.unfold_button)
        unfoldButton.setOnClickListener {
            toggleExpand(menuLayout, unfoldButton)
        }
    }

    private fun toggleExpand(menuLayout: ViewGroup, unfoldButton: ImageView) {
        val initialWidth = menuLayout.width
        val targetWidth = if (isUnfolded) initialWidth - expandSize else initialWidth + expandSize

        val animator = ValueAnimator.ofInt(initialWidth, targetWidth)
        animator.duration = 300
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            val params = menuLayout.layoutParams
            params.width = value
            menuLayout.layoutParams = params
        }
        animator.start()

        // Animate label visibility fade in/out
        for (i in 0 until menuLayout.childCount) {
            val child = menuLayout.getChildAt(i)
            if (child is LinearLayout) {
                // Find any TextView inside this child LinearLayout
                val label = child.children.filterIsInstance<TextView>().firstOrNull()

                label?.let {
                    it.visibility = View.VISIBLE
                    it.alpha = 0f
                    it.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                }
            }
        }

        val newIcon = if (isUnfolded) R.drawable.angle_double_small_left_24 else R.drawable.angle_double_small_right_24
        unfoldButton.setImageResource(newIcon)

        isUnfolded = !isUnfolded
    }

    private fun resetToCollapsed(menuLayout: ViewGroup) {
        // Set width instantly to collapsed size
        val params = menuLayout.layoutParams
        params.width = menuLayout.width - expandSize
        menuLayout.layoutParams = params
        unfoldButton.setImageResource(R.drawable.angle_double_small_left_24)
    }

    private fun populateTools(menuLayout: ViewGroup) {
        val toolboxMenu = menuLayout.findViewById<LinearLayout>(R.id.toolbox_menu)
        //toolboxMenu.removeAllViews()  // Clear any existing items

        val menuItems = listOf(
            Pair(R.drawable.plus_24, "Add"),
            Pair(R.drawable.minus_24, "Remove"),
            Pair(R.drawable.file_export_24, "Export")
        )

        for ((iconRes, labelText) in menuItems) {
            val menuItemView = inflater.inflate(R.layout.tool_item, toolboxMenu, false)
            val iconView = menuItemView.findViewById<ImageView>(R.id.icon)
            val labelView = menuItemView.findViewById<TextView>(R.id.label)

            iconView.setImageResource(iconRes)
            labelView.text = labelText

            toolboxMenu.addView(menuItemView, 0)
        }

    }
}
