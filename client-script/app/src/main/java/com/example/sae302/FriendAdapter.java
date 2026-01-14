package com.example.sae302;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class FriendAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<String> friendsList;

    public FriendAdapter(Context context, ArrayList<String> friendsList) {
        this.context = context;
        this.friendsList = friendsList;
    }

    @Override
    public int getCount() {
        return friendsList.size();
    }

    @Override
    public Object getItem(int position) {
        return friendsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Chargement du design XML qu'on vient de créer
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.friend_item, parent, false);
        }

        TextView txtName = convertView.findViewById(R.id.txtFriendName);
        Button btnDelete = convertView.findViewById(R.id.btnDeleteFriend);

        String friendName = friendsList.get(position);
        txtName.setText(friendName);

        // Gestion du clic sur le bouton supprimer
        btnDelete.setOnClickListener(v -> {
            if (context instanceof MainActivity3) {
                // On appelle la fonction REMOVE_FRIEND dans MainActivity3
                ((MainActivity3) context).REMOVE_FRIEND(friendName);
            }
        });

        return convertView;
    }
}