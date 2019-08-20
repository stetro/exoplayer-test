package de.waipu.exotest

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util.getUserAgent
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var videoSource: ProgressiveMediaSource
    private lateinit var player: SimpleExoPlayer
    private var currentSurfaceViewId: Int = R.id.surface_view_1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val dataSourceFactory = DefaultDataSourceFactory(
            this,
            getUserAgent(this, getString(R.string.app_name))
        )
        videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"))
        player = ExoPlayerFactory.newSimpleInstance(applicationContext)

        switch_button.setOnClickListener {
            Log.d(TAG, "switch surface of player")
            currentSurfaceViewId = if (currentSurfaceViewId == R.id.surface_view_1) {
                R.id.surface_view_2
            } else {
                R.id.surface_view_1
            }
            setCurrentSurfaceViewSurface()
        }
        clear_button.setOnClickListener {
            Log.d(TAG, "clear surface of player")
            player.setVideoSurface(null)
        }
        fill_button.setOnClickListener {
            Log.d(TAG, "fill surface of player")
            setCurrentSurfaceViewSurface()
        }
    }

    override fun onStart() {
        super.onStart()
        setCurrentSurfaceViewSurface()
        player.playWhenReady = true
        player.prepare(videoSource)
    }

    private fun setCurrentSurfaceViewSurface() {
        player.setVideoSurface(findViewById<SurfaceView>(currentSurfaceViewId).holder.surface)
    }

    override fun onStop() {
        super.onStop()
        player.stop()
        player.setVideoSurface(null)
    }

    companion object {
        const val TAG: String = "MainActivity"
    }

}
