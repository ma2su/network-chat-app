package com.example.sae302;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivityGroups extends AppCompatActivity {

    private static final String SERVER_IP = "172.20.10.5";
    private static final int SERVER_PORT = 6010;

    private ListView listGroups;
    private GroupAdapter groupAdapter;
    private ArrayList<String> groupsList = new ArrayList<>();
    private BottomNavigationView bottomNavigationView;

    private Handler autoRefresh = new Handler();
    private Runnable refreshTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        listGroups = findViewById(R.id.listGroups);
        groupAdapter = new GroupAdapter(this, groupsList);
        listGroups.setAdapter(groupAdapter);

        FloatingActionButton fab = findViewById(R.id.fabCreateGroup);
        fab.setOnClickListener(v -> showCreateGroupDialog());

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_groups);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_friends) {
                startActivity(new Intent(this, MainActivity3.class));
                overridePendingTransition(0,0);
                return true;
            } else if (id == R.id.navigation_discussions) {
                startActivity(new Intent(this, MainActivity5.class));
                overridePendingTransition(0,0);
                return true;
            } else if (id == R.id.navigation_logout) {
                performLogout();
                return true;
            }
            return id == R.id.navigation_groups;
        });

        startAutoRefresh();
    }

    private void startAutoRefresh() {
        refreshTask = new Runnable() {
            @Override
            public void run() {
                loadGroups();
                checkNotifications();

                NotificationHelper.updateDiscussionBadge(MainActivityGroups.this, bottomNavigationView);

                autoRefresh.postDelayed(this, 5000);
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
        if (groupsList.isEmpty()) return;

        ArrayList<String> listCopy = new ArrayList<>(groupsList);

        SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        String username = prefs.getString("KEY_USERNAME", "username");

        new Thread(() -> {
            HashMap<String, Integer> unreadCounts = new HashMap<>();
            int totalUnreadGlobal = 0;

            for (String groupName : groupsList) {
                try (DatagramSocket socket = new DatagramSocket()) {
                    String msg = "GET_GROUP;" + username + ";" + groupName;
                    byte[] data = msg.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                    socket.send(packet);

                    byte[] buf = new byte[65535];
                    DatagramPacket res = new DatagramPacket(buf, buf.length);
                    socket.setSoTimeout(1000);

                    while (true) {
                        try {
                            socket.receive(res);
                            String response = new String(res.getData(), 0, res.getLength()).trim();

                            if (response.startsWith("PING") || response.startsWith("OK") || response.startsWith("ERROR")) {
                                if(response.startsWith("PING")) {
                                    String pong = "PONG";
                                    byte[] pData = pong.getBytes();
                                    DatagramPacket pPacket = new DatagramPacket(pData, pData.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                                    socket.send(pPacket);
                                }
                                continue;
                            }

                            int serverCount = 0;
                            if (!response.isEmpty()) {
                                String[] msgs = response.split("\\|");
                                for(String m : msgs) if(!m.trim().isEmpty() && m.contains(";")) serverCount++;
                            }

                            int savedCount = prefs.getInt("READ_COUNT_GROUP_" + groupName, 0);
                            int unread = Math.max(0, serverCount - savedCount);
                            unreadCounts.put(groupName, unread);
                            totalUnreadGlobal += unread;
                            break;

                        } catch (SocketTimeoutException e) { break; }
                    }
                } catch (Exception e) {
                    android.util.Log.w("CheckNotif", "Échec vérification notif (packet perdu)");
                }
            }

            int finalTotal = totalUnreadGlobal;
            runOnUiThread(() -> {
                groupAdapter.updateUnreadCounts(unreadCounts);

                if (finalTotal > 0) {
                    var badge = bottomNavigationView.getOrCreateBadge(R.id.navigation_groups);
                    badge.setVisible(true);
                    badge.setNumber(finalTotal);
                } else {
                    var badge = bottomNavigationView.getBadge(R.id.navigation_groups);
                    if (badge != null) badge.setVisible(false);
                }
            });
        }).start();
    }

    private void loadGroups() {
        SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        String username = prefs.getString("KEY_USERNAME", "username");

        sendUDPCommand("GET_GROUPS;" + username, response -> {
            groupsList.clear();
            if (!response.isEmpty() && !response.startsWith("ERREUR")) {
                String[] parts = response.split(",");
                for(String g : parts) {
                    if(!g.trim().isEmpty()) groupsList.add(g.trim());
                }
            }
            groupAdapter.notifyDataSetChanged();
        });
    }

    private void showCreateGroupDialog() {
        final EditText input = new EditText(this);
        input.setHint("Nom du groupe");
        new AlertDialog.Builder(this)
                .setTitle("Créer un groupe")
                .setView(input)
                .setPositiveButton("Créer", (dialog, which) -> {
                    String groupName = input.getText().toString().trim();
                    SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
                    String username = prefs.getString("KEY_USERNAME", "username");
                    if (!groupName.isEmpty()) {
                        sendUDPCommand("CREATE_GROUP;" + groupName + ";" + username, res -> {
                            Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
                            loadGroups();
                        });
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    public void deleteGroup(String groupName) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer le groupe")
                .setMessage("Voulez-vous vraiment supprimer " + groupName + " ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
                    String username = prefs.getString("KEY_USERNAME", "username");
                    prefs.edit().remove("READ_COUNT_GROUP_" + groupName).apply();

                    sendUDPCommand("DELETE_GROUP;" + groupName + ";" + username, res -> {
                        Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
                        loadGroups();
                    });
                })
                .setNegativeButton("Non", null)
                .show();
    }

    public void showGroupOptions(String groupName) {
        String[] options = {"Ouvrir la discussion", "Ajouter un ami", "Retirer un membre"};
        new AlertDialog.Builder(this)
                .setTitle("Gérer " + groupName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openGroupChat(groupName);
                    } else if (which == 1) {
                        showAddMemberDialog(groupName);
                    } else {
                        showRemoveMemberDialog(groupName);
                    }
                })
                .show();
    }

    public void openGroupChat(String groupName) {
        Intent intent = new Intent(this, GroupChatActivity.class);
        intent.putExtra("GROUP_NAME", groupName);
        startActivity(intent);
    }

    private void showAddMemberDialog(String groupName) {
        SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        String username = prefs.getString("KEY_USERNAME", "username");

        sendUDPCommand("GET_AMIS;" + username, response -> {
            if (response.isEmpty() || response.startsWith("ERREUR")) {
                Toast.makeText(this, "Aucun ami trouvé", Toast.LENGTH_SHORT).show();
                return;
            }
            ArrayList<String> friends = new ArrayList<>();
            for(String f : response.split(",")) if(!f.trim().isEmpty()) friends.add(f.trim());

            String[] friendsArray = friends.toArray(new String[0]);
            new AlertDialog.Builder(this)
                    .setTitle("Ajouter qui au groupe ?")
                    .setItems(friendsArray, (d, w) -> {
                        String selectedFriend = friendsArray[w];
                        String cmd = "ADD_TO_GROUP;" + groupName + ";" + username + ";" + selectedFriend;
                        sendUDPCommand(cmd, res -> Toast.makeText(this, res, Toast.LENGTH_SHORT).show());
                    })
                    .show();
        });
    }

    private void showRemoveMemberDialog(String groupName) {
        SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        String username = prefs.getString("KEY_USERNAME", "username");

        sendUDPCommand("GET_GROUP_MEMBERS;" + username + ";" + groupName, response -> {
            if (response.isEmpty() || response.startsWith("ERREUR")) {
                Toast.makeText(this, "Aucun membre trouvé ou erreur", Toast.LENGTH_SHORT).show();
                return;
            }
            ArrayList<String> members = new ArrayList<>();
            for(String m : response.split(",")) if(!m.trim().isEmpty()) members.add(m.trim());
            String[] membersArray = members.toArray(new String[0]);
            new AlertDialog.Builder(this)
                    .setTitle("Retirer qui ?")
                    .setItems(membersArray, (d, w) -> {
                        String selectedMember = membersArray[w];
                        String cmd = "REMOVE_FROM_GROUP;" + groupName + ";" + username + ";" + selectedMember;
                        sendUDPCommand(cmd, res -> Toast.makeText(this, res, Toast.LENGTH_SHORT).show());
                    })
                    .show();
        });
    }

    interface UDPCallback {
        void onResponse(String response);
    }

    private void sendUDPCommand(String fullCommand, UDPCallback callback) {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                byte[] data = fullCommand.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                socket.send(packet);

                byte[] buf = new byte[8192];
                DatagramPacket res = new DatagramPacket(buf, buf.length);
                socket.setSoTimeout(3000);

                socket.receive(res);

                String response = new String(res.getData(), 0, res.getLength()).trim();
                runOnUiThread(() -> callback.onResponse(response));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Erreur serveur", Toast.LENGTH_SHORT).show());
            }
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