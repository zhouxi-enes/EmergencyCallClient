package org.enes.lanvideocall.adapters;

import android.app.Activity;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.github.abdularis.civ.AvatarImageView;

import org.enes.lanvideocall.R;
import org.enes.lanvideocall.pojos.User;
import org.enes.lanvideocall.utils.UserListUtil;

import java.util.ArrayList;
import java.util.List;

public class UserListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface UserListAdapterListener {

        void onUserListAdapterListenerPress(UserListAdapter adapter, int position);

    }

    private UserListAdapterListener listener;

    public void setUserListAdapterListener(UserListAdapterListener newListener) {
        this.listener = newListener;
    }

    private int ITEM_TYPE_USER = 0;

    private int ITEM_TYPE_USER_GROUP_NAME = 1;

    public List<Integer> colorList;

    private Activity activity;

    public UserListAdapter(Activity activity) {
        super();
        this.activity = activity;
        getColorList();
    }

    private void getColorList() {
        colorList = new ArrayList<>();
        Resources resources = activity.getResources();
        int[] colors_id = {
                R.color.color_cycle_1,
                R.color.color_cycle_2,
                R.color.color_cycle_3,
                R.color.color_cycle_4,
                R.color.color_cycle_5,
                R.color.color_cycle_6,
                R.color.color_cycle_7,
                R.color.color_cycle_8
        };
        for(int i = 0 ; i < colors_id.length ; i ++) {
           int color = resources.getColor(colors_id[i],activity.getTheme());
            colorList.add(color);
        }
    }

    public void refresh() {
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        if(viewType == ITEM_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.adapter_user_list,parent,false);
            viewHolder = new UserViewHolder(view);
        }else if(viewType == ITEM_TYPE_USER_GROUP_NAME) {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.view_user_group_title_name,parent,false);
            viewHolder = new UserGroupTitleViewHolder(view);
        }
        return viewHolder;
    }

    private static final long _5_min_millis = 5 * 60 * 1000;

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof UserGroupTitleViewHolder) {
//            UserGroupTitleViewHolder viewHolder = (UserGroupTitleViewHolder) holder;
//            if(position == 0) {
//
//
//            }else if(position == 2) {
//
//            }
        }else if (holder instanceof UserViewHolder) {
            UserViewHolder viewHolder = (UserViewHolder) holder;
            User user = UserListUtil.getInstance().getUsers().get(position);
            String name = user.name;
            viewHolder.avatar_image_view.setText(String.format("%1s",name));
            viewHolder.tv_name.setText(name);
            // change color
            int color_pos = position % colorList.size();
            viewHolder.avatar_image_view.setAvatarBackgroundColor(colorList.get(color_pos));
            //
            if(user.is_online) {
                viewHolder.tv_is_online.setText(activity.getResources().
                        getString(R.string.txt_online));
                viewHolder.tv_is_online.setTextColor(activity.getColor(R.color.color_online));
            }else {
                viewHolder.tv_is_online.setText(activity.getResources().
                        getString(R.string.txt_offline));
                viewHolder.tv_is_online.setTextColor(activity.getColor(R.color.color_offline));
            }
//            if(position == 1) {
//
//            }else {
//                int truePosition = position + 2;
//                User user = UserListUtil.getInstance().getUsers().get(truePosition);
//            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        int type = ITEM_TYPE_USER;
//        if(position == 0 || position == 2) {
//            type = ITEM_TYPE_USER_GROUP_NAME;
//        }
        return type;
    }

    @Override
    public int getItemCount() {
        int user_size = UserListUtil.getInstance().getUsers().size();
//        user_size += 2;
        return user_size;
    }

    class UserViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public CardView card_view;

        public LinearLayout ll_main;

        public AvatarImageView avatar_image_view;

        public TextView tv_name;

        public TextView tv_is_online;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            card_view = itemView.findViewById(R.id.card_view);
            ll_main = itemView.findViewById(R.id.ll_main);
            ll_main.setOnClickListener(this);
            avatar_image_view = itemView.findViewById(R.id.avatar_image_view);
            avatar_image_view.setOnClickListener(this);
            tv_name = itemView.findViewById(R.id.tv_name);
            tv_is_online = itemView.findViewById(R.id.tv_is_online);
        }

        @Override
        public void onClick(View v) {
            if(listener != null) {
                listener.onUserListAdapterListenerPress(UserListAdapter.this,
                        getLayoutPosition());
            }
        }
    }

    class UserGroupTitleViewHolder extends RecyclerView.ViewHolder {

        public UserGroupTitleViewHolder(@NonNull View itemView) {
            super(itemView);
        }

    }

}
