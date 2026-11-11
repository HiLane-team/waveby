package com.drivepark.app.drive

import com.drivepark.app.domain.SplitPane

/**
 * Pleos 3분할에서 Waveby가 어느 칸에 있는지 판단하고, 켜질 때 마지막으로 있던 칸으로 되돌린다.
 *
 * Pleos 규칙(에뮬레이터 v2.1에서 확인): 분할 중에 앱을 실행하면 그 앱이 가운데로 가고 다른 앱은 오른쪽으로 밀린다.
 * 그래서 자리를 바꿀 때는 내비를 한 번 실행한다(Waveby가 오른쪽으로 감).
 * 사용자가 옮긴 자리는 그대로 두고, 저장만 해 둔다(계속 감시하지 않는다).
 */
object SplitPlacement {
    /** 기본 화면(오른쪽 2/3, 폭 1682)과 3분할 한 칸(폭 841)을 가르는 폭(px). */
    private const val SPLIT_MAX_WIDTH = 1200

    /** 맨 오른쪽 칸은 화면 오른쪽 끝에 붙어 있다. */
    private const val RIGHT_EDGE_SLACK = 80

    fun paneOf(left: Int, right: Int, displayWidth: Int): SplitPane = when {
        right - left >= SPLIT_MAX_WIDTH -> SplitPane.NONE
        right >= displayWidth - RIGHT_EDGE_SLACK -> SplitPane.RIGHT
        else -> SplitPane.MIDDLE
    }

    /** 이 활동의 창이 지금 어느 칸에 있는지. */
    fun paneOf(activity: android.app.Activity): SplitPane {
        // 창 크기 API는 30부터 있다(Pleos는 34). 그보다 낮으면 분할이 아닌 것으로 본다.
        if (android.os.Build.VERSION.SDK_INT < 30) return SplitPane.NONE
        val wm = activity.windowManager
        val b = wm.currentWindowMetrics.bounds
        return paneOf(b.left, b.right, wm.maximumWindowMetrics.bounds.width())
    }

    /** 지금 분할 칸에 있고, 기억한 칸과 다르면 한 번 바꾼다. */
    fun needsSwap(current: SplitPane, remembered: SplitPane): Boolean =
        current != SplitPane.NONE && remembered != SplitPane.NONE && current != remembered
}
