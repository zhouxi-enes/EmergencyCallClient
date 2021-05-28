package org.enes.lanvideocall.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.enes.lanvideocall.R;
import org.enes.lanvideocall.activities.CallActivity;

public class CallButtonAdapter extends RecyclerView.
        Adapter<CallButtonAdapter.CallButtonAdapterViewHolder> {

    public interface CallButtonAdapterListener {

        void onButtonPress(int position);
    }

    private CallButtonAdapterListener listener;

    public void setListener(CallButtonAdapterListener new_) {
        listener = new_;
    }

    private CallActivity activity;

    private RecyclerView recyclerView;

    private int show_type;

    public CallButtonAdapter(CallActivity activity,RecyclerView recyclerView) {
        super();
        this.activity = activity;
        this.recyclerView = recyclerView;
        init();
    }

    private void init() {
        show_type = CallActivity.METHOD_AUDIO;
    }

    public void setMethodType(int new_type) {
        if(new_type != show_type) {
            show_type = new_type;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public CallButtonAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.view_call_button_item,parent,false);
        return new CallButtonAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CallButtonAdapterViewHolder holder, int position) {
        ImageView iv_icon = holder.iv_icon;
        TextView textView = holder.tv_function;
        if(show_type == CallActivity.METHOD_AUDIO) {
            if(position == 0) {
                if(activity.isUseSpeaker()) {
                    iv_icon.setImageResource(R.drawable.ic_sound_from_phone_black_24dp);
                    textView.setText(recyclerView.getContext().getString(R.string.txt_phone));
                }else {
                    iv_icon.setImageResource(R.drawable.ic_sound_from_speaker_black_24dp);
                    textView.setText(recyclerView.getContext().getString(R.string.txt_speaker));
                }
            }else if(position == 1) {
                if(activity.isSilent()) {
                    iv_icon.setImageResource(R.drawable.ic_mic_on_black_24dp);
                    textView.setText(recyclerView.getContext().getString(R.string.txt_un_mute));
                }else {
                    iv_icon.setImageResource(R.drawable.ic_mic_off_black_24dp);
                    textView.setText(recyclerView.getContext().getString(R.string.txt_mute));
                }
            }
        }else if(show_type == CallActivity.METHOD_VIDEO) {
            if(position == 0) {
                if(activity.isCameraOn()) {
                    iv_icon.setImageResource(R.drawable.ic_videocam_off_black_24dp);
                    textView.setText(recyclerView.getContext().getString(R.string.txt_camera_off));
                }else {
                    iv_icon.setImageResource(R.drawable.ic_videocam_open_black_24dp);
                    textView.setText(recyclerView.getContext().getString(R.string.txt_camera_on));
                }
            }else if(position == 1) {
                if(activity.isFrontCamera()) {
                    iv_icon.setImageResource(R.drawable.ic_camera_front_black_24dp);
                    textView.setText(recyclerView.getContext().getString(R.string.txt_turn_to_front));
                }else {
                    iv_icon.setImageResource(R.drawable.ic_camera_rear_black_24dp);
                    textView.setText(recyclerView.getContext().getString(R.string.txt_turn_to_rear));
                }
            }else if(position == 2) {
                if(activity.isSilent()) {
                    iv_icon.setImageResource(R.drawable.ic_mic_on_black_24dp);
                    textView.setText(recyclerView.getContext().getString(R.string.txt_un_mute));
                }else {
                    iv_icon.setImageResource(R.drawable.ic_mic_off_black_24dp);
                    textView.setText(recyclerView.getContext().getString(R.string.txt_mute));
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        int count = 0;
        if(show_type == CallActivity.METHOD_AUDIO) {
            count = 2;
        }else if(show_type == CallActivity.METHOD_VIDEO) {
            count = 3;
        }
        return count;
    }

    class CallButtonAdapterViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        public LinearLayout ll_main;

        public ImageView iv_icon;

        public TextView tv_function;

        public CallButtonAdapterViewHolder(@NonNull View itemView) {
            super(itemView);
            ll_main = itemView.findViewById(R.id.ll_main);
            ll_main.setOnClickListener(this);
            iv_icon = itemView.findViewById(R.id.iv_icon);
            tv_function = itemView.findViewById(R.id.tv_function);
        }

        @Override
        public void onClick(View v) {
            if(listener != null) {
                listener.onButtonPress(getLayoutPosition());
            }
        }
    }

}
