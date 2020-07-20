package co.beh.gyroscope;


import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import static co.beh.gyroscope.GyroscopeActivity.END_MARKER;
import static co.beh.gyroscope.GyroscopeActivity.START_MARKER;


public class ledControl extends AppCompatActivity {
    Handler progressBarHandler = new Handler();
    Button btnTest, btnDis;
    SeekBar brightness;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    static BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    MyApplication app;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String TAG = ledControl.class.getSimpleName();
    private int direction = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ledControl
        setContentView(R.layout.activity_led_control);

        //call the widgtes
        btnTest = findViewById(R.id.button2);
        btnDis = findViewById(R.id.button4);
        brightness = findViewById(R.id.seekBar);
        app = MyApplication.getInstance();
        new ConnectBT().execute(); //Call the class to connect

        //commands to be sent to bluetooth
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOnLed();      //method to turn on
                direction = 1;
            }
        });



        btnDis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect(); //close connection
            }
        });

        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser == true) {

                    try {
                        if (direction == 1) {
                            btSocket.getOutputStream().write("<123>".getBytes());
                        } else {
                            btSocket.getOutputStream().write("<023>".getBytes());
                    }

                        // btSocket.getInputStream().toString();
                    } catch (IOException e) {

                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void Disconnect() {
        if (btSocket != null) //If the btSocket is busy
        {
            try {
                btSocket.getOutputStream().write("3 \n".toString().getBytes());
                btSocket.close(); //close connection
            } catch (IOException e) {
                msg("Error");
            }
        }
        finish(); //return to the first layout

    }

    private void turnOffLed() {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write((START_MARKER + "L100" + END_MARKER).getBytes());
//                progressBarHandler.post(new Runnable() {
//
//                    public void run() {
//                        brightness.setProgress(25);
//                    }
//                });

            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    private void turnOnLed() {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write((START_MARKER + "R100" + END_MARKER).getBytes());
//                progressBarHandler.post(new Runnable() {
//
//                    public void run() {
//                        brightness.setProgress(25);
//                    }
//                });
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    // fast way to call Toast
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        if (id == R.id.action_voice) {
            promptSpeechInput();
        }

        if (id == R.id.action_gyroscope) {
            startActivity(new Intent(app, GyroscopeActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(ledControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            } catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    JSONObject rgb = new JSONObject();
                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    // txtSpeechInput.setText(result.get(0));
                    Log.e(TAG, result.get(0));
                    Toast.makeText(app, result.get(0), Toast.LENGTH_SHORT).show();
                    switch (result.get(0).toLowerCase()) {


                        case "right":

                            try {
                                btSocket.getOutputStream().write("125 \n".toString().getBytes());
                                progressBarHandler.post(new Runnable() {

                                    public void run() {
                                        brightness.setProgress(25);
                                    }
                                });
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case "left":

                            try {
                                btSocket.getOutputStream().write("025 \n".toString().getBytes());
                                progressBarHandler.post(new Runnable() {

                                    public void run() {
                                        brightness.setProgress(25);
                                    }
                                });
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;

                        case "fast":

                            try {
                                btSocket.getOutputStream().write("4 \n".toString().getBytes());
                                progressBarHandler.post(new Runnable() {

                                    public void run() {
                                        brightness.setProgress(brightness.getProgress() + 10);
                                    }
                                });

                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case "slow":

                            try {
                                btSocket.getOutputStream().write("5 \n".toString().getBytes());
                                progressBarHandler.post(new Runnable() {

                                    public void run() {
                                        brightness.setProgress(brightness.getProgress() - 10);
                                    }
                                });
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case "stop":

                            try {
                                btSocket.getOutputStream().write("3 \n".toString().getBytes());
                                progressBarHandler.post(new Runnable() {

                                    public void run() {
                                        brightness.setProgress(1);
                                    }
                                });
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;

                    }
                }
                break;
            }

        }
    }
}
