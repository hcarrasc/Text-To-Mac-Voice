package cl.hcarrasco.texttomacvoice;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Socket socket;
    private String ipServer   = null;
    private int    portServer = 0;
    InputStream    in;
    Thread         background;
    DataReceiver   dataReceiver;
    RelativeLayout ipConfigView;
    RelativeLayout aboutView;
    String         userName;
    String         messageToServer;
    EditText mainEditTextView;
    final int REQ_CODE_SPEECH_INPUT = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataReceiver = new DataReceiver();
        ipConfigView = (RelativeLayout) findViewById(R.id.set_ip);
        aboutView = (RelativeLayout) findViewById(R.id.set_about);
        mainEditTextView = (EditText) findViewById(R.id.command);

        Cursor c = getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
        int count = c.getCount();
        String[] columnNames = c.getColumnNames();
        boolean b = c.moveToFirst();
        int position = c.getPosition();
        if (count == 1 && position == 0) {
            for (int j = 0; j < columnNames.length; j++) {
                String columnName = columnNames[j];
                String columnValue = c.getString(c.getColumnIndex(columnName));
                if ("display_name".equals(columnName)){
                    userName = columnValue;
                    continue;
                }
            }
        }
        c.close();
        Log.i("INFO", userName);


        Button buttonSend = (Button) findViewById(R.id.send_btn);
        buttonSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("INFO", "button clicked sending data...");
                if (ipServer != null && portServer != 0) {
                    EditText et = (EditText) findViewById(R.id.command);
                    if (userName != null) {
                        messageToServer = ">hc;msg=" + et.getText().toString() + ";sender=" + userName + "<";
                    } else {
                        messageToServer = ">hc;msg=" + et.getText().toString() + ";sender=Anonymous<";
                    }

                    PrintWriter out = null;
                    try {
                        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    out.println(messageToServer);
                    out.flush();
                    //background.start();
                } else {
                    RelativeLayout ipConfigView = (RelativeLayout) findViewById(R.id.set_ip);
                    ipConfigView.setVisibility(View.VISIBLE);
                    Toast.makeText(getBaseContext(), "Please, set IP and PORT first to communicate with server", Toast.LENGTH_SHORT).show();
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
                if (!"".equals(portValidator)) {
                    portServer = Integer.parseInt(portValidator);
                } else {
                    portServer = 0;
                }

                if (ipServer != null && portServer != 0) {
                    RelativeLayout ipConfigView = (RelativeLayout) findViewById(R.id.set_ip);
                    ipConfigView.setVisibility(View.INVISIBLE);
                    new Thread(new ClientThread()).start();
                } else {
                    RelativeLayout ipConfigView = (RelativeLayout) findViewById(R.id.set_ip);
                    ipConfigView.setVisibility(View.INVISIBLE);
                }
            }
        });

        ImageView speechButton = (ImageView) findViewById(R.id.btnSpeak);
        speechButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                promptSpeechInput();
            }
        });


        ImageView imagePersonalWeb = (ImageView) findViewById(R.id.logo_wwwpersonal);
        imagePersonalWeb.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goToUrl("http://hcarrasco.cl");
            }
        });

        ImageView imageLinkedin = (ImageView) findViewById(R.id.logo_linkedin);
        imageLinkedin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goToUrl("http://cl.linkedin.com/in/hcarrasc");
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
                        Log.i("INFO", "Thread  exception " + t);
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
                    String response = msg.getData().getString("message");
                    if ((null != response)) {
                        Toast.makeText( getBaseContext(), "Server Response: "+response, Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getBaseContext(), "Not Got Response From Server.", Toast.LENGTH_SHORT).show();
                    }
                }
            };
        });
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), getString(R.string.speech_not_supported), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    mainEditTextView.append(result.get(0));
                }
                break;
            }
        }
    }

    private void goToUrl (String url) {
        Uri uriUrl = Uri.parse(url);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
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
            case R.id.menu_item_share:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.putExtra(Intent.EXTRA_TEXT, "Test voice->mac share intent :D ");
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.app_name)));
                Log.i("INFO", "lanzando share");
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
            Log.i("INFO", "recibiendo datos");
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