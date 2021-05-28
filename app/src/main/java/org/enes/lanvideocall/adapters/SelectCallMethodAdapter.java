package org.enes.lanvideocall.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.enes.lanvideocall.R;

public class SelectCallMethodAdapter extends RecyclerView.Adapter
        <SelectCallMethodAdapter.SelectCallMethodViewHolder> {

    public interface SelectCallMethodAdapterSelectedListener {

        void onSelectCallMethodAdapterItemPressed(int position);

    }

    private SelectCallMethodAdapterSelectedListener listener;

    public void setSelectCallMethodAdapterSelectedListener(
            SelectCallMethodAdapterSelectedListener new_listener) {
        listener = new_listener;
    }

    public SelectCallMethodAdapter() {
        super();
    }

    @NonNull
    @Override
    public SelectCallMethodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.adapter_select_call_method_to_user,parent,false);
        SelectCallMethodViewHolder selectCallMethodViewHolder =
                new SelectCallMethodViewHolder(view);
        return selectCallMethodViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull SelectCallMethodViewHolder holder, int position) {
        if(position == 0) {
            holder.iv_icon.setImageResource(R.drawable.ic_local_phone_black_32dp);
            holder.tv_method.setText(holder.tv_method.getContext().getString(R.string.txt_audio_call));
        }else if (position == 1) {
            holder.iv_icon.setImageResource(R.drawable.ic_video_call_black_24dp);
            holder.tv_method.setText(holder.tv_method.getContext().getString(R.string.txt_video_call));
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    class SelectCallMethodViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public ImageView iv_icon;

        public TextView tv_method;

        public SelectCallMethodViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            iv_icon = itemView.findViewById(R.id.iv_icon);
            tv_method = itemView.findViewById(R.id.tv_method);
        }

        @Override
        public void onClick(View v) {
            if(listener != null) {
                listener.onSelectCallMethodAdapterItemPressed(getLayoutPosition());
            }
        }
    }
}
