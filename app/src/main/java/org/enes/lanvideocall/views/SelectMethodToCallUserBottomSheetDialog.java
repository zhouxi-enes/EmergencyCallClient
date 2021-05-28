package org.enes.lanvideocall.views;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.enes.lanvideocall.R;
import org.enes.lanvideocall.adapters.SelectCallMethodAdapter;

public class SelectMethodToCallUserBottomSheetDialog extends BottomSheetDialogFragment implements
        SelectCallMethodAdapter.SelectCallMethodAdapterSelectedListener {

    public interface SelectMethodToCallUserBottomSheetDialogListener {

        void onSelectMethodToCallUserBottomSheetDialogPressed(int position);

    }

    private SelectMethodToCallUserBottomSheetDialogListener listener;

    public void setSelectMethodToCallUserBottomSheetDialogListener(
            SelectMethodToCallUserBottomSheetDialogListener new_listener) {
        listener = new_listener;
    }

    private TextView tv_title;

    private RecyclerView recycler_view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view =inflater.inflate(R.layout.view_select_call_method_to_user,
                container,false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tv_title = view.findViewById(R.id.tv_title);
        if(title != null) {
            tv_title.setText(title);
        }
        recycler_view = view.findViewById(R.id.recycler_view);
        recycler_view.setLayoutManager(new LinearLayoutManager(recycler_view.getContext()));
        SelectCallMethodAdapter selectCallMethodAdapter = new SelectCallMethodAdapter();
        selectCallMethodAdapter.setSelectCallMethodAdapterSelectedListener(this);
        recycler_view.setAdapter(selectCallMethodAdapter);

    }

    private String title;

    public void setTitle(String string) {
        title = string;
        if(tv_title != null) {
            tv_title.setText(title);
        }
    }

    @Override
    public void onSelectCallMethodAdapterItemPressed(int position) {
        if(listener != null) {
            listener.onSelectMethodToCallUserBottomSheetDialogPressed(position);
        }
        dismiss();
    }
}
