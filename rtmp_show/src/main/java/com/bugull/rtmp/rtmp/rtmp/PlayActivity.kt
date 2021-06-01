package com.bugull.rtmp.rtmp.rtmp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.dou361.ijkplayer.widget.AndroidMediaController
import com.dou361.ijkplayer.widget.IjkVideoView
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer

class PlayActivity : AppCompatActivity() {
    private var ijkMediaPlayer: IjkMediaPlayer? = null
    private val useA = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)
        val url = intent?.extras?.getString(URL, "") ?: BuildConfig.rtmp
        findViewById<TextView>(R.id.url).text = "url : \n $url "

        initIJK()

        if (useA) {
            findViewById<IJKView>(R.id.ijkplayerView).visibility = View.GONE
            findViewById<IjkVideoView>(R.id.ijkplayerView_aaa).visibility = View.VISIBLE
            initA(url)
        } else {
            findViewById<IJKView>(R.id.ijkplayerView).visibility = View.VISIBLE
            findViewById<IjkVideoView>(R.id.ijkplayerView_aaa).visibility = View.GONE
            initB(url)
        }

    }

    private fun initIJK() {
        IjkMediaPlayer.loadLibrariesOnce(null)
        IjkMediaPlayer.native_profileBegin("libijkplayer.so")
        ijkMediaPlayer = IjkMediaPlayer().apply {
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec_mpeg4", 1)
            setOption(1, "analyzemaxduration", 100L)
            setOption(1, "probesize", 10240L)
            setOption(1, "flush_packets", 1L)
            setOption(4, "packet-buffering", 0L)
            setOption(4, "packet-framedrop", 1L)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1)
        }
    }


    private fun initB(url: String) {
        findViewById<IJKView>(R.id.ijkplayerView).setPath(url)
        findViewById<IJKView>(R.id.ijkplayerView).setListener(object :
            IJKView.VideoPlayerListener() {
            override fun onPrepared(p0: IMediaPlayer?) {
                p0?.start()
            }

            override fun onCompletion(p0: IMediaPlayer?) {
            }
        })
        ijkMediaPlayer?.start()


    }

    private fun initA(url: String) {
        findViewById<IjkVideoView>(R.id.ijkplayerView_aaa).apply {
            setVideoPath(url)
            setMediaController(AndroidMediaController(this@PlayActivity, false))
        }

        ijkMediaPlayer?.start()
    }

    private fun stopAaa() {
        if (findViewById<IjkVideoView>(R.id.ijkplayerView_aaa).isPlaying) {
            findViewById<IjkVideoView>(R.id.ijkplayerView_aaa).stopPlayback()
            findViewById<IjkVideoView>(R.id.ijkplayerView_aaa).release(true)
        }
        IjkMediaPlayer.native_profileEnd()
    }


    override fun onStop() {
        super.onStop()
        if (useA) {
            stopAaa()
        } else {
            stopBbb()
        }
    }

    private fun stopBbb() {
        if (findViewById<IJKView>(R.id.ijkplayerView).isPlaying) {
            findViewById<IJKView>(R.id.ijkplayerView).stop()
            findViewById<IJKView>(R.id.ijkplayerView).release()

        }
        IjkMediaPlayer.native_profileEnd()
    }

    companion object {
        private const val URL = "url"

        @JvmStatic
        fun skip(activity: FragmentActivity, url: String) {
            activity.startActivity(Intent(activity, PlayActivity::class.java).apply {
                putExtras(bundleOf(URL to url))
            })
        }
    }
}