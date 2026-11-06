package com.drivepark.app.drive

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.PopupWindowPort
import com.drivepark.app.ui.pixel.CharacterRenderer
import com.drivepark.app.ui.pixel.Pose

/** 팝업 한 벌의 색. 순정 내비 지도가 테마를 따라가므로 팝업도 같은 톤으로 맞춘다. */
data class PopupColors(val background: Int, val border: Int, val text: Int) {
    companion object {
        /** 라이트: 흰 알약 + 연회색 테두리 + 검정 글씨(Pleos 라이트 카드와 같은 톤). */
        val LIGHT = PopupColors(Color.parseColor("#F2FFFFFF"), Color.parseColor("#E4E4E7"), Color.parseColor("#18181B"))

        /** 다크: 진회색 알약(앱 다크 카드 #1E1E22) + 은은한 테두리 + 흰 글씨. */
        val DARK = PopupColors(Color.parseColor("#EB1E1E22"), Color.parseColor("#3F3F46"), Color.parseColor("#F4F4F5"))
    }
}

/**
 * 순정 내비 위에 뜨는 알약형 주행 팝업. 왼쪽에는 내 캐릭터가 제자리걸음을 하고, 오른쪽에 "친구를 찾는 중…".
 * 캐릭터가 아직 없으면 주황 점이 깜빡인다. 터치를 받지 않고 아래 앱의 터치를 막지 않는다.
 * 권한: SYSTEM_ALERT_WINDOW (에뮬레이터: adb shell appops set --user 10 com.drivepark.app SYSTEM_ALERT_WINDOW allow)
 */
class OverlayPopupWindow(
    context: Context,
    private val myParts: () -> CharacterParts?,
    private val isDark: () -> Boolean,
) : PopupWindowPort {
    private val appContext = context.applicationContext
    private val wm = appContext.getSystemService(WindowManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var root: LinearLayout? = null
    private var label: TextView? = null
    private var pulse: ValueAnimator? = null
    private var walker: Runnable? = null

    fun canDraw(): Boolean = Settings.canDrawOverlays(appContext)

    override fun show(text: String) {
        main.post {
            runCatching {
                val current = root
                if (current != null) {
                    label?.text = text.removePrefix("● ")
                    return@post
                }
                val view = buildView(text.removePrefix("● "))
                wm.addView(view, layoutParams())
                root = view
                view.translationY = -dp(80f)
                view.animate().translationY(0f).setDuration(300).start()
            }.onFailure { Log.w(TAG, "show failed", it) }
        }
    }

    override fun hide() {
        main.post {
            val view = root ?: return@post
            root = null
            pulse?.cancel()
            walker?.let(main::removeCallbacks)
            walker = null
            view.animate().translationY(-dp(80f)).setDuration(250).withEndAction {
                runCatching { wm.removeView(view) }
            }.start()
        }
    }

    private fun buildView(text: String): LinearLayout {
        val colors = if (isDark()) PopupColors.DARK else PopupColors.LIGHT
        val icon = myParts()?.let(::walkingCharacter) ?: blinkingDot()
        val textView = TextView(appContext).apply {
            this.text = text
            setTextColor(colors.text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setPadding(dp(10f).toInt(), 0, 0, 0)
        }
        label = textView
        return LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18f).toInt(), 0, dp(28f).toInt(), 0)
            background = GradientDrawable().apply {
                setColor(colors.background)
                setStroke(dp(1.5f).toInt(), colors.border)
                cornerRadius = dp(36f)
            }
            addView(icon)
            addView(textView)
        }
    }

    /** 내 캐릭터 걷기 4장을 0.3초마다 넘긴다(공원에서 걷는 속도와 같음). 글자 쪽(오른쪽)을 보고 걷게 뒤집는다. */
    private fun walkingCharacter(parts: CharacterParts): View {
        val frames = (0..3).map { f -> pixelBitmap(parts, f) }
        val size = dp(60f).toInt()
        val image = ImageView(appContext).apply {
            layoutParams = LinearLayout.LayoutParams(size, size)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        var i = 0
        val tick = object : Runnable {
            override fun run() {
                image.setImageDrawable(BitmapDrawable(appContext.resources, frames[i % frames.size]).apply { isFilterBitmap = false })
                i++
                main.postDelayed(this, 300)
            }
        }
        walker = tick
        tick.run()
        return image
    }

    /** 도트를 정수배로 키워(보간 없이) 각진 픽셀을 유지한다. */
    private fun pixelBitmap(parts: CharacterParts, frame: Int): Bitmap {
        val img = CharacterRenderer.render(parts, Pose.WALK, frame)
        val raw = Bitmap.createBitmap(img.argb, img.width, img.height, Bitmap.Config.ARGB_8888)
        val k = 4f
        val m = Matrix().apply { setScale(-k, k) } // 좌우 뒤집기 + 4배
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, false)
    }

    private fun blinkingDot(): View {
        val dot = TextView(appContext).apply {
            this.text = "●"
            setTextColor(Color.parseColor("#FF7A1A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setPadding(dp(6f).toInt(), 0, 0, 0)
        }
        pulse = ObjectAnimator.ofFloat(dot, View.ALPHA, 1f, 0.3f).apply {
            duration = 600
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
        return dot
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        dp(72f).toInt(),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        // 차량 상단 시스템 바(TopCarSystemBar, 90px)가 오버레이보다 위에 그려지므로 그 아래에 배치되게 한다.
        fitInsetsTypes = android.view.WindowInsets.Type.statusBars()
        y = dp(24f).toInt()
        // Pleos 화면은 왼쪽 1/3이 차량 패널이고, 내비의 회전 안내 카드가 지도 영역 왼쪽 위에 있다.
        // 안내를 가리지 않도록 지도 영역(오른쪽 2/3)의 가운데로 옮긴다.
        x = displayWidth() / 6
    }

    /** 앱 창 폭이 아니라 화면 전체(물리) 폭. */
    private fun displayWidth(): Int = runCatching {
        appContext.getSystemService(android.hardware.display.DisplayManager::class.java)
            .getDisplay(android.view.Display.DEFAULT_DISPLAY).mode.physicalWidth
    }.getOrDefault(appContext.resources.displayMetrics.widthPixels)

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, appContext.resources.displayMetrics)

    private companion object {
        const val TAG = "DrivePark/Popup"
    }
}
