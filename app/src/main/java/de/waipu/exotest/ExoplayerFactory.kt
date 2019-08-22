package de.waipu.exotest

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer
import com.google.android.exoplayer2.video.VideoRendererEventListener
import java.util.*

object ExoplayerFactory {
    fun createPlayer(context: Context, workaround: Boolean = false): SimpleExoPlayer {
        return if (workaround) {
            val trackSelector = DefaultTrackSelector()
            val renderersFactory = ExtendedDefaultRenderersFactory(context)
            ExoPlayerFactory.newSimpleInstance(context, renderersFactory, trackSelector)
        } else {
            ExoPlayerFactory.newSimpleInstance(context)
        }
    }
}

class ExtendedDefaultRenderersFactory(context: Context) :
    DefaultRenderersFactory(context) {

    /**
     * extended copy of DefaultRenderersFactory#buildVideoRenderers to deliver ExtendedMediaCodecVideoRenderer
     *
     * Builds video renderers for use by the player.
     *
     * @param context The [Context] associated with the player.
     * @param extensionRendererMode The extension renderer mode.
     * @param mediaCodecSelector A decoder selector.
     * @param drmSessionManager An optional [DrmSessionManager]. May be null if the player will
     * not be used for DRM protected playbacks.
     * @param playClearSamplesWithoutKeys Whether renderers are permitted to play clear regions of
     * encrypted media prior to having obtained the keys necessary to decrypt encrypted regions of
     * the media.
     * @param enableDecoderFallback Whether to enable fallback to lower-priority decoders if decoder
     * initialization fails. This may result in using a decoder that is slower/less efficient than
     * the primary decoder.
     * @param eventHandler A handler associated with the main thread's looper.
     * @param eventListener An event listener.
     * @param allowedVideoJoiningTimeMs The maximum duration for which video renderers can attempt to
     * seamlessly join an ongoing playback, in milliseconds.
     * @param out An array to which the built renderers should be appended.
     */
    override fun buildVideoRenderers(
        context: Context,
        @ExtensionRendererMode extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?,
        playClearSamplesWithoutKeys: Boolean,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        eventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>
    ) {
        out.add(
            ExtendedMediaCodecVideoRenderer(
                context,
                mediaCodecSelector,
                allowedVideoJoiningTimeMs,
                drmSessionManager,
                playClearSamplesWithoutKeys,
                enableDecoderFallback,
                eventHandler,
                eventListener,
                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY
            )
        )

        if (extensionRendererMode == EXTENSION_RENDERER_MODE_OFF) {
            return
        }
        var extensionRendererIndex = out.size
        if (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER) {
            extensionRendererIndex--
        }

        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            // LINT.IfChange
            val clazz = Class.forName("com.google.android.exoplayer2.ext.vp9.LibvpxVideoRenderer")
            val constructor = clazz.getConstructor(
                Long::class.javaPrimitiveType,
                android.os.Handler::class.java,
                com.google.android.exoplayer2.video.VideoRendererEventListener::class.java,
                Int::class.javaPrimitiveType
            )
            // LINT.ThenChange(../../../../../../../proguard-rules.txt)
            val renderer = constructor.newInstance(
                allowedVideoJoiningTimeMs,
                eventHandler,
                eventListener,
                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY
            ) as Renderer
            out.add(extensionRendererIndex++, renderer)
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the extension.
        } catch (e: Exception) {
            // The extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating VP9 extension", e)
        }
    }
}

class ExtendedMediaCodecVideoRenderer(
    context: Context,
    mediaCodecSelector: MediaCodecSelector,
    allowedVideoJoiningTimeMs: Long,
    drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?,
    playClearSamplesWithoutKeys: Boolean,
    enableDecoderFallback: Boolean,
    eventHandler: Handler,
    eventListener: VideoRendererEventListener,
    maxDroppedVideoFrameCountToNotify: Int
) : MediaCodecVideoRenderer(
    context,
    mediaCodecSelector,
    allowedVideoJoiningTimeMs,
    drmSessionManager,
    playClearSamplesWithoutKeys,
    enableDecoderFallback,
    eventHandler,
    eventListener,
    maxDroppedVideoFrameCountToNotify
) {
    override fun codecNeedsSetOutputSurfaceWorkaround(name: String?): Boolean {
        return true
    }
}


