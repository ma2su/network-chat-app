package com.example.sae302;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class FriendRequestAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<String> requests;
    private static final String SERVER_IP = "172.20.10.5";
    private static final int SERVER_PORT = 6010;

    public FriendRequestAdapter(Context ctx, ArrayList<String> data){
        this.context = ctx;
        this.requests = data;
    }

    @Override
    public int getCount() {
        return requests.size();
    }

    @Override
    public Object getItem(int position) {
        return requests.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.request_item, parent, false);
        }

        TextView username = convertView.findViewById(R.id.txtUsername);
        Button btnAccept = convertView.findViewById(R.id.btnAccept);
        Button btnRefuse = convertView.findViewById(R.id.btnRefuse);

        String friendName = requests.get(position);
        username.setText(friendName);

        // Attention : pour éviter plusieurs listeners superposés, on annule et recolle
        btnAccept.setOnClickListener(null);
        btnRefuse.setOnClickListener(null);

        btnAccept.setOnClickListener(v -> handleRequest(friendName, true));
        btnRefuse.setOnClickListener(v -> handleRequest(friendName, false));

        return convertView;
    }

    private void handleRequest(String friendName, boolean accept){

        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {

                SharedPreferences prefs = context.getSharedPreferences("PREFS_NAME", Context.MODE_PRIVATE);
                String username = prefs.getString("KEY_USERNAME", "username");

                String message = (accept ?
                        "ACCEPT_FRIEND;" :
                        "DECLINE_FRIEND;") + username + ";" + friendName;

                byte[] data = message.getBytes();
                InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
                DatagramPacket p = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
                socket.send(p);

                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                String response = new String(receivePacket.getData(), 0, receivePacket.getLength());

                if (context instanceof MainActivity3) {
                    ((MainActivity3) context).runOnUiThread(() -> {
                        if(response.contains("OK")){
                            Toast.makeText(context, accept ?
                                    "Ami accepté !" :
                                    "Demande refusée.", Toast.LENGTH_SHORT).show();

                            // Retire de la liste et met à jour l'affichage
                            requests.remove(friendName);
                            notifyDataSetChanged();

                            // Recharge la liste d'amis si on a accepté
                            if (accept) {
                                ((MainActivity3) context).GetFriends();
                            }

                        } else {
                            Toast.makeText(context, "Erreur : " + response, Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            } catch (Exception e){
                e.printStackTrace();
                if (context instanceof MainActivity3) {
                    ((MainActivity3) context).runOnUiThread(() ->
                            Toast.makeText(context, "Erreur réseau", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();

    }
}
