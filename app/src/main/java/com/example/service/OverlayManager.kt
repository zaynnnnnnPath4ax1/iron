package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var energyTextView: TextView? = null

    fun showOverlay(currentEnergy: Int) {
        if (!Settings.canDrawOverlays(context)) return
        if (overlayView != null) {
            updateEnergy(currentEnergy)
            return
        }

        try {
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dpToPx(36) // Safe margin below standard camera notch / status bar
            }

            val pillLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(12), dpToPx(6), dpToPx(14), dpToPx(6))

                // Futuristic Geometric Balance glass pill background
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(24).toFloat()
                    setColor(Color.argb(235, 12, 16, 26)) // Deep slate dark surface
                    setStroke(dpToPx(1), Color.argb(180, 160, 97, 255)) // Geometric violet border
                }
            }

            val lightningIcon = TextView(context).apply {
                text = "⚡"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(Color.parseColor("#A061FF")) // Geometric violet accent
                setPadding(0, 0, dpToPx(4), 0)
            }

            val energyText = TextView(context).apply {
                text = "$currentEnergy%"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD))
                setTextColor(Color.parseColor("#F8FAFC"))
            }

            energyTextView = energyText
            pillLayout.addView(lightningIcon)
            pillLayout.addView(energyText)

            // Make draggable and clickable
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isClick = false

            pillLayout.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false
                        }
                        layoutParams.x = initialX + dx
                        layoutParams.y = (initialY + dy).coerceAtLeast(dpToPx(16))
                        windowManager.updateViewLayout(view, layoutParams)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            // Tap opens VOLT app
                            val appIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            context.startActivity(appIntent)
                        }
                        true
                    }
                    else -> false
                }
            }

            overlayView = pillLayout
            windowManager.addView(pillLayout, layoutParams)
        } catch (e: Exception) {
            overlayView = null
        }
    }

    fun updateEnergy(currentEnergy: Int) {
        energyTextView?.text = "$currentEnergy%"
    }

    fun removeOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // View already detached
            }
        }
        overlayView = null
        energyTextView = null
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
