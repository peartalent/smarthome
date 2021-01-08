package com.sunasterisk.smarthomejava.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sunasterisk.smarthomejava.R;
import com.sunasterisk.smarthomejava.model.Door;
import com.sunasterisk.smarthomejava.unit.FormatTimestamp;

import java.text.SimpleDateFormat;
import java.util.List;

public class AdapterHistory extends RecyclerView.Adapter<AdapterHistory.ViewHolderHisory> {
    private List<Door> doors;
    private int value;
    private Context mContext;

    public AdapterHistory(List doors, Context mContext) {
        this.mContext = mContext;
        this.doors = doors;
    }

    @Override
    public AdapterHistory.ViewHolderHisory onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View view =
                inflater.inflate(R.layout.item_history, parent, false);

        ViewHolderHisory viewHolder = new ViewHolderHisory(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderHisory holder, int position) {
           Door door = doors.get(position);
           holder.textHistory.setText(FormatTimestamp.formatTimestamp(Long.parseLong(door.timeStamp)));
           if(door.nameCard != null){
               if(door.nameCard.length()>0){
                   holder.textHistory.setText(FormatTimestamp.formatTimestamp(Long.parseLong(door.timeStamp)) + "   "+door.nameCard);
               }
           }
           if (door.permission.equals("granted")){
               holder.textHistory.setTextColor(Color.GREEN);
               holder.imageLock.setImageResource(R.drawable.ic_baseline_lock_open_24);
           }
           else {
               holder.imageLock.setImageResource(R.drawable.ic_baseline_lock_24);
               holder.textHistory.setTextColor(Color.RED);
           }
    }


    @Override
    public int getItemCount() {
        return doors.size();
    }

    public class ViewHolderHisory extends RecyclerView.ViewHolder {
        //        private View itemview;
        private TextView textHistory;
        private ImageView imageLock;

        public ViewHolderHisory(View itemView) {
            super(itemView);
            textHistory = itemView.findViewById(R.id.textTime);
            imageLock = itemView.findViewById(R.id.imageLock);
        }
    }
}
