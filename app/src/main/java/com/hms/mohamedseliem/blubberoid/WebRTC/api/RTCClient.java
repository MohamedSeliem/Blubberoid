package com.hms.mohamedseliem.blubberoid.WebRTC.api;

/**
 * Created by Mohamed Seliem on 11/10/2017.
 */import com.pubnub.api.Pubnub;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;

import java.util.List;


/**
 * <h1>Main WebRTC Signaling class, holds all functions to set up {@link org.webrtc.PeerConnection}</h1>
 * <pre>
 * Author:  Kevin Gleason - Boston College '16
 * File:    PnRTCClient.java
 * Date:    7/20/15
 * Use:     PubNub WebRTC Signaling
 * &copy; 2009 - 2015 PubNub, Inc.
 * </pre>
 */
public class RTCClient {
    private SignalingParams pnSignalingParams;
    private Pubnub mPubNub;
    private PeerConnectionClient pcClient;
    private String UUID;

    /**
     * Minimal constructor. Requires a valid Pub and Sub key. Get your Pub/Sub keys for free at
     *  https://admin.pubnub.com/#/register and find keys on developer portal.
     * No UUID provided so a random phone number will be generated with this constructor (XXX-XXXX).
     * @param pubKey PubNub Pub Key
     * @param subKey PubNub Sub Key
     */
    public RTCClient(String pubKey, String subKey) {
        this.UUID = generateRandomNumber();
        this.mPubNub  = new Pubnub(pubKey, subKey);
        this.mPubNub.setUUID(this.UUID);
        this.pnSignalingParams = SignalingParams.defaultInstance();
        this.pcClient = new PeerConnectionClient(this.mPubNub, this.pnSignalingParams, new RTCListener() {});
    }

    /**
     * Slightly more verbose constructor. Requires a valid Pub and Sub key. Get your Pub/Sub keys for free at
     *  https://admin.pubnub.com/#/register and find keys on developer portal.
     * @param pubKey PubNub Pub Key
     * @param subKey PubNub Sub Key
     * @param UUID Any UUID to be used as a username
     */
    public RTCClient(String pubKey, String subKey, String UUID) {
        this.UUID = UUID;
        this.mPubNub  = new Pubnub(pubKey, subKey);
        this.mPubNub.setUUID(this.UUID);
        this.pnSignalingParams = SignalingParams.defaultInstance();
        this.pcClient = new PeerConnectionClient(this.mPubNub, this.pnSignalingParams, new RTCListener() {});
    }

    /**
     * Return the {@link com.hms.mohamedseliem.blubberoid.WebRTC.api.RTCClient} peer connection constraints.
     * @return Peer Connection Constrains
     */
    public MediaConstraints pcConstraints() {
        return pnSignalingParams.pcConstraints;
    }

    /**
     * Return the {@link com.hms.mohamedseliem.blubberoid.WebRTC.api.RTCClient} video constraints.
     * @return Video Constrains
     */
    public MediaConstraints videoConstraints() {
        return pnSignalingParams.videoConstraints;
    }

    /**
     * Return the {@link com.hms.mohamedseliem.blubberoid.WebRTC.api.RTCClient} audio constraints.
     * @return Audio Constrains
     */
    public MediaConstraints audioConstraints() {
        return pnSignalingParams.audioConstraints;
    }

    /**
     * Return the {@link com.hms.mohamedseliem.blubberoid.WebRTC.api.RTCClient} Pubnub instance.
     * @return The PnRTCClient's {@link com.pubnub.api.Pubnub} instance
     */
    public Pubnub getPubNub(){
        return this.mPubNub;
    }

    /**
     * Return the UUID (username) of the {@link com.hms.mohamedseliem.blubberoid.WebRTC.api.RTCClient}. If not
     *   provided by the constructor, a random phone number is generated and can be retrieived
     *   with this method
     * @return The UUID username of the client
     */
    public String getUUID() {
        return UUID;
    }

    /**
     * Set the signaling parameters. This includes {@link org.webrtc.MediaConstraints} for
     *   {@link org.webrtc.PeerConnection}, Video, and Audio, as well as a list of possible
     *   {@link org.webrtc.PeerConnection.IceServer} candidates.
     * @param signalParams Parameters for WebRTC Signaling
     */
    public void setSignalParams(SignalingParams signalParams){
        this.pnSignalingParams = signalParams;
    }

    /**
     * Need to attach mediaStream before you can connect.
     * @param mediaStream Not null local media stream
     */
    public void attachLocalMediaStream(MediaStream mediaStream){
        this.pcClient.setLocalMediaStream(mediaStream);
    }

    /**
     * Attach custom listener for callbacks!
     * @param listener The listener which extends PnRTCListener to implement callbacks
     */
    public void attachRTCListener(RTCListener listener){
        this.pcClient.setRTCListener(listener);
    }

    /**
     * Set the maximum simultaneous connections allowed
     * @param max Max simultaneous connections
     */
    public void setMaxConnections(int max){
        this.pcClient.MAX_CONNECTIONS = max;
    }

    /**
     * Subscribe to a channel using PubNub to listen for calls.
     * @param channel The channel to listen on, your "phone number"
     */
    public void listenOn(String channel){
        this.pcClient.listenOn(channel);
    }

    /**
     * Connect with another user by their ID.
     * @param userId The user to establish a WebRTC connection with
     */
    public void connect(String userId, boolean dialed){
        this.pcClient.connect(userId, dialed);
    }

    /**
     * Close a single peer connection. Send a PubNub hangup signal as well
     * @param userId User to close a connection with
     */
    public void closeConnection(String userId){
        this.pcClient.closeConnection(userId);
    }

    /**
     * Close all peer connections. Send a PubNub hangup signal as well.
     */
    public void closeAllConnections(){
        this.pcClient.closeAllConnections();
    }

    /**
     * Send a custom JSONObject user message to a single peer.
     * @param userId user to send a message to
     * @param message the JSON message to pass to a peer.
     */
    public void transmit(String userId, JSONObject message){
        JSONObject usrMsgJson = new JSONObject();
        try {
            usrMsgJson.put(RTCMessage.JSON_USERMSG, message);
            this.pcClient.transmitMessage(userId, usrMsgJson);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * Send a custom JSONObject user message to all peers.
     * @param message the JSON message to pass to all peers.
     */
    public void transmitAll(JSONObject message){
        List<Peer> peerList = this.pcClient.getPeers();
        for(Peer p : peerList){
            transmit(p.getId(), message);
        }
    }

    private static String generateRandomNumber(){
        String areaCode = String.valueOf((int)(Math.random()*1000));
        String digits   = String.valueOf((int)(Math.random()*10000));
        while (areaCode.length() < 3) areaCode += "0";
        while (digits.length()   < 4) digits   += "0";
        return areaCode + "-" + digits;
    }

    /**
     * Call this method in Activity.onDestroy() to clost all open connections and clean up
     *   instance for garbage collection.
     */
    public void onDestroy() {
        this.pcClient.closeAllConnections();
        this.mPubNub.unsubscribeAll();
    }

}