import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Color.*
import android.graphics.PorterDuff
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.children
import androidx.core.view.isEmpty
import com.minapps.trackeditor.R
import com.minapps.trackeditor.core.domain.util.ToolGroup
import com.minapps.trackeditor.feature_map_editor.presentation.ActionDescriptor
import com.minapps.trackeditor.feature_map_editor.presentation.ActionType
import com.minapps.trackeditor.feature_map_editor.presentation.util.vibrate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ToolboxPopup(
    private val popupContainer: FrameLayout,
    private val inflater: LayoutInflater,
    private val coroutineScope: CoroutineScope
) {

    private var isUnfolded = false
    private var isVisibleTop = false
    private var isAnimatingTop = false
    private var isAnimatingLeft = false
    private var collapsedWidth = 0

    private lateinit var unfoldButton: ImageView

    // Menu View
    private lateinit var popupView: View

    // List given by VM to populate tool menu
    lateinit var menuItems: List<ActionDescriptor>

    // Stores the selected item
    private var selectedItemCount: MutableMap<ActionType, Int> = mutableMapOf()
    private var selectedItemView: MutableMap<View, Boolean?> = mutableMapOf()
    private var lastSelectedItemView: View? = null

    //List of all tools to select and deselect
    var toolViews: MutableList<Pair<ActionDescriptor, View>> = mutableListOf()


    /**
     * Called to show popup menu
     *
     */
    fun show() {

        //If animations are currently running : return
        if (isAnimatingTop || isAnimatingLeft) return

        //If second click on menu item, close menus
        if (isVisibleTop) {
            hide()
            return
        }

        //Menu is visible
        isVisibleTop = true

        //Retrieve popup container
        if (popupContainer.isEmpty()) {
            popupView = inflater.inflate(R.layout.popup_toolbox_menu, popupContainer, false)
            popupContainer.addView(popupView)
            setupUI(popupView as ViewGroup)
        }

        if (isUnfolded) {
            resetToCollapsed(popupContainer.getChildAt(0) as ViewGroup)
            isUnfolded = false
        }

        // Wait for layout before measuring width and animating
        popupContainer.visibility = View.INVISIBLE
        popupContainer.post {
            // Ensure width is measured now
            val width = popupContainer.width
            popupContainer.translationX = width.toFloat()
            popupContainer.visibility = VISIBLE
            animatePopupShow() // this animates to translationX = 0f
        }
    }

    /**
     * Called to hide popup menu
     *
     */
    fun hide() {
        if (!isVisibleTop || isAnimatingTop || isAnimatingLeft) return

        clearSelection()

        isVisibleTop = false
        isAnimatingTop = true
        popupContainer.animate()
            .translationX(popupContainer.width.toFloat()) // Move it back off-screen right
            .setDuration(300)
            .withEndAction {
                popupContainer.visibility = GONE
                isAnimatingTop = false
            }
            .start()
    }

    /**
     * Retrieve unfold second menu button
     *
     * @param menuLayout
     */
    private fun setupUI(menuLayout: ViewGroup) {
        unfoldButton = menuLayout.findViewById(R.id.unfold_button)
        unfoldButton.setOnClickListener {
            if (isAnimatingLeft) return@setOnClickListener
            isAnimatingLeft = true
            toggleExpand(menuLayout)
        }

        populateTools()
    }

    /**
     * Called to expand second menu (display btn info)
     *
     * @param menuLayout
     */
    private fun toggleExpand(menuLayout: ViewGroup) {
        if (collapsedWidth == 0) {
            collapsedWidth = menuLayout.width
        }

        val expandedWidth = measureExpandedWidth(menuLayout)
        val startWidth = menuLayout.width
        val endWidth = if (isUnfolded) collapsedWidth else expandedWidth

        //Animate width of layout
        ValueAnimator.ofInt(startWidth, endWidth).apply {
            duration = 300
            addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                menuLayout.layoutParams.width = value
                menuLayout.layoutParams = menuLayout.layoutParams
            }
            start()
        }

        //Expand labels
        val toolboxChildren = (menuLayout.findViewById<LinearLayout>(R.id.toolbox_menu)).children
        toolboxChildren.forEach { child ->
            val label = (child as? LinearLayout)?.children?.filterIsInstance<TextView>()?.firstOrNull()
            label?.let {
                if (!isUnfolded) animateLabelExpand(it, child, menuLayout)
                else animateLabelCollapse(it, child, menuLayout)
            }
        }

        //Change fold button icon
        val newIcon = if (isUnfolded) R.drawable.angle_double_small_left_24
        else R.drawable.angle_double_small_right_24
        unfoldButton.setImageResource(newIcon)

        isUnfolded = !isUnfolded
    }

    /**
     * Animate label when menu expands
     *
     * @param label
     * @param child
     * @param menuLayout
     */
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
                //Begin boing animation
                onLabelAnimationEnd(menuLayout, R.drawable.boing_double_arrow_right)
            }
            .start()
    }

    /**
     * Animate label when menu collapses
     *
     * @param label
     * @param child
     * @param menuLayout
     */
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
                //Begin boing animation
                onLabelAnimationEnd(menuLayout, R.drawable.boing_double_arrow_left)
            }
            .start()
    }

    /**
     * Start boing animation
     *
     * @param menuLayout
     * @param iconRes
     */
    private fun onLabelAnimationEnd(menuLayout: ViewGroup, iconRes: Int) {
        isAnimatingLeft = false
        unfoldButton.setImageResource(iconRes)

        (unfoldButton.drawable as? AnimatedVectorDrawable)?.let { drawable ->
            startAnimatedDrawableRepeat(drawable, unfoldButton)
        }
    }

    /**
     * Infinite animation
     *
     * @param drawable
     * @param imageView
     */
    private fun startAnimatedDrawableRepeat(drawable: AnimatedVectorDrawable, imageView: ImageView) {
        val intervalMillis = 3000L

        fun startAndRepeat() {
            drawable.start()
            imageView.postDelayed({ startAndRepeat() }, intervalMillis)
        }
        startAndRepeat()
    }

    /**
     * Animate main popup from the right
     *
     */
    private fun animatePopupShow() {
        isAnimatingTop = true

        popupContainer.animate()
            .translationX(0f) // Animate in from the right
            .setDuration(300)
            .withEndAction {
                isAnimatingTop = false

                val menuLayout = popupContainer.getChildAt(0) as ViewGroup

                // Scroll to bottom
                val scrollView = menuLayout.findViewById<ScrollView>(R.id.scroll)
                scrollView.post {
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }

                // Set btn animation
                val imageView = menuLayout.findViewById<ImageView>(R.id.unfold_button)
                imageView.setImageResource(R.drawable.boing_double_arrow_left)
                (imageView.drawable as? AnimatedVectorDrawable)?.let { drawable ->
                    startAnimatedDrawableRepeat(drawable, imageView)
                }
            }
            .start()
    }

    /**
     * Collapse second menu
     *
     * @param menuLayout
     */
    private fun resetToCollapsed(menuLayout: ViewGroup) {
        if (collapsedWidth > 0) {
            menuLayout.layoutParams.width = collapsedWidth
            menuLayout.layoutParams = menuLayout.layoutParams
        }

        // Hide labels
        val toolboxChildren = (menuLayout.findViewById<LinearLayout>(R.id.toolbox_menu)).children
        toolboxChildren.forEach { child ->
            val label = (child as? LinearLayout)?.children?.filterIsInstance<TextView>()?.firstOrNull()
            label?.apply {
                alpha = 0f
                visibility = GONE
            }
        }

        //Change btn image
        unfoldButton.setImageResource(R.drawable.angle_double_small_left_24)
    }

    /**
     * Measure width of expanded menu  (before expanding it)
     *
     * @param menuLayout
     * @return
     */
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

    /**
     *  Populate menu with ViewModel UseCases when loaded
     *
     * @param menuLayout
     */
    fun populateTools() {

        if (menuItems.isEmpty()) return

        val toolboxMenu = popupContainer.findViewById<LinearLayout>(R.id.toolbox_menu)

        menuItems.reversed().forEach { item ->
            val itemView = inflater.inflate(R.layout.tool_item, toolboxMenu, false)

            if(item.group != ToolGroup.FILE_SYSTEM){
                toolViews.add(item to itemView)
            }

            if (item.icon == null || item.label == null || item.action == null || item.selectionCount == null) {
                itemView.findViewById<View>(R.id.separator).visibility = VISIBLE
                itemView.findViewById<ImageView>(R.id.icon).visibility = GONE
                itemView.findViewById<TextView>(R.id.label).visibility = GONE
            } else {
                val iconCurrent = itemView.findViewById<ImageView>(R.id.icon)
                iconCurrent.setImageResource(item.icon)
                val labelCurrent = itemView.findViewById<TextView>(R.id.label)
                labelCurrent.text = item.label

                itemView.setOnClickListener {

                    if(selectedItemView[itemView] == true){
                        selectedItemCount[item.type] = selectedItemCount.getOrDefault(item.type, 0) + 1
                    }else{
                        selectedItemView[itemView] = true
                        selectedItemCount[item.type] = 0
                    }

                    lastSelectedItemView = itemView

                    coroutineScope.launch {
                        item.action.invoke()
                    }
                }
            }
            toolboxMenu.addView(itemView, 0)
        }
    }

    /**
     * Clear selected items
     *
     */
    private fun clearSelection() {
        toolViews.forEach { item ->
            val color = ContextCompat.getColor(popupContainer.context, R.color.unselectedTool)
            changeColor(item.second, color)
        }

        selectedItemView.keys.forEach { view ->
            selectedItemView[view] = false
        }
        selectedItemCount.keys.forEach { key ->
            selectedItemCount[key] = 0
        }

    }


    fun toolSelected(tool: ActionType){

        toolViews.forEach { item ->

            // Type 2 tools don't change colour and affect the ui
            if (tool.group == ToolGroup.FILE_SYSTEM){
                return
            }

            val group = item.first.group
            val type = item.first.type
            val selCount = selectedItemCount.getOrDefault(tool, 0)

            var color: Int

            // TODO Maybe error here
            // If same group, deselect all except same action/tool or if same tool
            if(tool.group == ToolGroup.NONE && group == ToolGroup.NONE && tool == type){

                color = LTGRAY
                //Selected item once = selected
                if(selCount%2 == 0){
                    color = ContextCompat.getColor(popupContainer.context, R.color.selectedFeature)
                }

                changeColor(item.second, color)
            }
            // If group 0 then nothing is affected
            else if (group == ToolGroup.NONE){
                return@forEach
            }

            // If same group other than 0
            if(group == tool.group && group != ToolGroup.NONE){
                // If different tools, deselect
                if(type != tool){
                    color = LTGRAY
                    selectedItemView[item.second] = false
                }
                // If same tool, select
                else{
                    color = LTGRAY
                    //Selected item once = selected
                    if(selCount%2 == 0){
                        color = ContextCompat.getColor(popupContainer.context, R.color.selectedTool)
                    }else{
                        selectedItemView[item.second] = false
                    }
                }

                changeColor(item.second, color)
            }
            // If different group nothing is affected
            else return@forEach
        }

        if (isUnfolded) {
            toggleExpand(popupView as ViewGroup)
        }
    }

    private fun changeColor(view: View, color: Int){
        val icon = view.findViewById<ImageView>(R.id.icon)
        val label = view.findViewById<TextView>(R.id.label)
        icon?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        label?.setTextColor(color)
    }




}
