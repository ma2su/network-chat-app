package com.example.sae302;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class HeartbeatService extends Service {

    private boolean isRunning = false;
    private static final String SERVER_IP = "172.20.10.5";
    private static final int SERVER_PORT = 6010;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            isRunning = true;
            startHeartbeat();
        }
        return START_STICKY;
    }

    private void startHeartbeat() {
        SharedPreferences prefs = getSharedPreferences("PREFS_NAME", MODE_PRIVATE);
        String username = prefs.getString("KEY_USERNAME", "username");

        new Thread(() -> {
            Log.d("Heartbeat", "Service démarré pour : " + username);
            while (isRunning) {
                try (DatagramSocket socket = new DatagramSocket()) {
                    // On envoie juste PONG pour dire "Je suis là"
                    String msg = "PONG;" + username;
                    byte[] data = msg.getBytes();
                    InetAddress address = InetAddress.getByName(SERVER_IP);
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, SERVER_PORT);
                    socket.send(packet);

                    // On N'ATTEND PLUS de réponse ici.
                    // Cela évite que le service tue l'application s'il y a un conflit de port.

                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(3000); // On répète toutes les 3 secondes
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
