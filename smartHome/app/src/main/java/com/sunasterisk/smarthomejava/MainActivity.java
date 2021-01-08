
package com.sunasterisk.smarthomejava;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sunasterisk.smarthomejava.adapter.AdapterCard;
import com.sunasterisk.smarthomejava.adapter.AdapterHistory;
import com.sunasterisk.smarthomejava.adapter.AdapterLed;
import com.sunasterisk.smarthomejava.model.Air;
import com.sunasterisk.smarthomejava.model.Card;
import com.sunasterisk.smarthomejava.model.Door;
import com.sunasterisk.smarthomejava.model.Led;
import com.sunasterisk.smarthomejava.mqtt.MqttClientConnect;
import com.sunasterisk.smarthomejava.retrofit.INetwork;
import com.sunasterisk.smarthomejava.retrofit.RetrofitRespon;
import com.sunasterisk.smarthomejava.unit.SaveFile;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Url;

import static com.sunasterisk.smarthomejava.config.Config.*;

public class MainActivity extends AppCompatActivity implements Callback<List<Led>> {
    RecyclerView recyclerView;
    RecyclerView recyclerViewHistory;
    RecyclerView.Adapter adapterLed;
    RecyclerView.Adapter adapterHisory;
    int themeIdcurrent;
    List<Entry> entries;
    LineChart lineChart;
    List<Air> airs;
    List<Door> doors;
    List<Led> leds;
    List<Card> cards;
    MqttClientConnect mqttClientConnect;
    MqttAndroidClient client;
    public static Retrofit retrofit;
    public INetwork iNetwork;
    ConstraintLayout constraintLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Đọc ID theme đã lưu, nếu chưa lưu thì dùng R.style.MyAppTheme
        SharedPreferences locationpref = getApplicationContext()
                .getSharedPreferences(FILE_USER, MODE_PRIVATE);
        themeIdcurrent = locationpref.getInt(FILE_MODE_THEME,R.style.AppTheme);
        Log.d("themss",themeIdcurrent+"");
        //Thiết lập theme cho Activity
        setTheme(themeIdcurrent);
        setContentView(R.layout.activity_main);

//        Service broadcast
        Intent mService = new Intent(this, MainService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(mService);
        } else {
            startService(mService);
        }
//        innit retrofit
        retrofit = RetrofitRespon.getInstance().getRetrofit();
        iNetwork = retrofit.create(INetwork.class);
//        innit display
        recyclerView = findViewById(R.id.recyclerViewContains);
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        lineChart = findViewById(R.id.lineChart);
        leds = new ArrayList<Led>();
        airs = new ArrayList<>();
        doors = new ArrayList<>();
        cards = new ArrayList<>();
//        load data
        loadLeds();
        loadHistoryDoor();
        loadAirs();
//        mqtt
        mqttClientConnect = MqttClientConnect.getInstance();
        mqttClientConnect.setContext(this);
        try {
            IMqttToken token = mqttClientConnect.mqttConnect1();
            client = mqttClientConnect.getClient1();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    mqttClientConnect.sub1(TOPIC_LED);

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TOPIC_LED, "Token tắt");
                }
            });
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    JSONObject jsonObject = new JSONObject(message.toString());
                    Log.d(topic, message.toString() + "   dfdfgfd" + jsonObject.getInt("type"));

                    if (jsonObject.getInt("type") == 2) {
                        Air a = new Gson().fromJson(message.toString(), Air.class);
                        airs.add(a);
                        addEntry(new Entry(Float.parseFloat(a.timeStamp), Float.parseFloat(a.value)));
                    } else if (jsonObject.getInt("type") == 3) {
                        doors.add(0, new Gson().fromJson(message.toString(), Door.class));

//                        Collections.reverse(doors);
                        adapterHisory = new AdapterHistory(doors, MainActivity.this);
                        recyclerViewHistory.setAdapter(adapterHisory);
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
                        recyclerViewHistory.setAdapter(adapterHisory);
                        recyclerViewHistory.setLayoutManager(linearLayoutManager);
                    }

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void addEntry(Entry entry) {

        LineData data = lineChart.getLineData();


        // get the dataset where you want to add the entry
        LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);

//            if (set == null) {
//                // create a new DataSet if there is none yet
//                set = createSet();
//                data.addDataSet(set);
//            }

        // add a new x-value first
        data.addEntry(entry, 0);

        // let the chart know it's data has changed
        lineChart.setVisibleXRangeMaximum(19000);
        data.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.moveViewToX(entry.getX());
        lineChart.invalidate();

    }

    private void loadCards() {
        iNetwork.getCards().enqueue(new Callback<List<Card>>() {
            @Override
            public void onResponse(Call<List<Card>> call, Response<List<Card>> response) {
                cards.clear();
                cards.addAll(response.body());
            }

            @Override
            public void onFailure(Call<List<Card>> call, Throwable t) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addCard:
                dialogAddCard();
                break;
            case R.id.changeDarkTheme:
                //Chuyển đổi theme
                themeIdcurrent = themeIdcurrent == R.style.AppTheme ?  R.style.Theme_AppCompat :R.style.AppTheme;

                //Lưu lại theme ID
                SharedPreferences locationpref = getApplicationContext()
                        .getSharedPreferences(FILE_USER, MODE_PRIVATE);
                SharedPreferences.Editor spedit = locationpref.edit();
                spedit.putInt(FILE_MODE_THEME, themeIdcurrent);
                spedit.apply();
                recreate();

                break;
            case R.id.getCards:
                dialogGetCards();
                break;
            case R.id.logout:
                doSaveShared(FILE_USER_TOKEN_SESSION, "false");
                startActivity(new Intent(this, Login.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void doSaveShared(String key, String value) {
        SharedPreferences sharedPreferences = getSharedPreferences(FILE_USER, this.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        // Save.
        editor.apply();
    }

    public void dialogAddCard() {
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.dialog_add_card, null);
        final EditText name = alertLayout.findViewById(R.id.inputNameCard);
        final Button btnAddCard = alertLayout.findViewById(R.id.btn_add_card);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setView(alertLayout);
        alert.setCancelable(true);
        AlertDialog dialog = alert.create();
        btnAddCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (name.getText().toString().length() > 0) {
                    iNetwork.addCard(name.getText().toString().trim()).enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            if (response.body().equals("false")) {
                                Toast.makeText(MainActivity.this, "Thẻ trùng tên!", Toast.LENGTH_SHORT);
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {

                        }
                    });

//                    try {
//                        String uri = "http://192.168.1.160:3000/addRFID?name=" + name.getText().toString().trim();
//                        URL url = new URL(uri);
//                        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
//                        httpConn.setRequestMethod("GET");
//                        httpConn.connect();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void dialogGetCards() {
        loadCards();
//        Gson gson = new Gson();
//        Type airsList = new TypeToken<ArrayList<Card>>() {
//        }.getType();
//        cards = gson.fromJson("[{\"id\":4,\"nameCard\":\"Duong\"},{\"id\":5,\"nameCard\":\"Son\"}]", airsList);
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.dialog_cards, null);
        final ListView listView = alertLayout.findViewById(R.id.listViewCards);
        AdapterCard adapterCard = new AdapterCard(cards, this);
        listView.setAdapter(adapterCard);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setView(alertLayout);
        alert.setTitle("Danh sách thẻ");
        alert.setCancelable(true);
        AlertDialog dialog = alert.create();
        dialog.show();
    }

    public void loadLeds() {
        iNetwork.getLeds().enqueue(this);
    }

    public void loadAirs() {
        iNetwork.getAirs().enqueue(new Callback<List<Air>>() {
            @Override
            public void onResponse(Call<List<Air>> call, Response<List<Air>> response) {
                airs.addAll(response.body());
//                String json = "[{\"time_stamp\":\"311332\",\"value\":11.143695},{\"time_stamp\":\"312332\",\"value\":11.143695},{\"time_stamp\":\"313332\",\"value\":11.143695},{\"time_stamp\":\"314332\",\"value\":11.143695},{\"time_stamp\":\"315332\",\"value\":11.143695},{\"time_stamp\":\"316332\",\"value\":11.143695},{\"time_stamp\":\"317332\",\"value\":11.143695},{\"time_stamp\":\"318332\",\"value\":11.045943},{\"time_stamp\":\"319332\",\"value\":11.045943},{\"time_stamp\":\"320332\",\"value\":11.045943},{\"time_stamp\":\"321332\",\"value\":11.143695},{\"time_stamp\":\"322332\",\"value\":11.143695},{\"time_stamp\":\"323332\",\"value\":11.045943},{\"time_stamp\":\"324332\",\"value\":11.143695},{\"time_stamp\":\"325332\",\"value\":11.045943},{\"time_stamp\":\"326332\",\"value\":11.045943},{\"time_stamp\":\"327332\",\"value\":11.045943},{\"time_stamp\":\"328332\",\"value\":11.045943},{\"time_stamp\":\"329332\",\"value\":11.143695},{\"time_stamp\":\"330332\",\"value\":11.045943},{\"time_stamp\":\"331332\",\"value\":11.045943},{\"time_stamp\":\"332332\",\"value\":11.045943},{\"time_stamp\":\"333332\",\"value\":11.143695},{\"time_stamp\":\"334332\",\"value\":11.045943},{\"time_stamp\":\"335332\",\"value\":11.045943},{\"time_stamp\":\"336332\",\"value\":11.045943},{\"time_stamp\":\"337332\",\"value\":11.045943},{\"time_stamp\":\"338332\",\"value\":11.045943},{\"time_stamp\":\"339332\",\"value\":11.045943},{\"time_stamp\":\"340332\",\"value\":11.045943},{\"time_stamp\":\"341332\",\"value\":11.045943},{\"time_stamp\":\"342332\",\"value\":11.045943},{\"time_stamp\":\"343332\",\"value\":11.045943},{\"time_stamp\":\"344332\",\"value\":11.045943},{\"time_stamp\":\"345332\",\"value\":11.045943},{\"time_stamp\":\"346332\",\"value\":11.045943},{\"time_stamp\":\"347332\",\"value\":11.045943},{\"time_stamp\":\"348332\",\"value\":11.045943},{\"time_stamp\":\"349332\",\"value\":11.045943},{\"time_stamp\":\"350332\",\"value\":11.045943},{\"time_stamp\":\"351332\",\"value\":11.045943},{\"time_stamp\":\"352332\",\"value\":11.045943},{\"time_stamp\":\"353332\",\"value\":11.045943},{\"time_stamp\":\"354332\",\"value\":11.045943},{\"time_stamp\":\"355332\",\"value\":11.045943},{\"time_stamp\":\"356332\",\"value\":11.045943},{\"time_stamp\":\"357332\",\"value\":11.045943},{\"time_stamp\":\"358332\",\"value\":11.045943},{\"time_stamp\":\"359332\",\"value\":11.045943},{\"time_stamp\":\"360332\",\"value\":11.045943},{\"time_stamp\":\"361332\",\"value\":10.948192},{\"time_stamp\":\"362332\",\"value\":11.045943},{\"time_stamp\":\"363332\",\"value\":11.045943},{\"time_stamp\":\"364332\",\"value\":10.948192},{\"time_stamp\":\"365332\",\"value\":10.948192},{\"time_stamp\":\"366332\",\"value\":10.948192},{\"time_stamp\":\"367332\",\"value\":10.948192},{\"time_stamp\":\"368332\",\"value\":10.948192},{\"time_stamp\":\"369332\",\"value\":10.948192},{\"time_stamp\":\"370332\",\"value\":10.85044},{\"time_stamp\":\"371332\",\"value\":10.948192},{\"time_stamp\":\"372332\",\"value\":10.948192},{\"time_stamp\":\"373332\",\"value\":10.948192},{\"time_stamp\":\"374332\",\"value\":10.948192},{\"time_stamp\":\"375332\",\"value\":10.948192},{\"time_stamp\":\"376332\",\"value\":10.948192},{\"time_stamp\":\"377332\",\"value\":10.948192},{\"time_stamp\":\"378332\",\"value\":10.948192},{\"time_stamp\":\"379332\",\"value\":10.948192},{\"time_stamp\":\"380332\",\"value\":10.948192},{\"time_stamp\":\"381332\",\"value\":10.948192},{\"time_stamp\":\"382332\",\"value\":10.948192},{\"time_stamp\":\"383332\",\"value\":10.948192},{\"time_stamp\":\"384332\",\"value\":10.948192},{\"time_stamp\":\"385332\",\"value\":10.948192},{\"time_stamp\":\"386332\",\"value\":10.948192},{\"time_stamp\":\"387332\",\"value\":11.045943},{\"time_stamp\":\"388332\",\"value\":11.045943},{\"time_stamp\":\"389332\",\"value\":11.045943},{\"time_stamp\":\"390332\",\"value\":11.045943},{\"time_stamp\":\"391332\",\"value\":10.948192},{\"time_stamp\":\"392332\",\"value\":11.045943},{\"time_stamp\":\"393332\",\"value\":10.85044},{\"time_stamp\":\"394332\",\"value\":10.85044},{\"time_stamp\":\"395332\",\"value\":10.752688},{\"time_stamp\":\"396332\",\"value\":10.752688},{\"time_stamp\":\"397332\",\"value\":10.752688},{\"time_stamp\":\"398332\",\"value\":10.752688},{\"time_stamp\":\"399332\",\"value\":10.85044},{\"time_stamp\":\"400332\",\"value\":10.85044},{\"time_stamp\":\"401332\",\"value\":10.85044},{\"time_stamp\":\"402332\",\"value\":10.85044},{\"time_stamp\":\"403332\",\"value\":10.948192},{\"time_stamp\":\"404332\",\"value\":10.948192},{\"time_stamp\":\"405332\",\"value\":11.045943},{\"time_stamp\":\"406332\",\"value\":11.045943},{\"time_stamp\":\"407332\",\"value\":10.948192},{\"time_stamp\":\"408332\",\"value\":10.752688},{\"time_stamp\":\"409332\",\"value\":10.752688},{\"time_stamp\":\"410332\",\"value\":10.85044},{\"time_stamp\":\"411332\",\"value\":10.85044},{\"time_stamp\":\"412332\",\"value\":10.948192},{\"time_stamp\":\"413332\",\"value\":11.045943},{\"time_stamp\":\"414332\",\"value\":10.948192},{\"time_stamp\":\"415332\",\"value\":10.752688},{\"time_stamp\":\"416332\",\"value\":10.752688},{\"time_stamp\":\"417332\",\"value\":10.85044},{\"time_stamp\":\"418332\",\"value\":10.85044},{\"time_stamp\":\"419332\",\"value\":11.045943},{\"time_stamp\":\"420332\",\"value\":10.948192},{\"time_stamp\":\"421332\",\"value\":10.752688},{\"time_stamp\":\"422332\",\"value\":10.752688},{\"time_stamp\":\"423332\",\"value\":10.85044},{\"time_stamp\":\"424332\",\"value\":10.85044},{\"time_stamp\":\"425332\",\"value\":10.85044},{\"time_stamp\":\"426332\",\"value\":11.045943},{\"time_stamp\":\"427332\",\"value\":11.045943},{\"time_stamp\":\"428332\",\"value\":10.85044},{\"time_stamp\":\"429332\",\"value\":10.752688},{\"time_stamp\":\"430332\",\"value\":10.752688},{\"time_stamp\":\"431332\",\"value\":10.85044},{\"time_stamp\":\"432332\",\"value\":10.752688},{\"time_stamp\":\"433332\",\"value\":10.752688},{\"time_stamp\":\"434332\",\"value\":10.85044},{\"time_stamp\":\"435332\",\"value\":10.948192},{\"time_stamp\":\"436332\",\"value\":10.948192},{\"time_stamp\":\"437332\",\"value\":10.85044},{\"time_stamp\":\"438332\",\"value\":10.752688},{\"time_stamp\":\"439332\",\"value\":10.752688},{\"time_stamp\":\"440332\",\"value\":10.752688},{\"time_stamp\":\"441332\",\"value\":10.85044},{\"time_stamp\":\"442332\",\"value\":10.752688},{\"time_stamp\":\"443332\",\"value\":10.948192},{\"time_stamp\":\"444332\",\"value\":11.045943},{\"time_stamp\":\"445332\",\"value\":10.85044},{\"time_stamp\":\"446332\",\"value\":10.752688},{\"time_stamp\":\"447332\",\"value\":10.752688},{\"time_stamp\":\"448332\",\"value\":10.752688},{\"time_stamp\":\"449332\",\"value\":10.752688},{\"time_stamp\":\"450332\",\"value\":10.752688},{\"time_stamp\":\"451332\",\"value\":10.948192},{\"time_stamp\":\"452332\",\"value\":10.948192},{\"time_stamp\":\"453332\",\"value\":10.654937},{\"time_stamp\":\"454332\",\"value\":10.752688},{\"time_stamp\":\"455332\",\"value\":10.85044},{\"time_stamp\":\"456332\",\"value\":10.752688},{\"time_stamp\":\"457332\",\"value\":10.752688},{\"time_stamp\":\"458332\",\"value\":10.948192},{\"time_stamp\":\"459332\",\"value\":10.85044},{\"time_stamp\":\"460332\",\"value\":10.752688},{\"time_stamp\":\"461332\",\"value\":10.752688},{\"time_stamp\":\"462332\",\"value\":10.752688},{\"time_stamp\":\"463332\",\"value\":10.752688},{\"time_stamp\":\"464332\",\"value\":10.85044},{\"time_stamp\":\"465332\",\"value\":10.948192},{\"time_stamp\":\"466332\",\"value\":10.948192},{\"time_stamp\":\"467332\",\"value\":10.948192},{\"time_stamp\":\"468332\",\"value\":10.948192},{\"time_stamp\":\"469332\",\"value\":10.752688},{\"time_stamp\":\"470332\",\"value\":10.752688},{\"time_stamp\":\"471332\",\"value\":10.752688},{\"time_stamp\":\"472332\",\"value\":10.85044},{\"time_stamp\":\"473332\",\"value\":10.85044},{\"time_stamp\":\"474332\",\"value\":10.85044},{\"time_stamp\":\"475332\",\"value\":11.045943},{\"time_stamp\":\"476332\",\"value\":11.045943},{\"time_stamp\":\"477332\",\"value\":10.948192},{\"time_stamp\":\"478332\",\"value\":10.85044},{\"time_stamp\":\"479332\",\"value\":10.752688},{\"time_stamp\":\"480332\",\"value\":10.752688},{\"time_stamp\":\"481332\",\"value\":10.752688},{\"time_stamp\":\"482332\",\"value\":10.85044},{\"time_stamp\":\"483332\",\"value\":10.85044},{\"time_stamp\":\"484332\",\"value\":10.85044},{\"time_stamp\":\"485332\",\"value\":10.948192},{\"time_stamp\":\"486332\",\"value\":10.948192},{\"time_stamp\":\"487332\",\"value\":10.948192},{\"time_stamp\":\"488332\",\"value\":10.85044},{\"time_stamp\":\"489332\",\"value\":10.752688},{\"time_stamp\":\"490332\",\"value\":10.654937},{\"time_stamp\":\"491332\",\"value\":10.752688},{\"time_stamp\":\"492332\",\"value\":10.752688},{\"time_stamp\":\"493332\",\"value\":10.752688},{\"time_stamp\":\"494332\",\"value\":10.752688},{\"time_stamp\":\"495332\",\"value\":10.752688},{\"time_stamp\":\"496332\",\"value\":10.948192},{\"time_stamp\":\"497332\",\"value\":10.948192},{\"time_stamp\":\"498332\",\"value\":10.948192},{\"time_stamp\":\"499332\",\"value\":10.948192},{\"time_stamp\":\"500332\",\"value\":10.752688},{\"time_stamp\":\"501332\",\"value\":10.752688},{\"time_stamp\":\"502332\",\"value\":10.752688},{\"time_stamp\":\"503332\",\"value\":10.752688},{\"time_stamp\":\"504332\",\"value\":10.752688},{\"time_stamp\":\"505332\",\"value\":10.752688},{\"time_stamp\":\"506332\",\"value\":10.752688},{\"time_stamp\":\"507332\",\"value\":10.752688},{\"time_stamp\":\"508332\",\"value\":10.85044},{\"time_stamp\":\"509332\",\"value\":10.752688},{\"time_stamp\":\"510332\",\"value\":10.85044},{\"time_stamp\":\"511332\",\"value\":10.85044},{\"time_stamp\":\"512332\",\"value\":10.948192},{\"time_stamp\":\"513332\",\"value\":11.045943},{\"time_stamp\":\"514332\",\"value\":11.045943},{\"time_stamp\":\"515332\",\"value\":11.045943},{\"time_stamp\":\"516332\",\"value\":10.948192},{\"time_stamp\":\"517332\",\"value\":11.045943},{\"time_stamp\":\"518332\",\"value\":11.045943},{\"time_stamp\":\"519332\",\"value\":11.045943},{\"time_stamp\":\"520332\",\"value\":11.045943},{\"time_stamp\":\"521332\",\"value\":11.045943},{\"time_stamp\":\"522332\",\"value\":11.045943},{\"time_stamp\":\"523332\",\"value\":11.045943},{\"time_stamp\":\"524332\",\"value\":11.045943},{\"time_stamp\":\"525332\",\"value\":11.045943},{\"time_stamp\":\"526332\",\"value\":11.045943},{\"time_stamp\":\"527332\",\"value\":11.045943},{\"time_stamp\":\"528332\",\"value\":11.045943},{\"time_stamp\":\"529332\",\"value\":11.045943},{\"time_stamp\":\"530332\",\"value\":11.143695},{\"time_stamp\":\"531332\",\"value\":11.045943},{\"time_stamp\":\"532332\",\"value\":11.045943},{\"time_stamp\":\"533332\",\"value\":11.143695},{\"time_stamp\":\"534332\",\"value\":11.045943},{\"time_stamp\":\"535332\",\"value\":11.045943},{\"time_stamp\":\"536332\",\"value\":11.045943},{\"time_stamp\":\"537332\",\"value\":11.045943},{\"time_stamp\":\"538332\",\"value\":11.045943},{\"time_stamp\":\"539332\",\"value\":11.045943},{\"time_stamp\":\"540332\",\"value\":11.045943},{\"time_stamp\":\"541332\",\"value\":11.045943},{\"time_stamp\":\"542332\",\"value\":11.045943},{\"time_stamp\":\"543332\",\"value\":11.045943},{\"time_stamp\":\"544332\",\"value\":11.045943},{\"time_stamp\":\"545332\",\"value\":11.045943},{\"time_stamp\":\"546332\",\"value\":11.045943},{\"time_stamp\":\"547332\",\"value\":11.045943},{\"time_stamp\":\"548332\",\"value\":11.045943},{\"time_stamp\":\"549332\",\"value\":11.045943},{\"time_stamp\":\"550332\",\"value\":11.045943},{\"time_stamp\":\"551332\",\"value\":11.045943},{\"time_stamp\":\"552332\",\"value\":11.045943},{\"time_stamp\":\"553332\",\"value\":11.045943},{\"time_stamp\":\"554332\",\"value\":10.361681},{\"time_stamp\":\"555332\",\"value\":10.948192},{\"time_stamp\":\"556332\",\"value\":10.948192},{\"time_stamp\":\"557332\",\"value\":10.948192},{\"time_stamp\":\"558332\",\"value\":11.143695},{\"time_stamp\":\"559332\",\"value\":10.654937},{\"time_stamp\":\"560332\",\"value\":0},{\"time_stamp\":\"561332\",\"value\":11.045943},{\"time_stamp\":\"562332\",\"value\":11.045943},{\"time_stamp\":\"563332\",\"value\":10.948192},{\"time_stamp\":\"564332\",\"value\":17.008799},{\"time_stamp\":\"565332\",\"value\":11.339199},{\"time_stamp\":\"566332\",\"value\":10.948192},{\"time_stamp\":\"567332\",\"value\":10.85044},{\"time_stamp\":\"568332\",\"value\":10.654937},{\"time_stamp\":\"569332\",\"value\":0},{\"time_stamp\":\"570332\",\"value\":10.654937},{\"time_stamp\":\"571332\",\"value\":10.654937},{\"time_stamp\":\"572332\",\"value\":10.752688},{\"time_stamp\":\"573332\",\"value\":10.752688},{\"time_stamp\":\"574332\",\"value\":10.85044},{\"time_stamp\":\"575332\",\"value\":10.752688},{\"time_stamp\":\"576332\",\"value\":10.85044},{\"time_stamp\":\"577332\",\"value\":10.85044},{\"time_stamp\":\"578332\",\"value\":10.948192},{\"time_stamp\":\"579332\",\"value\":10.752688},{\"time_stamp\":\"580332\",\"value\":10.85044},{\"time_stamp\":\"581332\",\"value\":10.948192},{\"time_stamp\":\"582332\",\"value\":10.948192},{\"time_stamp\":\"583332\",\"value\":10.85044},{\"time_stamp\":\"584332\",\"value\":11.045943},{\"time_stamp\":\"585332\",\"value\":11.045943},{\"time_stamp\":\"586332\",\"value\":11.045943},{\"time_stamp\":\"587332\",\"value\":11.045943},{\"time_stamp\":\"588332\",\"value\":11.045943},{\"time_stamp\":\"589332\",\"value\":11.045943},{\"time_stamp\":\"590332\",\"value\":11.045943},{\"time_stamp\":\"591332\",\"value\":10.948192},{\"time_stamp\":\"592332\",\"value\":10.948192},{\"time_stamp\":\"593332\",\"value\":10.948192},{\"time_stamp\":\"594332\",\"value\":10.948192},{\"time_stamp\":\"595332\",\"value\":10.948192},{\"time_stamp\":\"596332\",\"value\":10.85044},{\"time_stamp\":\"597332\",\"value\":10.85044},{\"time_stamp\":\"598332\",\"value\":10.85044},{\"time_stamp\":\"599332\",\"value\":10.85044},{\"time_stamp\":\"600332\",\"value\":10.85044},{\"time_stamp\":\"601332\",\"value\":10.85044},{\"time_stamp\":\"602332\",\"value\":10.85044},{\"time_stamp\":\"603332\",\"value\":10.948192},{\"time_stamp\":\"604332\",\"value\":10.948192},{\"time_stamp\":\"605332\",\"value\":10.948192},{\"time_stamp\":\"606332\",\"value\":10.948192},{\"time_stamp\":\"607332\",\"value\":10.85044},{\"time_stamp\":\"608332\",\"value\":10.85044},{\"time_stamp\":\"609332\",\"value\":10.948192},{\"time_stamp\":\"610332\",\"value\":10.948192},{\"time_stamp\":\"611332\",\"value\":10.948192},{\"time_stamp\":\"612332\",\"value\":10.85044},{\"time_stamp\":\"613332\",\"value\":10.85044},{\"time_stamp\":\"614332\",\"value\":10.948192},{\"time_stamp\":\"615332\",\"value\":10.85044},{\"time_stamp\":\"616332\",\"value\":10.948192},{\"time_stamp\":\"617332\",\"value\":10.85044},{\"time_stamp\":\"618332\",\"value\":10.948192},{\"time_stamp\":\"619332\",\"value\":10.948192},{\"time_stamp\":\"620332\",\"value\":10.85044},{\"time_stamp\":\"621332\",\"value\":10.948192},{\"time_stamp\":\"622332\",\"value\":10.948192},{\"time_stamp\":\"623332\",\"value\":10.948192},{\"time_stamp\":\"624332\",\"value\":10.85044},{\"time_stamp\":\"625332\",\"value\":10.948192},{\"time_stamp\":\"626332\",\"value\":10.948192},{\"time_stamp\":\"627332\",\"value\":10.948192},{\"time_stamp\":\"628332\",\"value\":10.948192},{\"time_stamp\":\"629332\",\"value\":10.948192},{\"time_stamp\":\"630332\",\"value\":10.948192},{\"time_stamp\":\"631332\",\"value\":10.948192},{\"time_stamp\":\"632332\",\"value\":10.948192},{\"time_stamp\":\"633332\",\"value\":10.948192},{\"time_stamp\":\"634332\",\"value\":10.948192},{\"time_stamp\":\"635332\",\"value\":10.948192},{\"time_stamp\":\"636332\",\"value\":10.948192},{\"time_stamp\":\"637332\",\"value\":10.948192},{\"time_stamp\":\"638332\",\"value\":10.948192},{\"time_stamp\":\"639332\",\"value\":10.85044},{\"time_stamp\":\"640332\",\"value\":10.948192},{\"time_stamp\":\"641332\",\"value\":10.948192},{\"time_stamp\":\"642332\",\"value\":10.948192},{\"time_stamp\":\"643332\",\"value\":10.948192},{\"time_stamp\":\"644332\",\"value\":10.948192},{\"time_stamp\":\"645332\",\"value\":10.948192},{\"time_stamp\":\"646332\",\"value\":10.948192},{\"time_stamp\":\"647332\",\"value\":10.948192},{\"time_stamp\":\"648332\",\"value\":10.948192},{\"time_stamp\":\"649332\",\"value\":10.948192},{\"time_stamp\":\"650332\",\"value\":10.948192},{\"time_stamp\":\"651332\",\"value\":10.948192},{\"time_stamp\":\"652332\",\"value\":10.948192},{\"time_stamp\":\"653332\",\"value\":10.948192},{\"time_stamp\":\"654332\",\"value\":10.948192},{\"time_stamp\":\"655332\",\"value\":10.948192}]";
//                Gson gson = new Gson();
//                Type airsList = new TypeToken<ArrayList<Air>>() {
//                }.getType();
//                airs = gson.fromJson(json, airsList);
                entries = new ArrayList<Entry>();
                airs.forEach((a) -> {
                    entries.add(new Entry(Float.parseFloat(a.timeStamp), Float.parseFloat(a.value)));

                });
                XAxis xAxis = lineChart.getXAxis();
                xAxis.setLabelCount(20);
                YAxis yAxisLeft = lineChart.getAxisLeft();
                yAxisLeft.setLabelCount(10);
                YAxis yAxisRight = lineChart.getAxisRight();
                yAxisRight.setLabelCount(10);
                xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return "";
                    }
                });

//                lineChart.setVisibleXRangeMaximum(0);
                LineDataSet dataSet = new LineDataSet(entries, "Không khí");
//                dataSet.setHighLightColor(R.color.colorPrimary);
                dataSet.setColor(Color.RED);
//                dataSet.setColor(Color.RED);
                dataSet.setDrawCircles(false);
                ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(dataSet);
                LineData data = new LineData(dataSets);
                lineChart.setData(data);
                lineChart.setMaxVisibleValueCount(0);
                lineChart.setNoDataText("Chưa có dữ liệu gửi về");
                lineChart.setVisibleXRangeMaximum(19000);
                data.notifyDataChanged();
                if (entries.size() > 0) {
                    lineChart.moveViewToX(entries.get(entries.size() - 1).getX());
                    lineChart.invalidate();
                }
            }

            @Override
            public void onFailure(Call<List<Air>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Danh sách rỗng", Toast.LENGTH_SHORT);
            }
        });
    }

    public void loadHistoryDoor() {
        iNetwork.getHistory().enqueue(new Callback<List<Door>>() {
            @Override
            public void onResponse(Call<List<Door>> call, Response<List<Door>> response) {
                doors.addAll(response.body());
                Collections.reverse(doors);
                adapterHisory = new AdapterHistory(doors, MainActivity.this);
                recyclerViewHistory.setAdapter(adapterHisory);
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(MainActivity.this);
                recyclerViewHistory.setAdapter(adapterHisory);
                recyclerViewHistory.setLayoutManager(linearLayoutManager);
            }

            @Override
            public void onFailure(Call<List<Door>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lịch sử rỗng", Toast.LENGTH_SHORT);
            }
        });
    }

    @Override
    public void onResponse(Call<List<Led>> call, Response<List<Led>> response) {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        leds.addAll(response.body());
        adapterLed = new AdapterLed(leds, this);
        recyclerView.setAdapter(adapterLed);
        recyclerView.setLayoutManager(gridLayoutManager);
    }

    @Override
    public void onFailure(Call<List<Led>> call, Throwable t) {
        Toast.makeText(MainActivity.this, "Danh sách rỗng", Toast.LENGTH_SHORT);
    }

    public void sortArrayAirs() {
        Collections.sort(airs, new Comparator<Air>() {
            @Override
            public int compare(Air air, Air t1) {
                if (Integer.parseInt(t1.timeStamp) < Integer.parseInt(air.timeStamp)) {
                    return 1;
                } else {
                    if (Integer.parseInt(t1.timeStamp) == Integer.parseInt(air.timeStamp)) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
            }
        });

    }
}

