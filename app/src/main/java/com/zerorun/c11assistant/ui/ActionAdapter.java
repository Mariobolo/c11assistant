package com.leapmotor.c11assistant.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.leapmotor.c11assistant.R;
import com.leapmotor.c11assistant.model.ActionItem;
import java.util.List;

public class ActionAdapter extends RecyclerView.Adapter<ActionAdapter.ActionVH> {
    public interface OnActionClickListener { void onActionClick(ActionItem item); }

    private final List<ActionItem> items;
    private final OnActionClickListener listener;

    public ActionAdapter(List<ActionItem> items, OnActionClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull @Override public ActionVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_action, parent, false);
        return new ActionVH(view);
    }

    @Override public void onBindViewHolder(@NonNull ActionVH holder, int position) {
        ActionItem item = items.get(position);
        holder.title.setText(item.id + " (" + item.type + ")");
        holder.meta.setText("包名: " + item.packageName + " | 启动方式: " + item.launchMode + " | 延时: " + item.delayMs + "ms");
        holder.itemView.setAlpha(item.enabled ? 1f : 0.5f);
        holder.itemView.setOnClickListener(v -> listener.onActionClick(item));
    }

    @Override public int getItemCount() { return items.size(); }

    static class ActionVH extends RecyclerView.ViewHolder {
        TextView title;
        TextView meta;

        ActionVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvActionTitle);
            meta = itemView.findViewById(R.id.tvActionMeta);
        }
    }
}
