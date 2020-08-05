package com.sumit.wifip2p;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class ChatActivity extends AppCompatActivity {

    private ServerSocket serverSocket;
    Handler updateConversationHandler;
    Thread serverThread = null;
    public static final int SERVERPORT = 6000;
    private static String SERVER_IP;
    private Socket socket;


    TextView receivedText;
    EditText yourMessage;
    Button send;
    boolean server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intent = getIntent();

        server = intent.getBooleanExtra("Server?", false);
        SERVER_IP = intent.getStringExtra("Server Address");
        receivedText = (TextView) findViewById(R.id.text_incoming);
        yourMessage = (EditText) findViewById(R.id.text_send);
        send = (Button) findViewById(R.id.btn_send);

        updateConversationHandler = new Handler();

        // If the server open port and if the client listen the port
        if (server) {
            this.serverThread = new Thread(new ServerThread());
            this.serverThread.start();
        } else {
            new Thread(new ClientThread()).start();
        }

        // Sends the message to server
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                    String str = yourMessage.getText().toString();
                    OutputStream s = socket.getOutputStream();
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(s);
                    BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
                    PrintWriter out = new PrintWriter(bufferedWriter, true);
                    out.println(str);
                    Log.i("Send Data",str);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });

            thread.start();

            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Open Socket port for Server and listen incomming messages
    class ServerThread implements Runnable {
        @Override
        public void run() {

            try {
                // Create a socket on port 6000
                serverSocket = new ServerSocket(SERVERPORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Start listening for messages
                    socket = serverSocket.accept();
                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Receives the message over socket
    class CommunicationThread implements Runnable {
        private Socket clientSocket;
        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                // read received data
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = input.readLine();
                    updateConversationHandler.post(new UpdateUIThread(read));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // Show messages on screen
    class UpdateUIThread implements Runnable {
        private String msg;

        public UpdateUIThread(String str) {
            this.msg = str;
        }

        // Print message on screen
        @Override
        public void run() {
            receivedText.setText(receivedText.getText().toString() + "Server Message: " + msg + "\n");
        }
    }

    // Create Client Socket using port and address
    class ClientThread implements Runnable {
        @Override
        public void run() {
            try {
                InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddress, SERVERPORT);
                CommunicationThread commThread = new CommunicationThread(socket);
                new Thread(commThread).start();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
