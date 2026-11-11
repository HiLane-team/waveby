package com.drivepark.app.drive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.Typeface
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.drivepark.app.domain.ParkBannerPort
import com.drivepark.app.domain.ParkKeeper
import com.drivepark.app.ui.pixel.CharacterRenderer
import com.drivepark.app.ui.pixel.Pose

/** 안내 박스 한 벌의 색. Pleos 라이트/다크 카드와 같은 톤. 주황은 왼쪽 막대 하나만. */
private data class BannerColors(
    val card: Int, val border: Int, val title: Int, val subtitle: Int, val tile: Int,
    val button: Int, val buttonText: Int, val close: Int,
) {
    companion object {
        val LIGHT = BannerColors(
            Color.WHITE, Color.parseColor("#E4E4E7"), Color.parseColor("#18181B"), Color.parseColor("#71717A"),
            Color.parseColor("#F4F4F5"), Color.BLACK, Color.WHITE, Color.parseColor("#18181B"),
        )
        val DARK = BannerColors(
            Color.parseColor("#1E1E22"), Color.parseColor("#3F3F46"), Color.parseColor("#F4F4F5"), Color.parseColor("#A1A1AA"),
            Color.parseColor("#2E2E33"), Color.parseColor("#F4F4F5"), Color.parseColor("#18181B"), Color.parseColor("#F4F4F5"),
        )
        const val ACCENT = 0xFFFF7A1A.toInt()
    }
}

/**
 * 주차 10분 뒤 순정 내비 위에 뜨는 안내 박스(Pleos 카드 모양): 주황 막대 · 이호랑 · 제목/작은 글씨 · [공원 보기] · ×.
 * 주행 팝업과 같은 '다른 앱 위에 표시' 창이지만 버튼을 눌러야 하므로 터치를 받는다.
 * 박스 바깥 터치는 아래 앱(내비)으로 그대로 보낸다(FLAG_NOT_TOUCH_MODAL). 주차 중에만 뜬다([IdleParkBanner]).
 */
class ParkBannerWindow(
    context: Context,
    private val isDark: () -> Boolean,
    private val onOpenPark: () -> Unit,
    private val onClose: () -> Unit,
) : ParkBannerPort {
    private val appContext = context.applicationContext
    private val wm = appContext.getSystemService(WindowManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var root: View? = null

    fun canDraw(): Boolean = Settings.canDrawOverlays(appContext)

    override fun show(title: String, subtitle: String) {
        main.post {
            runCatching {
                root?.let { old -> root = null; runCatching { wm.removeView(old) } }
                val view = buildView(title, subtitle)
                wm.addView(view, layoutParams())
                root = view
                view.alpha = 0f
                view.translationY = -dp(40f)
                view.animate().alpha(1f).translationY(0f).setDuration(300).start()
            }.onFailure { Log.w(TAG, "show failed", it) }
        }
    }

    override fun hide() {
        main.post {
            val view = root ?: return@post
            root = null
            view.animate().alpha(0f).translationY(-dp(40f)).setDuration(250).withEndAction {
                runCatching { wm.removeView(view) }
            }.start()
        }
    }

    private fun buildView(title: String, subtitle: String): View {
        val c = if (isDark()) BannerColors.DARK else BannerColors.LIGHT
        val card = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(c.card)
                setStroke(dp(2f).toInt(), c.border)
                cornerRadius = dp(8f)
            }
            elevation = dp(6f)
        }
        // 왼쪽 주황 막대(강조색 하나).
        card.addView(View(appContext).apply {
            setBackgroundColor(BannerColors.ACCENT)
            layoutParams = LinearLayout.LayoutParams(dp(6f).toInt(), dp(88f).toInt()).apply { marginStart = dp(1f).toInt() }
        })
        // 이호랑(공원지기) 칸.
        card.addView(FrameLayout(appContext).apply {
            setBackgroundColor(c.tile)
            layoutParams = LinearLayout.LayoutParams(dp(88f).toInt(), dp(88f).toInt()).apply { marginStart = dp(21f).toInt() }
            addView(ImageView(appContext).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageDrawable(BitmapDrawable(appContext.resources, keeperBitmap()).apply { isFilterBitmap = false })
                layoutParams = FrameLayout.LayoutParams(dp(72f).toInt(), dp(72f).toInt(), Gravity.CENTER)
            })
        })
        // 제목 + 작은 글씨.
        card.addView(LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24f).toInt(), 0, dp(24f).toInt(), 0)
            addView(text(title, 26f, c.title, bold = true))
            addView(text("Waveby · $subtitle", 21f, c.subtitle, bold = false).apply { setPadding(0, dp(6f).toInt(), 0, 0) })
        })
        // [공원 보기] 주 버튼(Pleos 검정 바처럼 직각).
        card.addView(text("공원 보기", 24f, c.buttonText, bold = true).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(c.button)
            layoutParams = LinearLayout.LayoutParams(dp(196f).toInt(), dp(72f).toInt()).apply { marginStart = dp(16f).toInt() }
            setOnClickListener { onOpenPark() }
        })
        // 구분선 + 닫기(90×128 터치 칸).
        card.addView(View(appContext).apply {
            setBackgroundColor(c.border)
            layoutParams = LinearLayout.LayoutParams(dp(2f).toInt(), dp(88f).toInt()).apply { marginStart = dp(16f).toInt() }
        })
        card.addView(text("✕", 30f, c.close, bold = false).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(88f).toInt(), dp(128f).toInt())
            setOnClickListener { onClose() }
        })
        return FrameLayout(appContext).apply {
            // 그림자가 잘리지 않게 바깥 여백.
            setPadding(dp(8f).toInt(), dp(4f).toInt(), dp(8f).toInt(), dp(12f).toInt())
            addView(card, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, dp(128f).toInt()))
        }
    }

    private fun text(s: String, sp: Float, color: Int, bold: Boolean) = TextView(appContext).apply {
        text = s
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        maxLines = 1
    }

    /** 공원지기 이호랑(서 있는 모습)을 정수배로 키운 도트 그림. */
    private fun keeperBitmap(): Bitmap {
        val k = ParkKeeper.CHARACTER
        val img = CharacterRenderer.render(k.parts, Pose.IDLE, 0, shape = k.shape, look = k.look, kind = k.kind, drawing = k.drawing)
        val raw = Bitmap.createBitmap(img.argb, img.width, img.height, Bitmap.Config.ARGB_8888)
        val m = Matrix().apply { setScale(3f, 3f) }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, false)
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // 터치는 받되(버튼) 포커스는 가져가지 않고, 박스 바깥 터치는 아래 앱으로 넘긴다.
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        // fitInsetsTypes는 API 30부터 있다(Pleos는 34). 그보다 낮으면 기본 배치를 쓴다.
        if (android.os.Build.VERSION.SDK_INT >= 30) fitInsetsTypes = android.view.WindowInsets.Type.statusBars()
        y = dp(16f).toInt()
        // 주행 팝업과 같은 자리: 지도 영역(오른쪽 2/3) 가운데.
        x = displayWidth() / 6
    }

    private fun displayWidth(): Int = runCatching {
        appContext.getSystemService(android.hardware.display.DisplayManager::class.java)
            .getDisplay(android.view.Display.DEFAULT_DISPLAY).mode.physicalWidth
    }.getOrDefault(appContext.resources.displayMetrics.widthPixels)

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, appContext.resources.displayMetrics)

    private companion object {
        const val TAG = "DrivePark/Banner"
    }
}
