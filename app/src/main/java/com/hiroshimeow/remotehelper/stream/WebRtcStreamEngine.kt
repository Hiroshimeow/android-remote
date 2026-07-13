package com.hiroshimeow.remotehelper.stream

import android.content.Context
import android.content.Intent
import android.util.Log
import android.media.projection.MediaProjection
import com.hiroshimeow.remotehelper.protocol.StreamConfig
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer

class WebRtcStreamEngine(
    private val context: Context,
    private val sendSdp: (SessionDescription) -> Unit,
    private val sendIce: (IceCandidate) -> Unit
) : StreamEngine {

    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var screenCapturer: VideoCapturer? = null

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        eglBase = EglBase.create()
    }

    override fun start(config: StreamConfig, mediaProjectionPermissionResultData: Intent) {
        Log.d("WebRtcStreamEngine", "start stream with config: $config")
        
        val options = PeerConnectionFactory.Options()
        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(
            eglBase?.eglBaseContext, true, true
        )
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(eglBase?.eglBaseContext)
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()

        val iceServers = listOf(
            IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        
        peerConnection = peerConnectionFactory?.createPeerConnection(
            iceServers,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    Log.d("WebRtcStreamEngine", "ICE connection state: $state")
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidate(candidate: IceCandidate) {
                    sendIce(candidate)
                }
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(dataChannel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
            }
        )
        
        screenCapturer = ScreenCapturerAndroid(mediaProjectionPermissionResultData, object : MediaProjection.Callback() {
            override fun onStop() {
                Log.e("WebRtcStreamEngine", "User revoked screen capture permission")
            }
        })
        
        surfaceTextureHelper = SurfaceTextureHelper.create("ScreenCaptureThread", eglBase?.eglBaseContext)
        videoSource = peerConnectionFactory?.createVideoSource(screenCapturer!!.isScreencast)
        screenCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
        // 720p 30fps
        screenCapturer?.startCapture(1280, 720, 30)

        videoTrack = peerConnectionFactory?.createVideoTrack("video_track_id", videoSource)
        peerConnection?.addTrack(videoTrack)
        
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription?) {}
                    override fun onSetSuccess() {
                        sendSdp(sdp)
                    }
                    override fun onCreateFailure(error: String?) {}
                    override fun onSetFailure(error: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())
    }
    
    fun handleRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, sdp)
    }
    
    fun handleRemoteIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    override fun stop() {
        Log.d("WebRtcStreamEngine", "stop stream")
        try {
            screenCapturer?.stopCapture()
        } catch (e: Exception) {
            Log.e("WebRtcStreamEngine", "Error stopping capture", e)
        }
        videoTrack?.dispose()
        videoSource?.dispose()
        surfaceTextureHelper?.dispose()
        screenCapturer?.dispose()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
        // Do not dispose eglBase as it might be shared, or dispose it if it's strictly local.
        eglBase?.release()
    }

    override fun setQuality(profile: StreamConfig) {
        Log.d("WebRtcStreamEngine", "set quality: $profile")
    }
}
