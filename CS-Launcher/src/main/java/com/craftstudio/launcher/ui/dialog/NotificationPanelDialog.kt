package com.craftstudio.launcher.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.craftstudio.launcher.R

class NotificationPanelDialog(context: Context) : Dialog(context, R.style.CustomDialogStyle) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_notification_panel)

        window?.apply {
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.TOP or Gravity.END)
            attributes?.windowAnimations = R.style.QuickSettingsAnimation
        }

        setupViews()
    }

    private fun setupViews() {
        val btnClose = findViewById<ImageView>(R.id.btn_close)
        val emptyState = findViewById<LinearLayout>(R.id.empty_state)

        btnClose.setOnClickListener { dismiss() }

        val notifications = listOf(
            NotificationItem("Welcome to CS Launcher", "Explore the new features!", "2 min ago", R.drawable.ic_bell),
            NotificationItem("Update Available", "Version 2.0 is ready to download", "1 hour ago", R.drawable.ic_bell),
            NotificationItem("Community Update", "Join our Discord server", "3 hours ago", R.drawable.ic_bell)
        )

        val container = findViewById<LinearLayout>(R.id.notifications_container)
        if (container != null && notifications.isNotEmpty()) {
            emptyState?.visibility = View.GONE
            notifications.forEachIndexed { index, notification ->
                val itemView = inflateNotificationItem(notification)
                container.addView(itemView)
                animateItemEntry(itemView, index * 80L)
            }
        } else {
            emptyState?.visibility = View.VISIBLE
        }
    }

    private fun inflateNotificationItem(item: NotificationItem): View {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_notification, null, false)

        itemView.findViewById<TextView>(R.id.notif_title).text = item.title
        itemView.findViewById<TextView>(R.id.notif_message).text = item.message
        itemView.findViewById<TextView>(R.id.notif_time).text = item.time
        itemView.findViewById<ImageView>(R.id.notif_icon).setImageResource(item.iconRes)

        return itemView
    }

    private fun animateItemEntry(view: View, delay: Long) {
        view.alpha = 0f
        view.translationY = 20f
        view.scaleX = 0.95f
        view.scaleY = 0.95f

        view.postDelayed({
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator(1.3f))
                .start()
        }, delay)
    }

    data class NotificationItem(
        val title: String,
        val message: String,
        val time: String,
        val iconRes: Int = R.drawable.ic_bell
    )
}
