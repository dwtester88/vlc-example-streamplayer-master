package com.pedro.vlctestapp;

import android.content.Intent;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.StaticLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.pedro.vlc.VlcListener;
import com.pedro.vlc.VlcVideoLibrary;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by pedro on 25/06/17.
 */
public class MainActivity extends AppCompatActivity implements VlcListener, View.OnClickListener {

  private VlcVideoLibrary vlcVideoLibrary;
  private Button bStartStop,bgetpiture,btnpush,btnvideo10;
  private EditText etEndpoint;
 // public ImageView image;
  SurfaceView surfaceView;


  static final String LOG_TAG = "MainActivity";
  private static final int LISTENER_PORT = 50003;
  private static final int BUF_SIZE = 1024;
  //private ContactManager contactManager;
  private String displayName, Serverip;
  private boolean STARTED = false;
  private boolean IN_CALL = false;

    private boolean LISTEN = false;
    private boolean video10=false;
  EditText IPAddress;
  MakeCall makeCall = new MakeCall();
  ServiceListner serviceListner = new ServiceListner();


  File file;
    long start, end; // used for 10 sec video


  static final int SocketServerPORT = 1234;
    static ImageView image=null;


    public void getpicture(String msg) {

        Toast.makeText(getApplicationContext(),"In Progress...",Toast.LENGTH_LONG).show();
        surfaceView.setVisibility(View.INVISIBLE);
        vlcVideoLibrary.stop();

        image.setVisibility(View.INVISIBLE);
        image.setImageURI(null);
        bStartStop.setText(getString(R.string.start_player));
        ClientRxThread clientRxThread =
                new ClientRxThread(
                        etEndpoint.getText().toString(),
                        SocketServerPORT, msg);

        clientRxThread.start();


    }



  @Override
  protected void onCreate(Bundle savedInstanceState) {

    Intent intent = getIntent();
    String serverip =  null;
    serverip=intent.getStringExtra("ServerIP");
      boolean service_flag = intent.getBooleanExtra("Service_Flag",false);
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.activity_main);
    surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
    bStartStop = (Button) findViewById(R.id.b_start_stop);
    bgetpiture =(Button) findViewById(R.id.getpicture);
    btnpush = (Button) findViewById(R.id.b_PushToTalk);
    btnvideo10 = (Button) findViewById(R.id.video10sec);

    bStartStop.setOnClickListener(this);
    etEndpoint = (EditText) findViewById(R.id.et_endpoint);
    etEndpoint.setText(serverip);
    surfaceView.setVisibility(View.VISIBLE);
    vlcVideoLibrary = new VlcVideoLibrary(this, this, surfaceView);
    image = (ImageView) findViewById(R.id.image1);
    image.setVisibility(View.INVISIBLE);
      Log.d("Mainactivity","OPEN");

    //Start the service to cheack broadcast IP from server.
   // startService(new Intent(this,ServiceListner.class) ); // service is already started when device is booted.

    //serviceListner.LISTEN=false;

      if(service_flag){
          getpicture("First");
      }



    //take picture
    bgetpiture.setOnClickListener(new View.OnClickListener(){

      @Override
      public void onClick(View v) {


        Toast.makeText(getApplicationContext(),"In Progress...",Toast.LENGTH_LONG).show();
        surfaceView.setVisibility(View.INVISIBLE);
        vlcVideoLibrary.stop();

        image.setVisibility(View.INVISIBLE);
        image.setImageURI(null);
        bStartStop.setText(getString(R.string.start_player));
        ClientRxThread clientRxThread =
                new ClientRxThread(
                        etEndpoint.getText().toString(),
                        SocketServerPORT, "SNAPSHOT");

        clientRxThread.start();
      }});




    btnpush.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {

        Serverip = etEndpoint.getText().toString();
        // Log.d(LOG_TAG,"Server is "+Serverip);

        if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){

          if (Serverip.isEmpty()) {
            Toast.makeText(getApplicationContext(), "enter the Server IP", Toast.LENGTH_LONG).show();
          } else {




            //btnStart.setText("End");

            Log.i(LOG_TAG, "Start button pressed");

            Log.i(LOG_TAG, "Server IP" + Serverip.toString());
            STARTED = true;


            //contactManager = new ContactManager(displayName, getBroadcastIp());
            InetAddress ip = null;
            try {
              ip = InetAddress.getByName(Serverip);
              Log.i(LOG_TAG, "Server InetAddress" + ip.toString());
            } catch (UnknownHostException e) {
              e.printStackTrace();
            }
            IN_CALL = true;
            // Send this information to the MakeCallActivity and start that activity
            //Intent intent = new Intent(MainActivity.this, MakeCallActivity.class);
            //intent.putExtra(EXTRA_CONTACT, contact);
            String address = ip.toString();
            address = address.substring(1, address.length());
            Log.i(LOG_TAG, "Server Address" + address);
            makeCall.makeCall(address);
          }

          //intent.putExtra(EXTRA_IP, address);
          //intent.putExtra(EXTRA_DISPLAYNAME, displayName);
          //startActivity(intent);
        }
        else if (motionEvent.getAction()== MotionEvent.ACTION_UP && IN_CALL){

          Log.i(LOG_TAG, "Start button released");

          if (Serverip.isEmpty()) {
            Toast.makeText(getApplicationContext(), "enter the Server IP", Toast.LENGTH_LONG).show();
          } else {
            Log.i(LOG_TAG, "Start button released endcall");
            makeCall.endCall();
          }
        }


        return false;
      }
    });

        //Git Commit: 10sec video button built for 10sec video stream from server
      btnvideo10.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              if (!vlcVideoLibrary.isPlaying()) {
                  Log.d(LOG_TAG,"turning video10 click first ");
                  vlcVideoLibrary.stop();
                  video10 = true;
                  surfaceView.setVisibility(View.VISIBLE);
                  image.setVisibility(View.INVISIBLE);
                  vlcVideoLibrary.play("rtsp://"+etEndpoint.getText().toString()+":8555");
                  bStartStop.setText(getString(R.string.stop_player));
                  etEndpoint.setVisibility(View.INVISIBLE);
              } else {
                  Log.d(LOG_TAG,"Turning video10 off");
                  video10 = false;
                  surfaceView.setVisibility(View.INVISIBLE);
                  image.setVisibility(View.VISIBLE);
                  image.setImageURI(null);
                  vlcVideoLibrary.stop();
                  bStartStop.setText(getString(R.string.start_player));
                  etEndpoint.setVisibility(View.VISIBLE);
              }
          }
      });

   /* bgetpiture.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {

       // String hostname = "192.168.1.100";
        String hostname= etEndpoint.getText().toString();
        int port = 1234;
        Socket clientSocket=null;
        DataOutputStream os=null;
        BufferedReader is=null;

        try {
          clientSocket = new Socket(hostname,port);
          os = new DataOutputStream(clientSocket.getOutputStream());
          is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (UnknownHostException e) {
          System.err.println("Don't know about host: " + hostname);
          Log.d("unnerror ",e.getMessage());
        }  catch (IOException e) {
          Log.d("ioeerror ",e.getMessage());
          e.printStackTrace();
        }

        // If everything has been initialized then we want to write some data
        // to the socket we have opened a connection to on the given port

        if (clientSocket == null || os == null || is == null) {
          System.err.println( "Something is wrong. One variable is null." );
          Toast.makeText(getApplicationContext(), "Something is wrong. One variable is null.",Toast.LENGTH_LONG).show();
          return;
        }

        try {
          while ( true ) {
            System.out.print( "Enter an integer (0 to stop connection, -1 to stop server): " );
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String keyboardInput = br.readLine();
            os.writeBytes( keyboardInput + "\n" );

            int n = Integer.parseInt( keyboardInput );
            if ( n == 0 || n == -1 ) {
              break;
            }

            String responseLine = is.readLine();
            System.out.println("Server returns its square as: " + responseLine);
          }

          // clean up:
          // close the output stream
          // close the input stream
          // close the socket

          os.close();
          is.close();
          clientSocket.close();
        } catch (UnknownHostException e) {
          System.err.println("Trying to connect to unknown host: " + e);
          Toast.makeText(getApplicationContext(),"UnknownError "+e.getMessage().toString(),Toast.LENGTH_LONG).show();
        } catch (IOException e) {
          System.err.println("IOException:  " + e);
          Toast.makeText(getApplicationContext(),"IOError "+e.getMessage().toString(),Toast.LENGTH_LONG).show();
        }
      }
    });*/
  }



  private class ClientRxThread extends Thread {
    String dstAddress,dstmsg;
    int dstPort;

    ClientRxThread(String address, int port, String msg) {
      dstAddress = address;
      dstPort = port;
        dstmsg = msg;

    }

    @Override
    public void run() {
      Socket socket = null;

      try {
        socket = new Socket(dstAddress, dstPort);

        Log.d("destination port",dstAddress);


        file = new File(
                Environment.getExternalStorageDirectory(),
                "test.png");

        JSONObject obj = new JSONObject();
        obj.put("new",dstmsg);
        String msg=obj.toString();
        Log.d("Json",msg);

        //if condition ture the go to sobo ip camera else go to android app example1
        if(dstAddress.equals("192.168.1.10")){
          BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
          out.write(msg);
          out.flush();

        }
        else{
          DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
          dOut.writeUTF(msg);
          dOut.flush();

        }
       // BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        //out.write(msg);
        //out.flush();
       //Send SNAPSHOT text to server 192.168.1.100:1234
        byte[] bytes = new byte[1024];
        InputStream is = socket.getInputStream();
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        int bytesRead;
        while ((bytesRead = is.read(bytes)) != -1) {
          bos.write(bytes, 0, bytesRead);
        }
       // DataOutputStream dOut = new DataOutputStream(socket.getOutputStream()); // use this to send json to sobo
        //dOut.writeUTF("SNAPSHOT SNAPSHOT SNAPSHOT");
        //dOut.flush();
        //bos.write(bytes, 0, bytesRead);
        bos.close();
        socket.close();
        MainActivity.this.runOnUiThread(new Runnable() {

          @Override
          public void run() {
            Toast.makeText(MainActivity.this,
                    "Finished",
                    Toast.LENGTH_LONG).show();
            Uri uri = Uri.fromFile(file);

            Log.d("MainActivity", "URI is " + uri.toString());

            image.setImageURI(uri);
            image.requestFocus();
            image.setVisibility(View.VISIBLE);
              image.setRotation(-90);
          }});

      } catch (IOException e) {

        e.printStackTrace();

        final String eMsg = "Something wrong: " + e.getMessage();
        MainActivity.this.runOnUiThread(new Runnable() {

          @Override
          public void run() {
            Toast.makeText(MainActivity.this,
                    eMsg,
                    Toast.LENGTH_LONG).show();
          }});

      } catch (JSONException e) {
        e.printStackTrace();
      } finally {
        if(socket != null){
          try {
            socket.close();
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }
    }
  }

  private void stopCallListener() {
    // Ends the listener thread
    LISTEN = false;
  }

  @Override
  public void onPause() {

    super.onPause();
    if (STARTED) {

      //contactManager.bye(displayName);
      //contactManager.stopBroadcasting();
      //contactManager.stopListening();
      //STARTED = false;
    }
    stopCallListener();
    Log.i(LOG_TAG, "App paused!");
  }

  @Override
  public void onStop() {

    super.onStop();
    Log.i(LOG_TAG, "App stopped!");
    stopCallListener();
    if (!IN_CALL) {

      finish();
    }
  }

  @Override
  public void onRestart() {

    super.onRestart();
    Log.i(LOG_TAG, "App restarted!");
    IN_CALL = false;
    STARTED = true;
    //contactManager = new ContactManager(displayName, getBroadcastIp());
    //startCallListener();
  }


  @Override
  public void onComplete() {
    Toast.makeText(this, "Playing", Toast.LENGTH_SHORT).show();

      ////Git Commet: 10sec video if condition true then button is click again automatically adter 15sec
      if(video10) {
          start=System.currentTimeMillis();
          end=start+15000;
          Log.d(LOG_TAG, "video10 if true");
          while(System.currentTimeMillis()<end) {

          }
          btnvideo10.performClick();
          Log.d(LOG_TAG, "video10 click secondtime ");

      }
  }

  @Override
  public void onError() {
    Toast.makeText(this, "Error, make sure your endpoint is correct", Toast.LENGTH_SHORT).show();
    vlcVideoLibrary.stop();
    etEndpoint.setVisibility(View.VISIBLE);
    bStartStop.setText(getString(R.string.start_player));
  }

  @Override
  public void onClick(View view) {
      video10 = false;
      if (!vlcVideoLibrary.isPlaying()) {
      vlcVideoLibrary.stop();
      surfaceView.setVisibility(View.VISIBLE);
      image.setVisibility(View.INVISIBLE);
      vlcVideoLibrary.play("rtsp://"+etEndpoint.getText().toString()+":8555");
      bStartStop.setText(getString(R.string.stop_player));
      etEndpoint.setVisibility(View.INVISIBLE);
    } else {
        surfaceView.setVisibility(View.INVISIBLE);
        image.setVisibility(View.VISIBLE);
        image.setImageURI(null);
      vlcVideoLibrary.stop();
      bStartStop.setText(getString(R.string.start_player));
      etEndpoint.setVisibility(View.VISIBLE);
    }
  }



  @Override
  protected void onDestroy() {
    serviceListner.LISTEN=true;
    startService(new Intent(this,ServiceListner.class) );
    super.onDestroy();
  }
}
