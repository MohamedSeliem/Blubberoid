package com.hms.mohamedseliem.blubberoid.WebRTC.api;

/**
 * Created by Mohamed Seliem on 11/10/2017.
 */
import org.webrtc.MediaStream;



/**Todo: Think about parameters.

 * <p>

 *     Created by GleasonK on 7/20/15 for PubNub WebRTC Signaling.

 *     PubNub '15

 *     Boston College '16

 * </p>

 * Implement this interface to be notified of WebRTC events.

 * It is an abstract class with default behaviors of doing nothing.

 * Use a PnRTCListener to implement the various callbacks of your WebRTC application.

 */

public abstract class RTCListener{
    public void onCallReady(String callId){} // TODO: Maybe not needed?

    /**
     * Called in {@link com.pubnub.api.Pubnub} object's subscribe connected callback.
     * Means that you are ready to receive calls.
     * @param userId The channel you are subscribed to, the userId you may be called on.
     */
    public void onConnected(String userId){}

    /**
     * Peer status changed. {@link Peer} status changed, can be
     * CONNECTING, CONNECTED, or DISCONNECTED.
     * @param peer The peer object, can use to check peer.getStatus()
     */
    public void onPeerStatusChanged(Peer peer){}

    /**TODO: Is this different than onPeerStatusChanged == DISCONNECTED?
     * Called when a hangup occurs.
     * @param peer The peer who was hung up on, or who hung up on you
     */
    public void onPeerConnectionClosed(Peer peer){}

    /**
     * Called in {@link PeerConnectionClient} when setLocalStream
     * is invoked.
     * @param localStream The users local stream from Android's front or back camera.
     */
    public void onLocalStream(MediaStream localStream){}

    /**
     * Called when a remote stream is added in the {@link org.webrtc.PeerConnection.Observer}
     * in {@link Peer}.
     * @param remoteStream The remote stream that was added
     * @param peer The peer that added the remote stream
     *             Todo: Maybe not the right peer?
     */
    public void onAddRemoteStream(MediaStream remoteStream, Peer peer){}

    /**
     * Called in the {@link org.webrtc.PeerConnection.Observer} implemented
     * by {@link Peer}.
     * @param remoteStream The stream that was removed by your peer
     * @param peer The peer that removed the stream.
     */
    public void onRemoveRemoteStream(MediaStream remoteStream, Peer peer){}

    /**
     * Called when a user message is send via {@link com.pubnub.api.Pubnub} object.
     * @param peer The peer who sent the message
     * @param message The {@link org.json.JSONObject} message sent by the user.
     */
    public void onMessage(Peer peer, Object message){}

    /**
     * A helpful debugging callback for testing and developing your app.
     * @param message The {@link RTCMessage} debug message.
     */
    public void onDebug(RTCMessage message){}

}