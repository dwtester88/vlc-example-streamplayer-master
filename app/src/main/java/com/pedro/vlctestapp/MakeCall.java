package com.pedro.vlctestapp;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Created by Vijen on 11/09/2017.
 */

public class MakeCall {

    private static final String LOG_TAG = "MakeCall";
    private static final int BROADCAST_PORT = 50002;
    private static final int BUF_SIZE = 1024;
    private String displayName;
    private String contactName;
    private String contactIp;
    private boolean LISTEN = true;
    private boolean IN_CALL = false;
    private AudioCall call;

    public void makeCall(String contact) {

        contactIp = contact;
        LISTEN = true;
        startListener();

        // Send a request to start a call
        sendMessage("CAL:"+displayName, 50003);
    }

    private void startListener() {
        // Create listener thread
        LISTEN = true;
        Thread listenThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    Log.i(LOG_TAG, "Listener started!");
                    DatagramSocket socket = new DatagramSocket(BROADCAST_PORT);
                    socket.setSoTimeout(15000);
                    byte[] buffer = new byte[BUF_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);
                    while(LISTEN) {

                        try {
                            Log.i(LOG_TAG, "Listening for packets");
                            socket.receive(packet);
                            String data = new String(buffer, 0, packet.getLength());
                            Log.i(LOG_TAG, "Packet received from "+ packet.getAddress() +" with contents: " + data);
                            String action = data.substring(0, 4);
                            if(action.equals("ACC:")) {
                                // Accept notification received. Start call
                                call = new AudioCall(packet.getAddress());
                                call.startCall();
                                IN_CALL = true;
                            }
                            else if(action.equals("REJ:")) {
                                // Reject notification received. End call
                                endCall();
                            }
                            else if(action.equals("END:")) {
                                // End call notification received. End call
                                endCall();
                            }
                            else {
                                // Invalid notification received
                                Log.w(LOG_TAG, packet.getAddress() + " sent invalid message: " + data);
                            }
                        }
                        catch(SocketTimeoutException e) {
                            if(!IN_CALL) {
                                Log.i(LOG_TAG, "No reply from contact. Ending call");
                                endCall();
                                return;
                            }
                        }
                        catch(IOException e) {
                        }
                    }
                    Log.i(LOG_TAG, "Listener ending");
                    socket.disconnect();
                    socket.close();
                    return;
                }
                catch(SocketException e) {
                    Log.e(LOG_TAG, "SocketException in Listener" +e.getMessage());
                    endCall();
                }
            }
        });
        listenThread.start();
    }

    public void endCall() {
        // Ends the chat sessions
        stopListener();
        call.endCall();
        sendMessage("END:", BROADCAST_PORT);
        //finish();
    }

    private void stopListener() {
        // Ends the listener thread
        LISTEN = false;
    }

    private void sendMessage(final String message, final int port) {
        // Creates a thread used for sending notifications
        Thread replyThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    InetAddress address = InetAddress.getByName(contactIp);
                    byte[] data = message.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                    socket.send(packet);
                    Log.i(LOG_TAG, "Sent message( " + message + " ) to " + contactIp);
                    socket.disconnect();
                    socket.close();
                }
                catch(UnknownHostException e) {

                    Log.e(LOG_TAG, "Failure. UnknownHostException in sendMessage: " + contactIp);
                }
                catch(SocketException e) {

                    Log.e(LOG_TAG, "Failure. SocketException in sendMessage: " + e);
                }
                catch(IOException e) {

                    Log.e(LOG_TAG, "Failure. IOException in sendMessage: " + e);
                }
            }
        });
        replyThread.start();
    }
}
