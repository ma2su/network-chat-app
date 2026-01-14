package com.example.sae302;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

public class DiscussionAdapter extends RecyclerView.Adapter<DiscussionAdapter.DiscussionViewHolder> {

    private ArrayList<String> discussions;
    private MainActivity5 parentActivity;

    // MAP pour stocker les notifications (Nom -> Nombre de messages)
    private HashMap<String, Integer> unreadCounts = new HashMap<>();

    public DiscussionAdapter(ArrayList<String> discussions, MainActivity5 activity) {
        this.discussions = discussions;
        this.parentActivity = activity;
    }

    // Méthode pour mettre à jour les notifications depuis l'activité
    public void updateUnreadCounts(HashMap<String, Integer> newCounts) {
        this.unreadCounts = newCounts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DiscussionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.discussion_item, parent, false);
        return new DiscussionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiscussionViewHolder holder, int position) {
        String discussionName = discussions.get(position);
        holder.discussionNameTextView.setText(discussionName);

        // --- GESTION DU POINT ROUGE ---
        if (unreadCounts.containsKey(discussionName) && unreadCounts.get(discussionName) > 0) {
            holder.unreadBadge.setVisibility(View.VISIBLE);
            holder.unreadBadge.setText(String.valueOf(unreadCounts.get(discussionName)));
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
        }
        // ------------------------------

        View.OnClickListener openChat = v -> {
            // Quand on ouvre le chat, on remet le compteur à 0 visuellement immédiatement
            unreadCounts.put(discussionName, 0);
            notifyItemChanged(position);

            Intent intent = new Intent(v.getContext(), MainActivity6.class);
            intent.putExtra("FRIEND_NAME", discussionName);
            v.getContext().startActivity(intent);
        };
        holder.discussionNameTextView.setOnClickListener(openChat);
        holder.itemView.setOnClickListener(openChat);

        holder.btnDelete.setOnClickListener(v -> {
            parentActivity.deleteDiscussion(discussionName);
        });
    }

    @Override
    public int getItemCount() {
        return discussions.size();
    }

    public static class DiscussionViewHolder extends RecyclerView.ViewHolder {
        TextView discussionNameTextView;
        TextView unreadBadge; // Ajout
        Button btnDelete;

        public DiscussionViewHolder(@NonNull View itemView) {
            super(itemView);
            discussionNameTextView = itemView.findViewById(R.id.discussionNameTextView);
            unreadBadge = itemView.findViewById(R.id.unreadBadge); // Liaison
            btnDelete = itemView.findViewById(R.id.btnDeleteDiscussion);
        }
    }
}