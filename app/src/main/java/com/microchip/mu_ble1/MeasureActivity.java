package com.microchip.mu_ble1;

import static android.content.ContentValues.TAG;
import static com.microchip.mu_ble1.BleMainActivity.bleGlobalAddress;
import static com.microchip.mu_ble1.BleMainActivity.bleService;
import static com.microchip.mu_ble1.MeasureActivity.et_save_time;
import static com.microchip.mu_ble1.MeasureActivity.max_loop;
import static com.microchip.mu_ble1.MeasureActivity.min_loop;
import static com.microchip.mu_ble1.MeasureActivity.t2;
import static com.microchip.mu_ble1.MeasureActivity.t3;
import static com.microchip.mu_ble1.MeasureActivity.t4;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.icu.util.Measure;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.common.util.Hex;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class MeasureActivity extends AppCompatActivity {
    private DatabaseReference databaseReference;
    private FirebaseDatabase database;
    private static LineChart chart_iv;
    public static TextView vds_tv_2, gain_tv_2, duty_tv_2, stm_tv_2, test_tv_2;
    public static int interval_n, d_num = 0, min_d = 0, measure_n;
    public static String[] arr_rcv;
    public static String vds="0", gain="0", duty="0", stm="0", test="0";
    public static String G_BLE_Connection = "0";
    static TextView tv_ble;
    static EditText et_save_time;
    public static ArrayList<Entry> val_iv  = new ArrayList<>();
    static long t1, t2, t3, t4, t5, t6;
    static float min_loop, max_loop;
    static int start_time;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure);

        Thread thread = new continuous_req();

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("User");

        vds_tv_2   = findViewById(R.id.vds_tv2);
        gain_tv_2  = findViewById(R.id.gain_tv2);
        duty_tv_2  = findViewById(R.id.duty_tv2);
        stm_tv_2   = findViewById(R.id.stm_tv2);
        test_tv_2  = findViewById(R.id.test_tv2);

        // Graph View
        chart_iv  = (LineChart) findViewById(R.id.graph_iv);

        chart_iv.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart_iv.getAxisRight().setEnabled(false);
        chart_iv.animateXY(2000, 2000);
        chart_iv.invalidate();

        LineData data_iv = new LineData();
        chart_iv.setData(data_iv);
        XAxis xAxis = chart_iv.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);


        LineDataSet lineDataSet_iv = new LineDataSet(val_iv, "data1");
        lineDataSet_iv.setColor(Color.BLUE);
        lineDataSet_iv.setDrawCircles(false);

        tv_ble = findViewById(R.id.ble_status);
        et_save_time = findViewById(R.id.save_time);

        Button bt_setting = findViewById(R.id.set_btn);
        bt_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                set_activity();
            }
        });

        Button bt_presetting = findViewById(R.id.set_btn);
        bt_presetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                set_activity();
            }
        });

        Button btg_ = findViewById(R.id.btg);
        Button btt_ = findViewById(R.id.btt);
        btg_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sendData = "g";
                arr_rcv = new String[300];
                measure_n = 1;
                bleService.writeToTransparentUART(sendData.getBytes());
            }
        });

        btt_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sendData = "t";
                bleService.writeToTransparentUART(sendData.getBytes());
            }
        });
        // @@@ button functions @@@
        Button bt_start = findViewById(R.id.start);
        bt_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //@@
                thread.start();
            }
        });

        Button bt_stop = findViewById(R.id.stop);
        bt_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thread.interrupt();
            }
        });

        Button clear_ = findViewById(R.id.clear);
        clear_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Clear", "CLICKED");
                Arrays.fill(arr_rcv, "0");

                chart_iv.invalidate();
                chart_iv.clear();
            }
        });

        EditText et_save_ = findViewById(R.id.et_save);
        Button bt_save_   = findViewById(R.id.bt_save);
        bt_save_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    writeFile(String.valueOf(et_save_.getText()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Toast mToast = Toast.makeText(getApplicationContext(), "Successfully saved '"+et_save_.getText()+"'", Toast.LENGTH_SHORT);
                et_save_.setText(null);
                mToast.show();
            }
        });
        update_setting();
        Button reconn_ = findViewById(R.id.btn_reconn);
        reconn_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GConnAddress(bleGlobalAddress);
                Log.d("BLE", "trying reconnect...!");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        try {
            switch (item.getItemId()) {
                case R.id.menu_scan:
                case R.id.menu_disconnect: {                                                              //Menu option Scan chosen
                    //scan_int();
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "22Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
        return super.onOptionsItemSelected(item);
    }

    // data upload on the firebase server
    public void writeFile_server(String fileTitle) {
        for(int i =0; i<measure_n;i++){
            String[] d1 = Arrays.copyOfRange(arr_rcv, i*300, (i+1)*300);
            String d0 = Arrays.toString(d1);
            databaseReference.child(fileTitle).child("iv"+(i+1)).setValue(d0);
        }
    }

    public void writeFile(String fileTitle) throws IOException {
        FileOutputStream fos;
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();

        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileTitle+".txt");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/*");

        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS+ File.separator+"Database/");
        Uri fileUri = resolver.insert(MediaStore.Files.getContentUri("external"), values);
        try{
            fos = (FileOutputStream) resolver.openOutputStream(fileUri);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        try{
            int ll = arr_rcv.length;
            String[] saving_arr = new String[ll];
            System.arraycopy(arr_rcv, 0, saving_arr, 0, arr_rcv.length);
            fos.write(Arrays.toString(saving_arr).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fos.close();
        fos.flush();
        fos.close();
    }

    public static void processIncomingData(byte[] newBytes) {
            int d_len = Hex.bytesToStringUppercase(newBytes).length();

            if(d_len > 0) {
                String d11 = String.valueOf(Hex.bytesToStringUppercase(newBytes).charAt(2));
                String d12 = String.valueOf(Hex.bytesToStringUppercase(newBytes).charAt(3));
                String d13 = String.valueOf(Hex.bytesToStringUppercase(newBytes).charAt(0));
                String d14 = String.valueOf(Hex.bytesToStringUppercase(newBytes).charAt(1));

                String d21 = String.valueOf(Hex.bytesToStringUppercase(newBytes).charAt(6));
                String d22 = String.valueOf(Hex.bytesToStringUppercase(newBytes).charAt(7));
                String d23 = String.valueOf(Hex.bytesToStringUppercase(newBytes).charAt(4));
                String d24 = String.valueOf(Hex.bytesToStringUppercase(newBytes).charAt(5));

                arr_rcv[d_num] = String.valueOf(Integer.valueOf(d11 + d12 + d13 + d14, 16));
                arr_rcv[d_num+1] = String.valueOf(Integer.valueOf(d21 + d22 + d23 + d24, 16));
                addEntry();
            }
    }

    private static void addEntry() {
        int[] cc = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.GRAY, Color.CYAN, Color.BLACK, Color.MAGENTA, Color.DKGRAY, Color.LTGRAY};

        LineData data_iv = chart_iv.getData();

        if(data_iv != null) {

            ILineDataSet set_iv = data_iv.getDataSetByIndex(0);

            if (set_iv == null) {
                set_iv = createSet();
                data_iv.addDataSet(set_iv);
            }

            int rxd1 = Integer.parseInt(arr_rcv[d_num]);
            int rxd2 = Integer.parseInt(arr_rcv[d_num+1]);

            data_iv.addEntry(new Entry(set_iv.getEntryCount(), rxd1), 0);
            data_iv.notifyDataChanged();
            chart_iv.notifyDataSetChanged();


            data_iv.addEntry(new Entry(set_iv.getEntryCount(), rxd2), 0);
            data_iv.notifyDataChanged();
            chart_iv.notifyDataSetChanged();

            chart_iv.setVisibleXRangeMaximum(10);
            chart_iv.moveViewToX(data_iv.getEntryCount());

            if(d_num == 0){
                start_time = (int) System.currentTimeMillis();
            }

            d_num += 2;
            String[] newArr = Arrays.copyOf(arr_rcv, arr_rcv.length+2);
            arr_rcv = newArr;
        }
    }

    private static LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setFillAlpha(110);
        set.setFillColor(Color.parseColor("#d7e7fa"));
        set.setColor(Color.parseColor("#800080"));
        set.setValueTextColor(Color.WHITE);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setLineWidth(2);
        set.setValueTextSize(9f);

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setHighLightColor(Color.rgb(244, 117, 117));

        return set;
    }
    private void set_activity(){
        Intent set_int = new Intent(this, SettingActivity.class);
        startActivity(set_int);
    }

    private void update_setting(){
        Intent set_int = getIntent();
        String vv = set_int.getStringExtra("set_vds");
        String gg = set_int.getStringExtra("set_gain");
        String dd = set_int.getStringExtra("set_duty");
        String ss = set_int.getStringExtra("set_stm");
        String tt = set_int.getStringExtra("set_test");
        if(SettingActivity.set_xx == "1"){
            if (Objects.equals(vv, "1")){vds_tv_2.setText("Vds : 30mV");} else if (Objects.equals(vv, "2")) {vds_tv_2.setText("Vds : 60mV");} else if (Objects.equals(vv, "3")) {vds_tv_2.setText("Vds : 120mV");}
            if(Objects.equals(gg, "4")){gain_tv_2.setText("Gain : 35kOhm");} else if (Objects.equals(gg, "5")) {gain_tv_2.setText("Gain : 120kOhm");} else if (Objects.equals(gg, "6")) {gain_tv_2.setText("Gain : 350kOhm");}
            if(Objects.equals(dd, "p")){duty_tv_2.setText("Duty Cycle : 10%");} else if (Objects.equals(dd, "q")) {duty_tv_2.setText("Duty Cycle : 5%");}
            if(Objects.equals(ss, "r")){stm_tv_2.setText("Stimulate : Start");} else if (Objects.equals(ss, "l")) {stm_tv_2.setText("Stimulate : Stop");}
            if(Objects.equals(tt, "d")){test_tv_2.setText("Test the gate Voltage : On");}
        }
    }

    public static void get_data(){
        final byte[] newBytes = bleService.readFromTransparentUART();
        processIncomingData(newBytes);
    }

    @SuppressLint("SetTextI18n")
    public static void GConnAddress(String address) {
        Log.d("BLE CONN", "Reconnecting...");
        G_BLE_Connection = "0";
        while(G_BLE_Connection.equals("0")){
            bleService.connectBle(address);
            Log.d("@@", G_BLE_Connection);
            try {
                tv_ble.setText("reconnect..");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Log.d("##", G_BLE_Connection);
        tv_ble.setText("connected");
    }
}

class continuous_req extends Thread{
    @Override
    public void run(){
        MeasureActivity.arr_rcv = new String[2];
        String sendData = "g";
        int i = 0;
        while(true) {
            if (i == 0) {
                MeasureActivity.t1 = System.nanoTime();
                min_loop = (long) 10.0;  //그냥 적당히 큰 값으로..
                max_loop = (long) 0.0;
            } else{
                t2 = System.nanoTime();
                float loop_time2 = (float) ((t2 - MeasureActivity.t1)/ Math.pow(10, 9));
                System.out.println("One-loop : "+ loop_time2);
                if (min_loop > loop_time2){
                    min_loop = loop_time2;
                }
                if (max_loop < loop_time2 ){
                    max_loop = loop_time2;
                }

                if(loop_time2 <0.06){
                    try {
                        float add_time = (float) ((0.06 - loop_time2) * Math.pow(10, 3));
                        Log.d("add : ", String.valueOf(add_time)+"ms");
                        Thread.sleep((long) add_time);
                        t3 = System.nanoTime();
                        Log.d("after adding time : ", String.valueOf((t3- MeasureActivity.t1)/ Math.pow(10, 9)));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else{
                    Log.d("over", "60ms");
                }
            }
            try {
                Thread.sleep(40);

            } catch (InterruptedException e) {
                break;
            }
            i++;
            MeasureActivity.t1 = t2;
            bleService.writeToTransparentUART(sendData.getBytes());
        }
    }
}






