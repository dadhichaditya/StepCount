package com.example.aditya.stepcount;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.Vibrator;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import org.apache.commons.collections.ArrayStack;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    boolean addEntry=true;
    Button mreset;
    TextView step;
    LineChart mChart;
    Boolean plotData = true;
    float prev_acc = 150f;
    float valley;
    float prev_acc1 = 150f;
    boolean firstpeak = true;
    float sjs = 1.5f;
    float sja = 1.5f;
    float jerk = 0f;
    int count = 0;
    boolean firstvalley = true;
    private SensorManager sensorManager;
    private final static String TAG = "StepDetector";
    FileOutputStream writer;
    Float[] fin = new Float[10000];
    Thread thread;
    Vibrator vibrator;
    float result_distance;
    private float mLastDirections[] = new float[3 * 2];
    private float mLastExtremes[][] = {new float[3 * 2], new float[3 * 2]};
    private float mLastDiff[] = new float[3 * 2];
    private int mLastMatch = -1;
    int step_count = 0;
    float datapoints[] = new float[3];
    float smooth_sma[] = new float[3];
    int flag = 0, flag_sma = 0;
    float peak_value, valley_value = 9;
    double acc, lpfilter;
    TextView tv;
    Button mstop;
    DTW dtwObject = new DTW();
    float[] gravityValues = new float[3], smooth_acc = new float[3];
    File sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    File file;

    public void createFile() {
        File dir = new File(sdCard.getAbsolutePath() + "/");
        if (!dir.exists()) {
            dir.mkdir();
        }
        //System.out.println(dirsMade);
//            Log.v("Accel", dirsMade.toString());

         file = new File(dir, "output.csv");
        if (!file.exists()) {
            try {
                file.createNewFile();
                Toast.makeText(getApplicationContext(), "file created", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "file create exception", Toast.LENGTH_SHORT).show();

            }
        } else {
            file.delete();
            createFile();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);        //set layout


        tv = (TextView) findViewById(R.id.textView3);     //binding all
        mstop = (Button) findViewById(R.id.button2);
        mreset = (Button) findViewById(R.id.button);
        step = (TextView) findViewById(R.id.textView);
        step.setText("0");
        createFile();
        mreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View VIEW) {           //reset button
                step_count = 0;
                step.setText(Integer.toString(step_count));

            }
        });

        mstop.setOnClickListener(new View.OnClickListener() {


            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                if (addEntry){
                    addEntry=false;
                    mstop.setText("start recording");

                }else {
                    addEntry=true;
                    mstop.setText("stop recording");

                    return;
                }


                try {
                    Log.i("Entered TRY", "Reading File");
                    Reader reader = Files.newBufferedReader(Paths.get(sdCard.getAbsolutePath() + "/sean/output.csv"));
                    CSVReader csvReader = new CSVReader(reader);
                    String[] input;
                    List<Float> input_list=new ArrayList<>();
                    Float[] inputDTW;
                    while ((input = csvReader.readNext()) != null) {
                        input_list.add(Float.valueOf(input[0]));
                        Log.e("DATA",input[0]);
                    }
                    inputDTW=input_list.toArray(new Float[input_list.size()]);
                    double distance = 0;
                    if (inputDTW != null) {
                        distance = dtwObject.compute(inputDTW, inputDTW).getDistance();
                    }
                    Toast.makeText(MainActivity.this, "D:" + distance, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        //  sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);


        mChart = (LineChart) findViewById(R.id.chart);

        mChart.getDescription().setEnabled(true);
        mChart.getDescription().setText("Accelerolmeter");
        mChart.setTouchEnabled(false);
        mChart.setScaleEnabled(false);
        mChart.setDragEnabled(false);
        mChart.setPinchZoom(true);
        mChart.setDrawGridBackground(false);
        mChart.setBackgroundColor(Color.WHITE);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        mChart.setData(data);

        Legend l = mChart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.BLACK);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMaximum(17f);
        leftAxis.setAxisMinimum(3f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        mChart.getAxisLeft().setDrawGridLines(true);
        mChart.getXAxis().setDrawGridLines(true);
        mChart.setDrawBorders(true);
        startPlot();
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1);

    }


    private LineDataSet createset() {
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(1.5f);
        set.setColor(Color.MAGENTA);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setHighlightEnabled(false);
        set.setCubicIntensity(0.2f);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        return set;
    }

    private void startPlot() {
        if (thread != null) {
            thread.interrupt();
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    plotData = true;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    private boolean detectPeak(float prev_acc, float prev_acc1, double acc, SensorEvent event) {
        if (prev_acc1 > prev_acc && prev_acc1 > acc && prev_acc > 10.8) {

            if (firstpeak) {
                peak_value = prev_acc1;
                tv.setText("" + (event.timestamp) * (1.0 / 1000000));
                firstpeak = false;
            }

            jerk = Math.abs(prev_acc1 - valley);

            valley = detectValley(prev_acc, prev_acc1, acc, event);
            Log.i("Normal Peak Detected", "YES");


            if (jerk > 1.5 && Math.abs(peak_value - prev_acc1) < 1.0f) {


                Log.i("INFO", "Valid Peak Detected");
                peak_value = prev_acc1;


                return true;
            }


        }
        return false;

    }

    private float detectValley(float prev_acc, float prev_acc1, double acc, SensorEvent event) {
        if (prev_acc1 < prev_acc && prev_acc1 < acc && prev_acc1 < 9 && prev_acc1 > 7.8) {
            valley_value = prev_acc1;
            mreset.setText("" + Integer.toString((int) event.timestamp));
        }

        return valley_value;

    }

    private void addEntry(SensorEvent event) {
        if (!addEntry){
            return;
        }

        LineData data = mChart.getData();
        Sensor sensor = event.sensor;

//        if(gravityValues!=null)
//        {
//            gravityValues[0]=(float)(0.9*gravityValues[0]+0.1*event.values[0]);
//            gravityValues[1]=(float)(0.9*gravityValues[1]+0.1*event.values[1]);
//            gravityValues[2]=(float)(0.9*gravityValues[2]+0.1*event.values[2]);
//
//            smooth_acc[0]=event.values[0]-gravityValues[0];
//            smooth_acc[1]=event.values[1]-gravityValues[1];
//            smooth_acc[2]=event.values[2]-gravityValues[2];
//            Log.i("smoothened",smooth_acc[0]+" "+smooth_acc[1]+" "+smooth_acc[2]);
//
//        }
//        if(smooth_acc!=null)
        acc = Math.sqrt(Math.pow(event.values[0], 2) + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2));
        if (prev_acc == 150f) {
            prev_acc = (float) acc;
            Log.i(" Noted First Acc", "" + prev_acc);
        } else if (prev_acc1 == 150f) {
            prev_acc1 = (float) acc;
            Log.i(" Noted Second Acc", "" + prev_acc1);

        } else {
            acc = acc * 0.1 + 0.9 * prev_acc1;


            if (data != null) {
                ILineDataSet set = data.getDataSetByIndex(0);
                if (set == null) {
                    set = createset();
                    data.addDataSet(set);
                }
                if (detectPeak(prev_acc, prev_acc1, acc, event)) {
                    step_count++;

                    step.setText(Integer.toString(step_count));
                    vibrator.vibrate(250);


                }
                prev_acc = prev_acc1;
                prev_acc1 = (float) acc;

                try {


                    String entry[] = new String[1];
                    entry[0] = Float.toString(prev_acc1);
                    CSVWriter csvWriter = new CSVWriter(new FileWriter(file, true));


                    try {
                        csvWriter.writeNext(entry);
                        csvWriter.flush();
                        csvWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                data.addEntry(new Entry(set.getEntryCount(), (float) acc), 0);
                data.notifyDataChanged();
                mChart.notifyDataSetChanged();
                mChart.setVisibleXRangeMaximum(250);
                mChart.moveViewToX(data.getEntryCount());
            }
        }


    }


    public void onAccuracyChanged(Sensor arg0, int arg1) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravityValues = event.values;
        } else {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && plotData) {
                if (file != null) {
                    if (file.exists()) {
                        addEntry(event);
                    } else {
                        createFile();
                    }
                }

                plotData = false;
            }
//                }
//            }
        }


    }

    public void onStop() {
        super.onStop();
        sensorManager.unregisterListener((SensorEventListener) this);
    }

    public void onPause() {
        super.onPause();
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}