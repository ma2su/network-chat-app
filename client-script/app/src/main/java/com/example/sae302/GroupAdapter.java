package com.example.sae302;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;

public class GroupAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<String> groupsList;

    // MAP pour stocker les notifications (NomGroupe -> Nombre)
    private HashMap<String, Integer> unreadCounts = new HashMap<>();

    public GroupAdapter(Context context, ArrayList<String> groupsList) {
        this.context = context;
        this.groupsList = groupsList;
    }

    // Méthode pour mettre à jour les notifications
    public void updateUnreadCounts(HashMap<String, Integer> newCounts) {
        this.unreadCounts = newCounts;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() { return groupsList.size(); }
    @Override
    public Object getItem(int position) { return groupsList.get(position); }
    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.group_item, parent, false);
        }

        TextView txtName = convertView.findViewById(R.id.txtGroupName);
        TextView unreadBadge = convertView.findViewById(R.id.unreadBadge); // Ajout
        Button btnManage = convertView.findViewById(R.id.btnManageGroup);
        Button btnDelete = convertView.findViewById(R.id.btnDeleteGroup);

        String groupName = groupsList.get(position);
        txtName.setText(groupName);

        // --- GESTION DU POINT ROUGE ---
        if (unreadCounts.containsKey(groupName) && unreadCounts.get(groupName) > 0) {
            unreadBadge.setVisibility(View.VISIBLE);
            unreadBadge.setText(String.valueOf(unreadCounts.get(groupName)));
        } else {
            unreadBadge.setVisibility(View.GONE);
        }
        // ------------------------------

        btnDelete.setOnClickListener(v -> {
            if (context instanceof MainActivityGroups) {
                ((MainActivityGroups) context).deleteGroup(groupName);
            }
        });

        btnManage.setOnClickListener(v -> {
            if (context instanceof MainActivityGroups) {
                ((MainActivityGroups) context).showGroupOptions(groupName);
            }
        });

        View.OnClickListener openChatAction = v -> {
            // Reset visuel
            unreadCounts.put(groupName, 0);
            notifyDataSetChanged();

            if (context instanceof MainActivityGroups) {
                ((MainActivityGroups) context).openGroupChat(groupName);
            }
        };

        txtName.setOnClickListener(openChatAction);
        convertView.setOnClickListener(openChatAction);

        btnManage.setFocusable(false);
        btnDelete.setFocusable(false);

        return convertView;
    }
}