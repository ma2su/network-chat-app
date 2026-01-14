package com.example.sae302;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class MainActivity6 extends AppCompatActivity {

    private static final String SERVER_IP = "172.20.10.5";
    private static final int SERVER_PORT = 6010;
    private static final String TAG = "MainActivity6";

    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;
    private ArrayList<Message> messagesList = new ArrayList<>();
    private EditText messageEditText;
    private Button sendMessageButton;

    private String friendName;
    private String currentUsername;

    private Handler autoRefresh = new Handler();
    private Runnable refreshTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main6);

        friendName = getIntent().getStringExtra("FRIEND_NAME");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chat : " + friendName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        currentUsername = prefs.getString("KEY_USERNAME", "username");

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        messageEditText = findViewById(R.id.messageEditText);
        sendMessageButton = findViewById(R.id.sendMessageButton);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);

        messageAdapter = new MessageAdapter(messagesList);
        recyclerViewMessages.setAdapter(messageAdapter);

        sendMessageButton.setOnClickListener(v -> sendMessage());

        startAutoRefresh();
    }

    private void startAutoRefresh() {
        refreshTask = new Runnable() {
            @Override
            public void run() {
                loadMessages();
                autoRefresh.postDelayed(this, 1000);
            }
        };
        autoRefresh.post(refreshTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoRefresh.removeCallbacks(refreshTask);
    }

    private void loadMessages() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                String msg = "GET_CHATS;" + currentUsername + ";" + friendName;
                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                socket.send(packet);

                byte[] buf = new byte[65535];
                DatagramPacket res = new DatagramPacket(buf, buf.length);
                socket.setSoTimeout(5000);


                while (true) {
                    try {
                        socket.receive(res);
                        String response = new String(res.getData(), 0, res.getLength()).trim();

                        if (response.startsWith("PING")) {
                            String pong = "PONG";
                            byte[] pData = pong.getBytes();
                            DatagramPacket pPacket = new DatagramPacket(pData, pData.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                            socket.send(pPacket);
                            continue;
                        }

                        runOnUiThread(() -> {
                            ArrayList<Message> serverMessages = new ArrayList<>();
                            if (!response.isEmpty()) {
                                String[] messages = response.split("\\|");
                                for (String messageLine : messages) {
                                    if (messageLine.trim().isEmpty()) continue;
                                    String[] parts = messageLine.split(";", 4);
                                    if (parts.length == 4) {
                                        String sender = parts[1];
                                        String messageText = parts[3].trim();
                                        boolean isSentByUser = sender.equals(currentUsername);
                                        serverMessages.add(new Message(messageText, isSentByUser));
                                    }
                                }
                            }


                            SharedPreferences.Editor editor = getSharedPreferences("PREFS_NAME", MODE_PRIVATE).edit();
                            editor.putInt("READ_COUNT_" + friendName, serverMessages.size());
                            editor.apply();
                            // --------------------------------------------

                            ArrayList<Message> optimisticMessages = new ArrayList<>();
                            for (Message localMsg : messagesList) {
                                if (localMsg.isSentByUser()) {
                                    boolean foundOnServer = false;
                                    for (Message serverMsg : serverMessages) {
                                        if (serverMsg.isSentByUser() && serverMsg.getText().equals(localMsg.getText())) {
                                            foundOnServer = true;
                                            break;
                                        }
                                    }
                                    if (!foundOnServer) {
                                        optimisticMessages.add(localMsg);
                                    }
                                }
                            }

                            messagesList.clear();
                            messagesList.addAll(serverMessages);
                            messagesList.addAll(optimisticMessages);
                            messageAdapter.notifyDataSetChanged();

                            if (!messagesList.isEmpty()) {
                                recyclerViewMessages.scrollToPosition(messagesList.size() - 1);
                            }
                        });
                        break;

                    } catch (SocketTimeoutException e) {
                        break;
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Erreur loadMessages: " + e.getMessage());
            }
        }).start();
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        if (messageText.isEmpty()) return;

        Message newMessage = new Message(messageText, true);
        messagesList.add(newMessage);
        messageAdapter.notifyItemInserted(messagesList.size() - 1);
        recyclerViewMessages.scrollToPosition(messagesList.size() - 1);
        messageEditText.setText("");

        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                String msg = "SEND_CHAT;" + currentUsername + ";" + friendName + ";" + messageText;
                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}