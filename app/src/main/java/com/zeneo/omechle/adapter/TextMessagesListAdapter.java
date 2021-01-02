package com.zeneo.omechle.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zeneo.omechle.R;
import com.zeneo.omechle.model.Message;

import java.util.List;

public class TextMessagesListAdapter extends RecyclerView.Adapter<TextMessagesListAdapter.ViewHolder> {

    private List<Message> messages;
    private Context context;

    public TextMessagesListAdapter(List<Message> messages, Context context) {
        this.messages = messages;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TextMessagesListAdapter.ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_text, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TextMessagesListAdapter.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (message.isMe()) {
            holder.sender_me.setVisibility(View.VISIBLE);
            holder.sender_stranger.setVisibility(View.GONE);
            holder.sender_me.setText(message.getText());
        } else {
            holder.sender_me.setVisibility(View.GONE);
            holder.sender_stranger.setVisibility(View.VISIBLE);
            holder.sender_stranger.setText(message.getText());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView sender_me;
        TextView sender_stranger;
        TextView messageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            sender_me = itemView.findViewById(R.id.sender_me_txt);
            sender_stranger = itemView.findViewById(R.id.sender_stranger_txt);
        }
    }
}