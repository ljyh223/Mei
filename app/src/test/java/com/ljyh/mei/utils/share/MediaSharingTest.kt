package com.ljyh.mei.utils.share

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE, application = Application::class)
class MediaSharingTest {
    private val content = requireNotNull(MediaShareLink.playlist(42, "夜晚", "创建者"))

    @Suppress("DEPRECATION")
    @Test
    fun sharingOpensChooserWithTextInsteadOfSendingToAPreselectedApp() {
        val app = RuntimeEnvironment.getApplication()
        assertTrue(app.shareMediaLink(content))
        val chooser = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertTrue(chooser.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        val send = requireNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/plain", send.type)
        assertEquals(content.text, send.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals(content.title, send.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals(content.title, send.getStringExtra(Intent.EXTRA_TITLE))
        assertNull(send.`package`)
        assertNull(send.component)
        assertNull(send.data)
        assertFalse(send.hasExtra(Intent.EXTRA_STREAM))
    }

    @Test
    fun activityAndWrappedActivityDoNotStartAnotherTask() {
        val activity = Activity()
        for (context in listOf(activity, ContextWrapper(activity))) {
            assertEquals(0, context.mediaShareChooser(content).flags and Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    @Test
    fun unavailableLinkDoesNotLaunchAChooser() {
        val app = RuntimeEnvironment.getApplication()
        assertFalse(app.shareMediaLink(null))
        assertNull(shadowOf(app).nextStartedActivity)
        assertEquals("此内容暂无可分享的公开链接", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun missingSystemShareActivityReportsFailureWithoutCrashing() {
        val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun startActivity(intent: Intent) {
                throw ActivityNotFoundException("No chooser")
            }
        }
        assertFalse(context.shareMediaLink(content))
        assertEquals("无法打开系统分享，请稍后重试", ShadowToast.getTextOfLatestToast())
    }
}
