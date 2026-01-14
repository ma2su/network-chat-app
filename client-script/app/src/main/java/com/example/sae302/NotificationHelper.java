package com.example.sae302;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class NotificationHelper {

    private static final String SERVER_IP = "172.20.10.5";
    private static final int SERVER_PORT = 6010;


    public static void updateDiscussionBadge(Activity activity, BottomNavigationView nav) {
        new Thread(() -> {
            int totalUnread = 0;
            SharedPreferences prefs = activity.getSharedPreferences("PREFS_NAME", Context.MODE_PRIVATE);
            String username = prefs.getString("KEY_USERNAME", "username");

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(1000);


                String listMsg = "GET_AMIS;" + username;
                String responseList = sendAndReceive(socket, listMsg, username);

                if (!responseList.isEmpty() && !responseList.startsWith("ERREUR")) {
                    String[] friends = responseList.split(",");


                    for (String friend : friends) {
                        String fName = friend.trim();
                        if (fName.isEmpty()) continue;

                        String chatMsg = "GET_CHATS;" + username + ";" + fName;
                        String responseChat = sendAndReceive(socket, chatMsg, username);

                        int serverCount = 0;
                        if (!responseChat.isEmpty()) {
                            String[] msgs = responseChat.split("\\|");
                            for(String m : msgs) if(!m.trim().isEmpty() && m.contains(";")) serverCount++;
                        }

                        int savedCount = prefs.getInt("READ_COUNT_" + fName, 0);
                        totalUnread += Math.max(0, serverCount - savedCount);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }


            int finalTotal = totalUnread;
            activity.runOnUiThread(() -> {
                if (finalTotal > 0) {
                    var badge = nav.getOrCreateBadge(R.id.navigation_discussions);
                    badge.setVisible(true);
                    badge.setNumber(finalTotal);
                } else {
                    var badge = nav.getBadge(R.id.navigation_discussions);
                    if (badge != null) badge.setVisible(false);
                }
            });
        }).start();
    }


    public static void updateGroupBadge(Activity activity, BottomNavigationView nav) {
        new Thread(() -> {
            int totalUnread = 0;
            SharedPreferences prefs = activity.getSharedPreferences("PREFS_NAME", Context.MODE_PRIVATE);
            String username = prefs.getString("KEY_USERNAME", "username");

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(1000);


                String listMsg = "GET_GROUPS;" + username;
                String responseList = sendAndReceive(socket, listMsg, username);

                if (!responseList.isEmpty() && !responseList.startsWith("ERREUR")) {
                    String[] groups = responseList.split(",");


                    for (String group : groups) {
                        String gName = group.trim();
                        if (gName.isEmpty()) continue;

                        String chatMsg = "GET_GROUP;" + username + ";" + gName;
                        String responseChat = sendAndReceive(socket, chatMsg, username);

                        int serverCount = 0;
                        if (!responseChat.isEmpty()) {
                            String[] msgs = responseChat.split("\\|");
                            for(String m : msgs) if(!m.trim().isEmpty() && m.contains(";")) serverCount++;
                        }

                        int savedCount = prefs.getInt("READ_COUNT_GROUP_" + gName, 0);
                        totalUnread += Math.max(0, serverCount - savedCount);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }


            int finalTotal = totalUnread;
            activity.runOnUiThread(() -> {
                if (finalTotal > 0) {
                    var badge = nav.getOrCreateBadge(R.id.navigation_groups);
                    badge.setVisible(true);
                    badge.setNumber(finalTotal);
                } else {
                    var badge = nav.getBadge(R.id.navigation_groups);
                    if (badge != null) badge.setVisible(false);
                }
            });
        }).start();
    }


    private static String sendAndReceive(DatagramSocket socket, String msg, String username) throws Exception {
        byte[] data = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SERVER_IP), SERVER_PORT);
        socket.send(packet);

        byte[] buf = new byte[65535];
        DatagramPacket res = new DatagramPacket(buf, buf.length);


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
                return response;
            } catch (SocketTimeoutException e) {
                return "";
            } catch (Exception e) {
                android.util.Log.e("NotifHelper", "Erreur discrète : " + e.getMessage());
                return "";
            }
        }
    }
}