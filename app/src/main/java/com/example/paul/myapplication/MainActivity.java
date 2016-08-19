package com.example.paul.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "ChargeControl";

    private TextView textView_charge_input, textView_charge_charge, textView_light;
    private SeekBar seekBar_charge_input, seekBar_charge_charge, seekBar_light;

    DataOutputStream g_outputStream;

    private Handler mHandler = new Handler();
    private boolean bActive, bScrolling;


    int readIntFromFile(String str)
    {
        int value = -1;
        String text = null;
        try {
            File file = new File(str);

            BufferedReader br = new BufferedReader(new FileReader(file));
            text = br.readLine();
            value = Integer.valueOf(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            if(!bScrolling) {
                seekBar_charge_input.setProgress(readIntFromFile("/proc/charger_input_ma"));
                seekBar_charge_charge.setProgress(readIntFromFile("/proc/charger_charge_ma"));
            }
/*
            mHandler.post(new Runnable() {
                public void run() {
                }
            });*/
            if(bActive && !bScrolling)
                timerHandler.postDelayed(this, 1000);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView_charge_input = (TextView)findViewById(R.id.text_charge_input);
        seekBar_charge_input = (SeekBar)findViewById(R.id.seekBar_charge_input);
        seekBar_charge_input.setOnSeekBarChangeListener(this);

        textView_charge_charge = (TextView)findViewById(R.id.text_charge_charge);
        seekBar_charge_charge = (SeekBar)findViewById(R.id.seekBar_charge_charge);
        seekBar_charge_charge.setOnSeekBarChangeListener(this);

        textView_light = (TextView)findViewById(R.id.text_light);
        seekBar_light = (SeekBar)findViewById(R.id.seekBar_light);
        seekBar_light.setOnSeekBarChangeListener(this);

        try{
            Process su = Runtime.getRuntime().exec("su");
            g_outputStream = new DataOutputStream(su.getOutputStream());
        }catch(IOException e){
            e.printStackTrace();
        }

        bActive = true;
        bScrolling = false;
        timerHandler.postDelayed(timerRunnable, 0);
    }

    @Override
    protected void onPause() {
        bActive = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        bActive = true;
        timerHandler.postDelayed(timerRunnable, 0);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar_down if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar_down item clicks here. The action bar_down will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        String strProgr = String.valueOf(progress);
        switch(seekBar.getId())
        {
            case R.id.seekBar_charge_input:
                String strInput;
                textView_charge_input.setText(String.format("Input: %s mA", strProgr));
                break;
            case R.id.seekBar_charge_charge:
                textView_charge_charge.setText(String.format("Charge: %s mA", strProgr));
                break;
            case R.id.seekBar_light:
                textView_light.setText(String.format("Light: %s", strProgr));
                try{
                    g_outputStream.writeBytes("echo " + strProgr + " > /sys/class/leds/torch-sec1/brightness\n");
                }catch(IOException e){
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        bScrolling = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        String strFile;
        switch(seekBar.getId())
        {
            case R.id.seekBar_charge_input:
                strFile = "/proc/charger_input_ma\n";
                break;
            case R.id.seekBar_charge_charge:
                strFile = "/proc/charger_charge_ma\n";
                break;
            default:
                return;
        }
        try{
            g_outputStream.writeBytes("echo " + seekBar.getProgress() + " > " + strFile);
        }catch(IOException e){
            e.printStackTrace();
        }
        bScrolling = false;
        timerHandler.postDelayed(timerRunnable, 0);
    }

}
