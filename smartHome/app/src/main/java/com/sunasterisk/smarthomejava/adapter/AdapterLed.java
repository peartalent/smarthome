package com.sunasterisk.smarthomejava.adapter;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.sunasterisk.smarthomejava.R;
import com.sunasterisk.smarthomejava.model.Led;
import com.sunasterisk.smarthomejava.retrofit.INetwork;
import com.sunasterisk.smarthomejava.retrofit.RetrofitRespon;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class AdapterLed extends RecyclerView.Adapter<AdapterLed.ViewHolder>implements Callback<Void> {
    //Dữ liệu hiện thị là danh sách sinh viên
    private List<Led> leds;
    private int value;
    // Lưu Context để dễ dàng truy cập
    private Context mContext;
    public static Retrofit retrofit;
    public INetwork iNetwork;

    public AdapterLed(List leds, Context mContext) {
        this.mContext = mContext;
        this.leds = leds;
        retrofit = RetrofitRespon.getInstance().getRetrofit();
        iNetwork = retrofit.create(INetwork.class);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View view =
                inflater.inflate(R.layout.item, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Led led = leds.get(position);

        boolean switchCheck = led.getValue() == 0 ? false : true;
        if (led.getValue() == 0) {
            holder.imageLight.setImageResource(R.drawable.ic_light_off);
            holder.switchLight.setChecked(false);

        } else {
            holder.imageLight.setImageResource(R.drawable.ic_light_on);
            holder.switchLight.setChecked(true);
        }

        holder.switchLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    led.setValue(1);
                    holder.imageLight.setImageResource(R.drawable.ic_light_on);
                    iNetwork.turnLed(led.getId(),led.getValue()).enqueue(AdapterLed.this);
                    CountDownTimer countDownTimer = new CountDownTimer(2000, 20) {
                        @Override
                        public void onTick(long l) {
                            holder.switchLight.setEnabled(false);
                        }

                        @Override
                        public void onFinish() {
                            holder.switchLight.setEnabled(true);
                        }
                    };
                    countDownTimer.start() ;
                } else {
                    led.setValue(0);
                    holder.imageLight.setImageResource(R.drawable.ic_light_off);
                    iNetwork.turnLed(led.getId(),led.getValue()).enqueue(AdapterLed.this);
                    CountDownTimer countDownTimer = new CountDownTimer(2000, 20) {
                        @Override
                        public void onTick(long l) {
                            holder.switchLight.setEnabled(false);
                        }

                        @Override
                        public void onFinish() {
                            holder.switchLight.setEnabled(true);
                        }
                    };
                    countDownTimer.start() ;
                }
                leds.forEach((e) -> {
                    Log.d("tag", e.toString());
                });
//                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return leds.size();
    }

    @Override
    public void onResponse(Call<Void> call, Response<Void> response) {

    }

    @Override
    public void onFailure(Call<Void> call, Throwable t) {

    }

    /**
     * Lớp nắm giữ cấu trúc view
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        //        private View itemview;
        private Switch switchLight;
        private ImageView imageLight;

        public ViewHolder(View itemView) {
            super(itemView);
            imageLight = itemView.findViewById(R.id.imageLight);
            switchLight = itemView.findViewById(R.id.switchLight);
        }
    }
}
