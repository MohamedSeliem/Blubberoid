package com.hms.mohamedseliem.blubberoid.WebRTC.api;

import android.util.Log;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <h1>PeerConnection manager for {@link com.hms.mohamedseliem.blubberoid.WebRTC.api.RTCClient}</h1>
 * <pre>
 * Author:  Kevin Gleason - Boston College '16
 * File:    PnPeerConnectionClient.java
 * Date:    7/20/15
 * Use:     WebRTC PeerConnection Manager
 * &copy; 2009 - 2015 PubNub, Inc.
 * </pre>
 *
 * {@link PeerConnectionClient} is used to manage peer connections.
 */
public class PeerConnectionClient {
    private SessionDescription localSdp  = null; // either offer or answer SDP
    private MediaStream localMediaStream = null;
    PeerConnectionFactory pcFactory;
    RTCListener mRtcListener;
    SignalingParams signalingParams;
    int MAX_CONNECTIONS = Integer.MAX_VALUE;

    private Pubnub mPubNub;
    private PnRTCReceiver mSubscribeReceiver;
    private Map<String,PnAction> actionMap;
    private Map<String,Peer> peers;
    private String id;
    private String sessionID;

    public PeerConnectionClient(Pubnub pubnub, SignalingParams signalingParams, RTCListener rtcListener){
        this.mPubNub = pubnub;
        this.signalingParams = signalingParams;
        this.mRtcListener = rtcListener;
        this.pcFactory = new PeerConnectionFactory(); // TODO: Check it allowed, else extra param
        this.peers = new HashMap<String, Peer>();
        sessionID = this.mPubNub.uuid();
        init();
    }

    private void init(){
        this.actionMap = new HashMap<String, PnAction>();
        this.actionMap.put(CreateOfferAction.TRIGGER,     new CreateOfferAction());
        this.actionMap.put(CreateAnswerAction.TRIGGER,    new CreateAnswerAction());
        this.actionMap.put(SetRemoteSDPAction.TRIGGER,    new SetRemoteSDPAction());
        this.actionMap.put(AddIceCandidateAction.TRIGGER, new AddIceCandidateAction());
        this.actionMap.put(PnUserHangupAction.TRIGGER,    new PnUserHangupAction());
        this.actionMap.put(PnUserMessageAction.TRIGGER,   new PnUserMessageAction());
        mSubscribeReceiver = new PnRTCReceiver();
    }

    boolean listenOn(String myId){  // Todo: return success?
        if (localMediaStream==null){       // Not true for streaming?
            mRtcListener.onDebug(new RTCMessage("Need to add media stream before you can connect."));
            return false;
        }
        if (this.id != null){  // Prevent listening on multiple channels.
            mRtcListener.onDebug(new RTCMessage("Already listening on " + this.id + ". Cannot have multiple connections."));
            return false;
        }
        this.id = myId;
        subscribe(myId);
        return true;
    }

    /**TODO: Add a max user threshold.
     * Connect with another user by their ID.
     * @param userId The user to establish a WebRTC connection with
     * @return boolean value of success
     */
    boolean connect(String userId, boolean dialed) {
        if (!peers.containsKey(userId)) { // Prevents duplicate dials.
            if (peers.size() < MAX_CONNECTIONS) {
                Peer peer = addPeer(userId);
                peer.pc.addStream(this.localMediaStream);
                try {
                    if(!dialed) {
                        actionMap.get(CreateOfferAction.TRIGGER).execute(userId, new JSONObject());
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        }
        this.mRtcListener.onDebug(new RTCMessage("CONNECT FAILED. Duplicate dial or max peer " +
                "connections exceeded. Max: " + MAX_CONNECTIONS + " Current: " + this.peers.size()));
        return false;
    }

    public void setRTCListener(RTCListener listener){
        this.mRtcListener = listener;
    }

    private void subscribe(String channel){
        try {
            mPubNub.subscribe(channel, this.mSubscribeReceiver);
        } catch (PubnubException e){
            e.printStackTrace();
        }
    }

    public void setLocalMediaStream(MediaStream localStream){
        this.localMediaStream = localStream;
        mRtcListener.onLocalStream(localStream);
    }

    public MediaStream getLocalMediaStream(){
        return this.localMediaStream;
    }

    private Peer addPeer(String id) {
        Peer peer = new Peer(id, this);
        peers.put(id, peer);
        return peer;
    }

    Peer removePeer(String id) {
        Peer peer = peers.get(id);
        peer.pc.close();
        return peers.remove(peer.id);
    }

    List<Peer> getPeers(){
        return new ArrayList<Peer>(this.peers.values());
    }

    /**
     * Close connection (hangup) no a certain peer.
     * @param id PnPeer id to close connection with
     */
    public void closeConnection(String id){
        JSONObject packet = new JSONObject();
        try {
            if (!this.peers.containsKey(id)) return;
            Peer peer = this.peers.get(id);
            peer.hangup();
            packet.put(RTCMessage.JSON_HANGUP, true);
            transmitMessage(id, packet);
            mRtcListener.onPeerConnectionClosed(peer);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * Close connections (hangup) on all open connections.
     */
    public void closeAllConnections() {
        Iterator<String> peerIds = this.peers.keySet().iterator();
        while (peerIds.hasNext()) {
            closeConnection(peerIds.next());
        }
    }

    /**
     * Send SDP Offers/Answers nd ICE candidates to peers.
     * @param toID The id or "number" that you wish to transmit a message to.
     * @param packet The JSON data to be transmitted
     */
    void transmitMessage(String toID, JSONObject packet){
        if (this.id==null){ // Not logged in. Put an error in the debug cb.
            mRtcListener.onDebug(new RTCMessage("Cannot transmit before calling Client.connect"));
        }
        try {
            JSONObject message = new JSONObject();
            message.put(RTCMessage.JSON_PACKET, packet);
            message.put(RTCMessage.JSON_ID, sessionID); //Todo: session id, unused in js SDK?
            message.put(RTCMessage.JSON_NUMBER, this.id);
            this.mPubNub.publish(toID, message, new Callback() {  // Todo: reconsider callback.
                @Override
                public void successCallback(String channel, Object message, String timetoken) {
                    mRtcListener.onDebug(new RTCMessage((JSONObject)message));
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    mRtcListener.onDebug(new RTCMessage(error.errorObject));
                }
            });
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private interface PnAction{
        void execute(String peerId, JSONObject payload) throws JSONException;
    }

    private class CreateOfferAction implements PnAction{
        public static final String TRIGGER = "init";
        public void execute(String peerId, JSONObject payload) throws JSONException {
            Log.d("COAction","CreateOfferAction");
            Peer peer = peers.get(peerId);
            peer.setDialed(true);
            peer.setType(Peer.TYPE_ANSWER);
            peer.pc.createOffer(peer, signalingParams.pcConstraints);
        }
    }

    private class CreateAnswerAction implements PnAction{
        public static final String TRIGGER = "offer";
        public void execute(String peerId, JSONObject payload) throws JSONException {
            Log.d("CAAction","CreateAnswerAction");
            Peer peer = peers.get(peerId);
            peer.setType(Peer.TYPE_OFFER);
            peer.setStatus(Peer.STATUS_CONNECTED);
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.pc.setRemoteDescription(peer, sdp);
            peer.pc.createAnswer(peer, signalingParams.pcConstraints);
        }
    }

    private class SetRemoteSDPAction implements PnAction{
        public static final String TRIGGER = "answer";
        public void execute(String peerId, JSONObject payload) throws JSONException {
            Log.d("SRSAction","SetRemoteSDPAction");
            Peer peer = peers.get(peerId);
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.pc.setRemoteDescription(peer, sdp);
        }
    }

    private class AddIceCandidateAction implements PnAction{
        public static final String TRIGGER = "candidate";
        public void execute(String peerId, JSONObject payload) throws JSONException {
            Log.d("AICAction","AddIceCandidateAction");
            PeerConnection pc = peers.get(peerId).pc;
            if (pc.getRemoteDescription() != null) {
                IceCandidate candidate = new IceCandidate(
                        payload.getString("sdpMid"),
                        payload.getInt("sdpMLineIndex"),
                        payload.getString("candidate")
                );
                pc.addIceCandidate(candidate);
            }
        }
    }

    private class PnUserHangupAction implements PnAction{
        public static final String TRIGGER = RTCMessage.JSON_HANGUP;
        public void execute(String peerId, JSONObject payload) throws JSONException {
            Log.d("PnUserHangup","PnUserHangupAction");
            Peer peer = peers.get(peerId);
            peer.hangup();
            mRtcListener.onPeerConnectionClosed(peer);
            // Todo: Consider Callback?
        }
    }

    private class PnUserMessageAction implements PnAction{
        public static final String TRIGGER = RTCMessage.JSON_USERMSG;
        public void execute(String peerId, JSONObject payload) throws JSONException {
            Log.d("PnUserMessage","AddIceCandidateAction");
            JSONObject msgJson = payload.getJSONObject(RTCMessage.JSON_USERMSG);
            Peer peer = peers.get(peerId);
            mRtcListener.onMessage(peer, msgJson);
        }
    }


    /**
     * @param userId Your id. Used to tag the message before publishing it to another user.
     * @return
     */
    public static JSONObject generateHangupPacket(String userId){
        JSONObject json = new JSONObject();
        try {
            JSONObject packet = new JSONObject();
            packet.put(RTCMessage.JSON_HANGUP, true);
            json.put(RTCMessage.JSON_PACKET, packet);
            json.put(RTCMessage.JSON_ID, ""); //Todo: session id, unused in js SDK?
            json.put(RTCMessage.JSON_NUMBER, userId);
        } catch (JSONException e){
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Static method to generate the proper JSON for a user message. Use this when you don't have
     *   a {@link com.hms.mohamedseliem.blubberoid.WebRTC.api.RTCClient} instantiated. Simply send a publish with the
     *   returned JSONObject to the ID that a user is subscribed to.
     * @param userId Your UserID, needed to tag the message
     * @param message The message you with to send some other user
     * @return JSONObject properly formatted for the PubNub WebRTC API
     */
    public static JSONObject generateUserMessage(String userId, JSONObject message){
        JSONObject json = new JSONObject();
        try {
            JSONObject packet = new JSONObject();
            packet.put(RTCMessage.JSON_USERMSG, message);
            json.put(RTCMessage.JSON_PACKET, packet);
            json.put(RTCMessage.JSON_ID, ""); //Todo: session id, unused in js SDK?
            json.put(RTCMessage.JSON_NUMBER, userId);
        } catch (JSONException e){
            e.printStackTrace();
        }
        return json;
    }

    private class PnRTCReceiver extends Callback {

        @Override
        public void connectCallback(String channel, Object message) {
            mRtcListener.onDebug(new RTCMessage(((JSONArray) message).toString()));
            mRtcListener.onConnected(channel);
        }

        @Override
        public void successCallback(String channel, Object message) {
            if (!(message instanceof JSONObject)) return; // Ignore if not valid JSON.
            JSONObject jsonMessage = (JSONObject) message;
            mRtcListener.onDebug(new RTCMessage(jsonMessage));
            try {
                String peerId     = jsonMessage.getString(RTCMessage.JSON_NUMBER);
                JSONObject packet = jsonMessage.getJSONObject(RTCMessage.JSON_PACKET);
                Peer peer;
                if (!peers.containsKey(peerId)){
                    // Possibly threshold number of allowed users
                    peer = addPeer(peerId);
                    peer.pc.addStream(localMediaStream);
                } else {
                    peer = peers.get(peerId);
                }
                if (peer.getStatus().equals(Peer.STATUS_DISCONNECTED)) return; // Do nothing if disconnected.
                if (packet.has(RTCMessage.JSON_USERMSG)) {
                    actionMap.get(PnUserMessageAction.TRIGGER).execute(peerId,packet);
                    return;
                }
                if (packet.has(RTCMessage.JSON_HANGUP)){
                    actionMap.get(PnUserHangupAction.TRIGGER).execute(peerId,packet);
                    return;
                }
                if (packet.has(RTCMessage.JSON_THUMBNAIL)) {
                    return;   // No handler for thumbnail or hangup yet, will be separate controller callback
                }
                if (packet.has(RTCMessage.JSON_SDP)) {
                    if(!peer.received) {
                        peer.setReceived(true);
                        mRtcListener.onDebug(new RTCMessage("SDP - " + peer.toString()));
                        // Todo: reveivercb(peer);
                    }
                    String type = packet.getString(RTCMessage.JSON_TYPE);
                    actionMap.get(type).execute(peerId, packet);
                    return;
                }
                if (packet.has(RTCMessage.JSON_ICE)){
                    actionMap.get(AddIceCandidateAction.TRIGGER).execute(peerId,packet);
                    return;
                }
            } catch (JSONException e){
                e.printStackTrace();
            }
        }

        @Override
        public void errorCallback(String channel, PubnubError error) {
            super.errorCallback(channel, error);
        }

    }

}