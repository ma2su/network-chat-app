package com.example.sae302;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class MainActivity3 extends AppCompatActivity {

    private static final String SERVER_IP = "172.20.10.5";
    private static final int SERVER_PORT = 6010;

    private EditText inputFriend;
    private Button BtnAddFriend;

    private ListView listFriends, listRequests;

    private FriendAdapter friendsAdapter;
    private FriendRequestAdapter requestAdapter;

    private ArrayList<String> friendsList = new ArrayList<>();
    private ArrayList<String> requestList = new ArrayList<>();

    private Handler autoRefresh = new Handler();
    private Runnable refreshTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        inputFriend = findViewById(R.id.inputFriend);
        BtnAddFriend = findViewById(R.id.BtnAddFriend);

        listFriends = findViewById(R.id.listFriends);
        listRequests = findViewById(R.id.listRequests);

        friendsAdapter = new FriendAdapter(this, friendsList);
        listFriends.setAdapter(friendsAdapter);

        requestAdapter = new FriendRequestAdapter(this, requestList);
        listRequests.setAdapter(requestAdapter);

        BtnAddFriend.setOnClickListener(v -> {
            String username = inputFriend.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(this, "Le nom d'utilisateur ne peut pas être vide", Toast.LENGTH_SHORT).show();
            } else {
                ADDFRIEND(username);
            }
        });



        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_friends);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_discussions) {
                startActivity(new Intent(getApplicationContext(), MainActivity5.class));
                overridePendingTransition(0, 0);
                return true;
            }
            else if (itemId == R.id.navigation_groups) {
                startActivity(new Intent(getApplicationContext(), MainActivityGroups.class));
                overridePendingTransition(0, 0);
                return true;
            }
            else if (itemId == R.id.navigation_logout) { // --- CAS DECONNEXION ---
                performLogout();
                return true;
            }
            else if (itemId == R.id.navigation_friends) {
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
                GetFriends();
                GetRequests();

                // --- AJOUT : Vérification Globale ---
                BottomNavigationView nav = findViewById(R.id.bottom_navigation);
                NotificationHelper.updateDiscussionBadge(MainActivity3.this, nav);
                NotificationHelper.updateGroupBadge(MainActivity3.this, nav);
                // -----------------------------------

                autoRefresh.postDelayed(this, 4000);
            }
        };
        autoRefresh.post(refreshTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoRefresh.removeCallbacks(refreshTask);
    }

    public void REMOVE_FRIEND(String friendName) {
        SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        String username = prefs.getString("KEY_USERNAME", "username");

        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                String message = "SUPPR_FRIEND;" + username + ";" + friendName;
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                socket.send(packet);

                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                String reply = new String(receivePacket.getData(), 0, receivePacket.getLength());

                runOnUiThread(() -> {
                    Toast.makeText(this, reply, Toast.LENGTH_SHORT).show();
                    if(reply.contains("OK") || reply.contains("SUPPRIME")) {
                        GetFriends();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void ADDFRIEND(String lui){
        SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        String username = prefs.getString("KEY_USERNAME", "username");

        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                String message = "ADD_FRIEND;" + username + ";" + lui;
                byte[] data = message.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                socket.send(packet);
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                String reply = new String(receivePacket.getData(), 0, receivePacket.getLength());
                runOnUiThread(() -> Toast.makeText(this, reply, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void LOGOUT(){
        // Cette méthode est utilisée par le bouton "Déconnexion" dans la page
        performLogout();
    }

    public void GetRequests() {
        SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        String username = prefs.getString("KEY_USERNAME", "username");

        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                String msg = "GET_REQUESTS;" + username;
                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
                socket.send(packet);
                byte[] buf = new byte[1024];
                DatagramPacket res = new DatagramPacket(buf, buf.length);
                socket.receive(res);
                String response = new String(res.getData(), 0, res.getLength());
                runOnUiThread(() -> {
                    requestList.clear();
                    if (!response.isEmpty())
                        for (String r : response.split(","))
                            requestList.add(r.trim());
                    requestAdapter.notifyDataSetChanged();
                });
            } catch (Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    public void GetFriends(){
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
                    friendsList.clear();
                    if (!response.isEmpty()) {
                        String[] parts = response.split(",");
                        for (String f : parts) {
                            if (!f.trim().isEmpty()) {
                                friendsList.add(f.trim());
                            }
                        }
                    }
                    friendsAdapter.notifyDataSetChanged();
                });
            } catch (Exception e){
                e.printStackTrace();
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
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                Intent intent = new Intent(this, MainActivity2.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }).start();
    }
}