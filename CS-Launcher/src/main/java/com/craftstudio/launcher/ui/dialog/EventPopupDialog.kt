package com.craftstudio.launcher.ui.dialog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.craftstudio.launcher.R

class EventPopupDialog(
    context: Context,
    private val onNavigateToCursorStudio: (() -> Unit)? = null
) : android.app.Dialog(context, R.style.EventPopupStyle) {

    enum class ActionType {
        OPEN_URL,
        NAVIGATE_CURSOR_STUDIO
    }

    data class PopupItem(
        val imageRes: Int,
        val title: String,
        val body: String,
        val actionLabel: String,
        val actionType: ActionType,
        val actionUrl: String? = null,
        val showDontShowAgain: Boolean = false
    )

    private val popupQueue = mutableListOf<PopupItem>()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var currentIndex = 0
    private var dontShowAgain = false
    private var isTransitionRunning = false

    private lateinit var cardView: View
    private lateinit var promoImage: ImageView
    private lateinit var promoTitle: TextView
    private lateinit var promoBody: TextView
    private lateinit var actionButton: TextView
    private lateinit var btnClose: ImageButton
    private lateinit var checkboxDontShow: CheckBox

    companion object {
        private const val PREFS_NAME = "event_popup_prefs"
        private const val KEY_DONT_SHOW = "dont_show_events"
        private var isPromoShownInSession = false

        fun shouldShow(context: Context): Boolean {
            if (isPromoShownInSession) return false
            val permanentlyDismissed = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_DONT_SHOW, false)
            return !permanentlyDismissed
        }

        fun markShownThisSession() {
            isPromoShownInSession = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_event_popup)

        markShownThisSession()

        val displayMetrics = context.resources.displayMetrics
        val dialogWidth = (displayMetrics.widthPixels * 0.92f).toInt()

        window?.apply {
            setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0f)
        }

        initViews()
        setupQueue()
        bindCurrentPopup()
    }

    private fun initViews() {
        cardView = findViewById(R.id.event_card)
        promoImage = findViewById(R.id.promo_image)
        promoTitle = findViewById(R.id.promo_title)
        promoBody = findViewById(R.id.promo_body)
        actionButton = findViewById(R.id.promo_action_button)
        btnClose = findViewById(R.id.btn_close)
        checkboxDontShow = findViewById(R.id.checkbox_dont_show)

        findViewById<View>(R.id.event_popup_root).setOnClickListener { advanceOrDismiss() }
        cardView.setOnClickListener { advanceOrDismiss() }
        btnClose.setOnClickListener { advanceOrDismiss() }
        promoImage.setOnClickListener { handlePrimaryAction() }
        actionButton.setOnClickListener { handlePrimaryAction() }

        checkboxDontShow.setOnCheckedChangeListener { _, isChecked ->
            dontShowAgain = isChecked
        }
    }

    private fun setupQueue() {
        popupQueue.clear()
        popupQueue += PopupItem(
            imageRes = R.drawable.promo_discord,
            title = "Join Our Official Discord",
            body = "Get support, updates, and launch notes in one place.",
            actionLabel = "Open Discord",
            actionType = ActionType.OPEN_URL,
            actionUrl = "https://discord.gg/RcazEkcFWR"
        )
        popupQueue += PopupItem(
            imageRes = R.drawable.promo_website,
            title = "Visit Our Official Website",
            body = "Read the latest releases, news, and launcher updates.",
            actionLabel = "Open Website",
            actionType = ActionType.OPEN_URL,
            actionUrl = "https://cs-launcher.netlify.app/"
        )
        popupQueue += PopupItem(
            imageRes = R.drawable.promo_mouse,
            title = "New Feature: Custom Mouse Cursor",
            body = "Open Cursor Studio to build and preview a custom pointer pack.",
            actionLabel = "Open Cursor Studio",
            actionType = ActionType.NAVIGATE_CURSOR_STUDIO,
            showDontShowAgain = true
        )
    }

    private fun bindCurrentPopup() {
        if (currentIndex >= popupQueue.size) {
            completeSequence(null)
            return
        }

        val item = popupQueue[currentIndex]
        promoImage.setImageResource(item.imageRes)
        promoTitle.text = item.title
        promoBody.text = item.body
        actionButton.text = item.actionLabel

        val showDontShow = item.showDontShowAgain
        checkboxDontShow.visibility = if (showDontShow) View.VISIBLE else View.GONE
        if (showDontShow) {
            checkboxDontShow.isChecked = dontShowAgain
        }

        animateCardIn()
    }

    private fun animateCardIn() {
        isTransitionRunning = true
        cardView.animate().cancel()
        cardView.alpha = 0f
        cardView.scaleX = 0.94f
        cardView.scaleY = 0.94f
        cardView.translationY = 18f
        cardView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(280L)
            .setInterpolator(OvershootInterpolator(1.08f))
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isTransitionRunning = false
                }
            })
            .start()
    }

    private fun animateCardOut(onEnd: () -> Unit) {
        isTransitionRunning = true
        cardView.animate().cancel()
        cardView.animate()
            .alpha(0f)
            .scaleX(0.92f)
            .scaleY(0.92f)
            .translationY(12f)
            .setDuration(170L)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })
            .start()
    }

    private fun advanceOrDismiss() {
        if (isTransitionRunning) return

        val lastIndex = popupQueue.lastIndex
        if (currentIndex >= lastIndex) {
            completeSequence(null)
            return
        }

        animateCardOut {
            currentIndex++
            bindCurrentPopup()
        }
    }

    private fun handlePrimaryAction() {
        if (isTransitionRunning) return

        val item = popupQueue.getOrNull(currentIndex) ?: return
        when (item.actionType) {
            ActionType.OPEN_URL -> {
                item.actionUrl?.let { openUrl(it) }
                advanceAfterPrimaryAction()
            }
            ActionType.NAVIGATE_CURSOR_STUDIO -> {
                completeSequence(onNavigateToCursorStudio)
            }
        }
    }

    private fun advanceAfterPrimaryAction() {
        val lastIndex = popupQueue.lastIndex
        if (currentIndex >= lastIndex) {
            completeSequence(null)
            return
        }

        animateCardOut {
            currentIndex++
            bindCurrentPopup()
        }
    }

    private fun completeSequence(postDismissAction: (() -> Unit)?) {
        if (isTransitionRunning) return

        if (dontShowAgain || checkboxDontShow.isChecked) {
            prefs.edit().putBoolean(KEY_DONT_SHOW, true).apply()
        }

        animateCardOut {
            dismiss()
            postDismissAction?.invoke()
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (ignored: ActivityNotFoundException) {
        } catch (ignored: Throwable) {
        }
    }
}
