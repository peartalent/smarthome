package com.sunasterisk.smarthomejava.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sunasterisk.smarthomejava.R;
import com.sunasterisk.smarthomejava.model.Card;
import com.sunasterisk.smarthomejava.retrofit.INetwork;
import com.sunasterisk.smarthomejava.retrofit.RetrofitRespon;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class AdapterCard extends BaseAdapter{
    private List<Card> listData;
    private LayoutInflater layoutInflater;
    private Context mContext;
    public static Retrofit retrofit;
    public INetwork iNetwork;
    public AdapterCard(List<Card> listData,  Context context) {
        this.listData = listData;
        this.layoutInflater = LayoutInflater.from(context);;
        this.mContext = context;
        retrofit = RetrofitRespon.getInstance().getRetrofit();
        iNetwork = retrofit.create(INetwork.class);
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_card, null);
            holder = new ViewHolder();
            holder.delete = convertView.findViewById(R.id.btnDeleteCard);
            holder.name = convertView.findViewById(R.id.textNameCard);
            holder.edit = convertView.findViewById(R.id.btnEditCard);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Card card = this.listData.get(position);
        holder.name.setText(card.getName());
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iNetwork.deleteCard(card.getId()).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {

                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {

                    }
                });
                listData.remove(position);
                notifyDataSetChanged();
            }
        });
        return convertView;
    }
    static class ViewHolder {
        ImageView delete;
        TextView name;
        ImageView edit;
    }
}
