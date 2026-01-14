package com.example.sae302;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MainActivity5 extends AppCompatActivity {

    private static final String SERVER_IP = "172.20.10.5";
    private static final int SERVER_PORT = 6010;

    private RecyclerView recyclerViewDiscussions;
    private DiscussionAdapter discussionAdapter;
    private ArrayList<String> discussionsList = new ArrayList<>();
    private BottomNavigationView bottomNavigationView;

    private Handler autoRefresh = new Handler();
    private Runnable refreshTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main5);

        recyclerViewDiscussions = findViewById(R.id.recyclerViewDiscussions);
        recyclerViewDiscussions.setLayoutManager(new LinearLayoutManager(this));

        discussionAdapter = new DiscussionAdapter(discussionsList, this);
        recyclerViewDiscussions.setAdapter(discussionAdapter);

        FloatingActionButton fabNewDiscussion = findViewById(R.id.fabNewDiscussion);
        fabNewDiscussion.setOnClickListener(v -> showFriendsToChatDialog());

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_discussions);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_friends) {
                startActivity(new Intent(getApplicationContext(), MainActivity3.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.navigation_groups) {
                startActivity(new Intent(this, MainActivityGroups.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.navigation_logout) {
                performLogout();
                return true;
            } else if (itemId == R.id.navigation_discussions) {
                return true;
            }
            return false;
        });

        startAutoRefresh();
    }

    private void startAutoRefresh() {
        refreshTask = new Runnable() {
            @Override
            public void run() {
                getFriendsAsDiscussions();
                checkNotifications();


                NotificationHelper.updateGroupBadge(MainActivity5.this, bottomNavigationView);


                autoRefresh.postDelayed(this, 3000);
            }
        };
        autoRefresh.post(refreshTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoRefresh.removeCallbacks(refreshTask);
    }



    private void checkNotifications() {

        if (discussionsList.isEmpty()) return;

        ArrayList<String> listCopy = new ArrayList<>(discussionsList);

        SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        String username = prefs.getString("KEY_USERNAME", "username");

        new Thread(() -> {
            HashMap<String, Integer> unreadCounts = new HashMap<>();
            int totalUnreadGlobal = 0;


            for (String friendName : listCopy) {
                try (DatagramSocket socket = new DatagramSocket()) {
                    String msg = "GET_CHATS;" + username + ";" + friendName;
                    byte[] data = msg.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                    socket.send(packet);

                    byte[] buf = new byte[65535];
                    DatagramPacket res = new DatagramPacket(buf, buf.length);
                    socket.setSoTimeout(500);
                    socket.receive(res);

                    String response = new String(res.getData(), 0, res.getLength()).trim();


                    if (response.startsWith("PING") || response.startsWith("OK") || response.startsWith("ERROR")) {
                        continue;
                    }

                    int serverCount = 0;
                    if (!response.isEmpty()) {
                        String[] msgs = response.split("\\|");
                        for(String m : msgs) {
                            if(!m.trim().isEmpty() && m.contains(";")) {
                                serverCount++;
                            }
                        }
                    }

                    int savedCount = prefs.getInt("READ_COUNT_" + friendName, -1);
                    if (savedCount == -1) {
                        prefs.edit().putInt("READ_COUNT_" + friendName, serverCount).apply();
                        savedCount = serverCount;
                    }

                    int unread = Math.max(0, serverCount - savedCount);
                    unreadCounts.put(friendName, unread);
                    totalUnreadGlobal += unread;

                } catch (Exception e) {
                    android.util.Log.w("CheckNotif", "Échec vérification notif (packet perdu)");
                }
            }

            int finalTotal = totalUnreadGlobal;
            runOnUiThread(() -> {

                if (!isFinishing()) {
                    discussionAdapter.updateUnreadCounts(unreadCounts);

                    if (finalTotal > 0) {
                        var badge = bottomNavigationView.getOrCreateBadge(R.id.navigation_discussions);
                        badge.setVisible(true);
                        badge.setNumber(finalTotal);
                    } else {
                        var badge = bottomNavigationView.getBadge(R.id.navigation_discussions);
                        if (badge != null) badge.setVisible(false);
                    }
                }
            });
        }).start();
    }

    public void deleteDiscussion(String friendName) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer la discussion")
                .setMessage("Voulez-vous supprimer l'historique avec " + friendName + " ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
                    String username = prefs.getString("KEY_USERNAME", "username");

                    prefs.edit().remove("READ_COUNT_" + friendName).apply();

                    new Thread(() -> {
                        try (DatagramSocket socket = new DatagramSocket()) {
                            String msg = "DELETE_CHAT;" + username + ";" + friendName;
                            byte[] data = msg.getBytes();
                            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                            socket.send(packet);


                            runOnUiThread(() -> {
                                Toast.makeText(this, "Suppression demandée", Toast.LENGTH_SHORT).show();
                                getFriendsAsDiscussions();
                            });
                        } catch (Exception e) { e.printStackTrace(); }
                    }).start();
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private void getFriendsAsDiscussions() {
        SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        String username = prefs.getString("KEY_USERNAME", "username");

        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                String msg = "GET_AMIS;" + username;
                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                socket.send(packet);


                byte[] buf = new byte[1024];
                DatagramPacket res = new DatagramPacket(buf, buf.length);
                socket.setSoTimeout(2000);

                while(true) {
                    try {
                        socket.receive(res);
                        String response = new String(res.getData(), 0, res.getLength()).trim();
                        if (response.startsWith("PING")) continue;

                        runOnUiThread(() -> {
                            discussionsList.clear();
                            if (!response.isEmpty() && !response.startsWith("ERREUR")) {
                                String[] parts = response.split(",");
                                for (String s : parts) {
                                    if (!s.trim().isEmpty()) discussionsList.add(s.trim());
                                }
                            }
                            discussionAdapter.notifyDataSetChanged();
                        });
                        break;
                    } catch (SocketTimeoutException e) { break; }
                }
            } catch (Exception e) {
                android.util.Log.e("AutoRefresh", "Erreur rafraîchissement: " + e.getMessage());
            }
        }).start();
    }

    private void showFriendsToChatDialog() {
        SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        String username = prefs.getString("KEY_USERNAME", "username");
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                String msg = "GET_AMIS;" + username;
                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                socket.send(packet);

                byte[] buf = new byte[1024];
                DatagramPacket res = new DatagramPacket(buf, buf.length);
                socket.receive(res);
                String response = new String(res.getData(), 0, res.getLength());

                runOnUiThread(() -> {
                    ArrayList<String> friends = new ArrayList<>();
                    if (!response.isEmpty()) {
                        friends.addAll(Arrays.asList(response.split(",")));
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity5.this);
                    builder.setTitle("Démarrer une nouvelle discussion");
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(MainActivity5.this, android.R.layout.simple_list_item_1, friends);
                    builder.setAdapter(arrayAdapter, (dialog, which) -> {
                        String selectedFriend = friends.get(which);
                        Intent intent = new Intent(MainActivity5.this, MainActivity6.class);
                        intent.putExtra("FRIEND_NAME", selectedFriend);
                        startActivity(intent);
                    });
                    builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());
                    builder.show();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void performLogout() {
        Intent serviceIntent = new Intent(this, HeartbeatService.class);
        stopService(serviceIntent);
        SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        String username = prefs.getString("KEY_USERNAME", "username");
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                String message = "DISCONECT;" + username;
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                socket.send(packet);
            } catch (Exception e) { e.printStackTrace(); }
            runOnUiThread(() -> {
                Intent intent = new Intent(this, MainActivity2.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }).start();
    }
}