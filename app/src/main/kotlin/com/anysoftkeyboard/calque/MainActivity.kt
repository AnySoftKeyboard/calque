package com.anysoftkeyboard.calque

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.anysoftkeyboard.calque.di.AppContainer
import com.anysoftkeyboard.calque.ui.nav.NavDestination
import com.anysoftkeyboard.calque.ui.reader.ReaderView
import com.anysoftkeyboard.calque.ui.theme.CalqueColors

/** Main activity hosting the primary navigation shell of Calque. */
class MainActivity : Activity() {

    private lateinit var appContainer: AppContainer
    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var contentContainer: FrameLayout
    private lateinit var placeholderView: TextView
    private lateinit var readerView: ReaderView
    private val tabViews = mutableMapOf<NavDestination, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as? CalqueApplication
        appContainer = app?.appContainer ?: return

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(CalqueColors.Light.background)
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }

        root.addView(createHeader())
        root.addView(createContentArea())
        root.addView(createBottomNav())

        setContentView(root)

        appContainer.navStateManager.addListener { destination -> updateDestination(destination) }
        updateDestination(appContainer.navStateManager.currentDestination)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (readerView.handleBackPressed()) {
            return
        }
        super.onBackPressed()
    }

    private fun createHeader(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(12))
            setBackgroundColor(CalqueColors.Light.surface)

            titleView =
                TextView(context).apply {
                    text = context.getString(R.string.app_name)
                    textSize = 22f
                    typeface = Typeface.SERIF
                    setTextColor(CalqueColors.Light.textPrimary)
                }
            addView(titleView)

            subtitleView =
                TextView(context).apply {
                    textSize = 13f
                    setTextColor(CalqueColors.Light.textSecondary)
                }
            addView(subtitleView)
        }
    }

    private fun createContentArea(): View {
        contentContainer =
            FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            }

        readerView =
            ReaderView(this, appContainer).apply {
                layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }
        contentContainer.addView(readerView)

        placeholderView =
            TextView(this).apply {
                textSize = 16f
                typeface = Typeface.SERIF
                gravity = Gravity.CENTER
                setPadding(dp(24), dp(24), dp(24), dp(24))
                setTextColor(CalqueColors.Light.textPrimary)
                layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }
        contentContainer.addView(placeholderView)

        return contentContainer
    }

    private fun createBottomNav(): View {
        val nav =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(CalqueColors.Light.surface)
                setPadding(dp(8), dp(10), dp(8), dp(12))
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
            }

        for (dest in NavDestination.entries) {
            val tab = createTab(dest)
            tabViews[dest] = tab
            nav.addView(tab)
        }
        return nav
    }

    private fun createTab(dest: NavDestination): TextView {
        return TextView(this).apply {
            text = dest.title
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { appContainer.navStateManager.navigateTo(dest) }
        }
    }

    private fun updateDestination(destination: NavDestination) {
        val titleText = getString(R.string.app_name) + " — " + destination.title
        titleView.text = titleText
        subtitleView.text = destination.subtitle

        if (destination == NavDestination.READER) {
            readerView.visibility = View.VISIBLE
            placeholderView.visibility = View.GONE
        } else {
            readerView.visibility = View.GONE
            placeholderView.visibility = View.VISIBLE
            placeholderView.text = buildDestinationContent(destination)
        }

        for ((dest, tab) in tabViews) {
            val isSelected = dest == destination
            tab.setTextColor(
                if (isSelected) {
                    CalqueColors.Light.slateBlueAccent
                } else {
                    CalqueColors.Light.textSecondary
                }
            )
            tab.typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private fun buildDestinationContent(destination: NavDestination): String {
        return when (destination) {
            NavDestination.READER -> ""
            NavDestination.TOOLBOX ->
                "Toolbox Toys:\n\n• False Friend Radar\n• Word Transformer\n• " +
                    "Tense Shifter\n• Number Odometer\n• Clitic Surgery"
            NavDestination.VAULT -> "Vault:\n\nSaved words and custom texts will appear here."
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
