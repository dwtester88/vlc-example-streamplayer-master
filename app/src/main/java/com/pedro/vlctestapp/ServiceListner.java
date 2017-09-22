package com.pedro.vlctestapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Created by Vijen on 14/09/2017.
 */

public class ServiceListner  extends Service {

    static String UDP_BROADCAST = "UDPBroadcast";

    //Boolean shouldListenForUDPBroadcast = false;

    public boolean LISTEN = true;
    public static final int BROADCAST_PORT = 50005;

    Thread UDPBroadcastThread;
    private AudioCall call;

    private static final String LOG_TAG = UDP_BROADCAST;

    private static final int BUF_SIZE = 1024;
    private String displayName;
    private String contactName;
    private String contactIp;

    private boolean IN_CALL = false;




/*
    public void makecall(String contact) {

        contactIp = contact;
        LISTEN = true;
        listen();
        // Send a request to start a call
        sendMessage("CAL:"+displayName, 50001);
    }*/

    void startListenForUDPBroadcast() {
        UDPBroadcastThread = new Thread(new Runnable() {
            public void run() {
                try {

                    listen();


                    /*InetAddress broadcastIP = InetAddress.getByName("192.168.1.255"); //172.16.238.42 //192.168.1.255
                    Integer port = 50001;
                    while (shouldRestartSocketListen) {
                        listenAndWaitAndThrowIntent(broadcastIP, port);
                    }*/
                    //if (!shouldListenForUDPBroadcast) throw new ThreadDeath();
                } catch (Exception e) {
                    Log.i("UDP", "no longer listening for UDP broadcasts cause of error " + e.getMessage());
                }
            }
        });
        UDPBroadcastThread.start();
    }


    @Override
    public void onCreate() {
    };

    @Override
    public void onDestroy() {

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startListenForUDPBroadcast();
        Log.i("UDP", "Service started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    public void listen() {
        // Create the listener thread
        Log.i(UDP_BROADCAST, "Listening started!");
        Thread listenThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while(LISTEN) {

                    DatagramSocket socket1;
                    try {

                        socket1 = new DatagramSocket(BROADCAST_PORT);
                        // socket1 = new DatagramSocket(null);
                        //socket1.setReuseAddress(true);
                        // socket1.bind(new InetSocketAddress(BROADCAST_PORT));
                    } catch (SocketException e) {

                        Log.e(UDP_BROADCAST, "SocketExcepion in listener: " + e);
                        return;
                    }
                    byte[] buffer = new byte[1024];


                    listen(socket1, buffer);

                    Log.i(UDP_BROADCAST, "Listener ending!");
                    socket1.disconnect();
                    socket1.close();
                    startListenForUDPBroadcast();

                    return;
                }
            }

            public void listen(DatagramSocket socket1, byte[] buffer) {
                try {
                    //Listen in for new notifications
                    Log.i(UDP_BROADCAST, "Listening for a packet!");
                    DatagramPacket packet = new DatagramPacket(buffer, 1024);
                    socket1.setSoTimeout(15000);
                    socket1.receive(packet);
                    String data = new String(buffer, 0, packet.getLength());
                    Log.i(UDP_BROADCAST, "Packet received: " + data);
                    String action = data.substring(0, 4);
                    String ServerrIP = data.substring(4);
                    if(action.equals("ADD:")) {
                        Log.i(UDP_BROADCAST, "Packet received: " + data);

                        //Thread.sleep(5000);
                        Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);
                        startIntent.putExtra("ServerIP",data.substring(4));
                        startIntent.putExtra("Service_Flag",true);
                        startIntent.setAction(MainActivity.NOTIFICATION_SERVICE);
                        Log.i(UDP_BROADCAST, "Packet received: " + ServerrIP);
                        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|
                                Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startIntent.getBooleanExtra("service",true);
                        startIntent.putExtra("services",true);
                        getApplicationContext().startActivity(startIntent);
                        Thread.sleep(60000);
                        //stopListener();
                        // Add notification received. Attempt to add contact
                        Log.i(UDP_BROADCAST, "Listener received ADD request");
                        //  addContact(data.substring(4, data.length()), packet.getAddress());
                    }
                    else if(action.equals("BYE:")) {
                        // Bye notification received. Attempt to remove contact
                        Log.i(UDP_BROADCAST, "Listener received BYE request");
                        // removeContact(data.substring(4, data.length()));
                    }
                    else if(action.equals("ACC:")) {
                        // Accept notification received. Start call
                        LISTEN=false;
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

                        // Invalid notification received
                        Log.w(UDP_BROADCAST, "Listener received invalid request: " + action);
                    }
                }
                catch(SocketTimeoutException e) {
                    Log.i(UDP_BROADCAST, "No packet received!");
                    if(LISTEN) {
                        listen(socket1, buffer);
                    }
                    return;
                }
                catch(SocketException e) {
                    Log.e(UDP_BROADCAST, "SocketException in listen: " + e);
                    Log.i(UDP_BROADCAST, "Listener ending!");
                    return;
                }
                catch(IOException e) {
                    Log.e(UDP_BROADCAST, "IOException in listen: " + e);
                    Log.i(UDP_BROADCAST, "Listener ending!" +e.getMessage());
                    return;
                } catch (InterruptedException e) {
                    e.printStackTrace();
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



