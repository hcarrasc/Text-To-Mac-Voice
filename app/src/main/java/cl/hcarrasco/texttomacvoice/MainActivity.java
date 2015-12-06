package cl.hcarrasco.texttomacvoice;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    private Socket socket;
    private String ipServer   =null;
    private int    portServer =0;
    InputStream in;
    Thread background;
    DataReceiver dataReceiver;
    RelativeLayout ipConfigView;
    RelativeLayout aboutView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button buttonSend = (Button) findViewById(R.id.send_btn);
        dataReceiver = new DataReceiver();
        ipConfigView = (RelativeLayout) findViewById(R.id.set_ip);
        aboutView = (RelativeLayout) findViewById(R.id.set_about);

        buttonSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                System.out.println("boton clicked sending data...");
                if(ipServer!=null && portServer!=0){
                    EditText et = (EditText) findViewById(R.id.command);
                    String str = ">hc;msg=" + et.getText().toString() + "<";
                    PrintWriter out = null;
                    try {
                        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    out.println(str);
                    out.flush();
                    //background.start();
                }
                else {
                    RelativeLayout ipConfigView = (RelativeLayout) findViewById(R.id.set_ip);
                    ipConfigView.setVisibility(View.VISIBLE);
                }
            }
        });

        Button buttonSetIP = (Button) findViewById(R.id.ip_btn);
        buttonSetIP.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText etIP = (EditText) findViewById(R.id.te_ip);
                EditText etPort = (EditText) findViewById(R.id.te_port);

                ipServer = etIP.getText().toString();
                String portValidator = etPort.getText().toString();
                if(!"".equals(portValidator)){
                    portServer = Integer.parseInt(portValidator);
                }
                else {
                    portServer = 0;
                }

                if(ipServer!=null && portServer!=0){
                    RelativeLayout ipConfigView = (RelativeLayout) findViewById(R.id.set_ip);
                    ipConfigView.setVisibility(View.INVISIBLE);
                    new Thread(new ClientThread()).start();
                }else{
                    RelativeLayout ipConfigView = (RelativeLayout) findViewById(R.id.set_ip);
                    ipConfigView.setVisibility(View.INVISIBLE);
                }
            }
        });

        background = new Thread(new Runnable() {
            // After call for background.start this run method call
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(10000);
                        String SetServerString = "dataToServer";
                        try {
                            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                            if(!out.checkError()){
                                SetServerString = "connected!";
                            }else {
                                SetServerString = "disconnected!";
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        threadMsg(SetServerString);
                    } catch (Throwable t) {
                        // just end the background thread
                        Log.i("Animation", "Thread  exception " + t);
                    }
                }
            }

            private void threadMsg(String msg) {
                if (!msg.equals(null) && !msg.equals("")) {
                    Message msgObj = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("message", msg);
                    msgObj.setData(b);
                    handler.sendMessage(msgObj);
                }
            }

            // Define the Handler that receives messages from the thread and update the progress
            private final Handler handler = new Handler() {
                public void handleMessage(Message msg) {
                    String aResponse = msg.getData().getString("message");
                    if ((null != aResponse)) {
                        Toast.makeText( getBaseContext(), "Server Response: "+aResponse, Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getBaseContext(), "Not Got Response From Server.", Toast.LENGTH_SHORT).show();
                    }
                }
            };
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item1:
                aboutView.setVisibility(View.INVISIBLE);
                ipConfigView.setVisibility(View.VISIBLE);
                return true;
            case R.id.item2:
                Toast.makeText(this, "Option2", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.item3:

                aboutView.setVisibility(View.VISIBLE);
                ipConfigView.setVisibility(View.INVISIBLE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class ClientThread implements Runnable {
        @Override
        public void run() {
            try {
                socket = new Socket(ipServer, portServer);
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    class DataReceiver implements Runnable {
        @Override
        public void run() {
            System.out.println ("esperando datos...");
            try{
                in = socket.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = br.readLine ()) != null) {
                    System.out.println (line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}