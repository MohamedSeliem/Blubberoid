package com.hms.mohamedseliem.blubberoid.util;

import android.util.Log;

import com.hms.mohamedseliem.blubberoid.WebRTC.api.Peer;
import com.hms.mohamedseliem.blubberoid.WebRTC.api.RTCListener;
import com.hms.mohamedseliem.blubberoid.WebRTC.api.RTCMessage;
import org.webrtc.MediaStream;


/**
 * <p>Created by GleasonK on 7/23/15.</p>
 */
public class LogRTCListener extends RTCListener {
    @Override
    public void onCallReady(String callId) {
        Log.i("RTCListener", "OnCallReady - " + callId);
    }

    @Override
    public void onConnected(String userId) {
        Log.i("RTCListener", "OnConnected - " + userId);
    }

    @Override
    public void onPeerStatusChanged(Peer peer) {
        Log.i("RTCListener", "OnPeerStatusChanged - " + peer.toString());
    }

    @Override
    public void onPeerConnectionClosed(Peer peer) {
        Log.i("RTCListener", "OnPeerConnectionClosed - " + peer.toString());
    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        Log.i("RTCListener", "OnLocalStream - " + localStream.toString());
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, Peer peer) {
        Log.i("RTCListener", "OnAddRemoteStream - " + peer.toString());
    }

    @Override
    public void onRemoveRemoteStream(MediaStream remoteStream, Peer peer) {
        Log.i("RTCListener", "OnRemoveRemoteStream - " + peer.toString());
    }

    @Override
    public void onMessage(Peer peer, Object message) {
        Log.i("RTCListener", "OnMessage - " + message.toString());
    }

    @Override
    public void onDebug(RTCMessage message) {
        Log.i("RTCListener", "OnDebug - " + message.getMessage());
    }
}
