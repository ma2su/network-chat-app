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

public class GroupChatActivity extends AppCompatActivity {

    private static final String SERVER_IP = "172.20.10.5";
    private static final int SERVER_PORT = 6010;
    private static final String TAG = "GroupChatActivity";

    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;
    private ArrayList<Message> messagesList = new ArrayList<>();
    private EditText messageEditText;
    private Button sendMessageButton;

    private String groupName;
    private String currentUsername;

    private Handler autoRefresh = new Handler();
    private Runnable refreshTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main6);

        groupName = getIntent().getStringExtra("GROUP_NAME");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Groupe : " + groupName);
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
                String msg = "GET_GROUP;" + currentUsername + ";" + groupName;
                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                socket.send(packet);

                byte[] buf = new byte[65535];
                DatagramPacket res = new DatagramPacket(buf, buf.length);
                socket.setSoTimeout(4000);

                while(true) {
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
                                        if (!isSentByUser) {
                                            messageText = sender + ":\n" + messageText;
                                        }
                                        serverMessages.add(new Message(messageText, isSentByUser));
                                    }
                                }
                            }

                            // --- SAUVEGARDE LECTURE GROUPE ---
                            SharedPreferences.Editor editor = getSharedPreferences("PREFS_NAME", MODE_PRIVATE).edit();
                            editor.putInt("READ_COUNT_GROUP_" + groupName, serverMessages.size());
                            editor.apply();
                            // ---------------------------------

                            messagesList.clear();
                            messagesList.addAll(serverMessages);
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
                Log.e(TAG, "Erreur: " + e.getMessage());
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
                String msg = "SEND_GROUP;" + currentUsername + ";" + groupName + ";" + messageText;
                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}