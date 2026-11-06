package com.drivepark.app.drive

import android.content.Context
import android.content.Intent
import android.util.Log
import com.drivepark.app.MainActivity

/** 앱이 지금 화면에 보이는지. MainActivity의 onStart/onStop이 바꾼다. */
object AppForeground {
    @Volatile var visible: Boolean = false
}

/**
 * 순정 내비와 우리 앱 화면을 오간다.
 * 앱이 Pleos 3분할을 직접 켤 수는 없다(공개 API 없음, LAUNCH_ADJACENT도 무시됨). 분할은 사용자가 손잡이로 한다.
 */
object ScreenSwitcher {
    const val NAVI_PACKAGE = "ai.umos.maps.android.navigation.app"
    const val EXTRA_SKIP_WELCOME = "skip_welcome"
    private const val TAG = "DrivePark/Screen"

    /** 순정 내비를 앞으로 띄운다. */
    fun openNavi(context: Context) {
        runCatching {
            context.packageManager.getLaunchIntentForPackage(NAVI_PACKAGE)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?.let(context::startActivity)
        }.onFailure { Log.w(TAG, "openNavi failed", it) }
    }

    /** 공원(우리 앱)을 앞으로 띄운다. 다른 앱 위에 표시 권한이 있어 서비스에서도 띄울 수 있다. */
    fun openPark(context: Context) {
        if (AppForeground.visible) return
        runCatching {
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    // 시동이 아니라 주차 대기로 띄운 것: 인사 화면(→내비)을 다시 보이지 않는다.
                    .putExtra(EXTRA_SKIP_WELCOME, true),
            )
        }.onFailure { Log.w(TAG, "openPark failed", it) }
    }
}
