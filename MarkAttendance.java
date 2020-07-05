package com.syscode.attendance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class MarkAttendance extends AppCompatActivity implements MFS100Event {
    List<User> fingerprint ;
    String reg;
    String ecode,room_name;
    EditText sheetno;
    Button btnInit,btnAttendance;
    Button btnUninit;
    Button btnSyncCapture;
    Button btnStopCapture;
    Button btnMatchISOTemplate;
    Button btnExtractISOImage;
    Button btnExtractAnsi;
    Button btnClearLog;
    Button btnExtractWSQImage;
    TextView lblMessage;
    EditText txtEventLog;
    ImageView imgFinger;
    CheckBox cbFastDetection;
    private static long mLastClkTime = 0;
    private static long Threshold = 1500;

    String scanFingerprint;
    ArrayList<String> list = new ArrayList<String>();


    private enum ScannerAction {
        Capture, Verify
    }

    byte[] Enroll_Template;
    byte[] Verify_Template;
    private FingerData lastCapFingerData = null;
    MarkAttendance.ScannerAction scannerAction = MarkAttendance.ScannerAction.Capture;

    int timeout = 10000;
    MFS100 mfs100 = null;
    FirebaseDatabase database;
    DatabaseReference ref;
    private boolean isCaptureRunning = false;

    DatabaseReference databaseReference;
    ArrayAdapter<String> arrayAdapter;
    List<String> arrayListnew = new ArrayList<>();
    List<String> cities = new ArrayList<String>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        FindFormControls();
        try {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        } catch (Exception e) {
            Log.e("ErrorJi", e.toString());
        }

        try {
            mfs100 = new MFS100(this);
            mfs100.SetApplicationContext(MarkAttendance.this);
        } catch (Exception e) {
            e.printStackTrace();
        }


        databaseReference = FirebaseDatabase.getInstance().getReference("User");

        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                User value = dataSnapshot.getValue(User.class);
                cities.add(value.fingerprint1);
                cities.add(value.ecode);

                SetLogOnUIThread("Array List:  "+ cities.size());
//                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        database = FirebaseDatabase.getInstance();
        ref = database.getReference("Attendance");

        sheetno = (EditText) findViewById(R.id.sheetno);

        btnAttendance = (Button) findViewById(R.id.submit);

        btnAttendance.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String sheet_no = sheetno.getText().toString();
                String status = "Present";
                if (TextUtils.isEmpty(sheet_no) && TextUtils.isEmpty(ecode) && TextUtils.isEmpty(reg) ) {
                    Toast.makeText(MarkAttendance.this, "Please Enter Sheet Number", Toast.LENGTH_SHORT).show();
                    return;
                }else{

                    String id = ref.push().getKey();
                    Attendance users = new Attendance(reg,ecode,sheet_no,room_name, status);
                    ref.child(id).setValue(users);

                    Toast.makeText(MarkAttendance.this, "Attendance Mark", Toast.LENGTH_SHORT).show();

                }

            }
        });
    }






    public void FindFormControls() {
        try {
            btnInit = (Button) findViewById(R.id.btnInit);
//            btnUninit = (Button) findViewById(R.id.btnUninit);
//            btnMatchISOTemplate = (Button) findViewById(R.id.btnMatchISOTemplate);
//            btnExtractISOImage = (Button) findViewById(R.id.btnExtractISOImage);
//            btnExtractAnsi = (Button) findViewById(R.id.btnExtractAnsi);
//            btnExtractWSQImage = (Button) findViewById(R.id.btnExtractWSQImage);
            btnClearLog = (Button) findViewById(R.id.btnClearLog);
            lblMessage = (TextView) findViewById(R.id.lblMessage);
            txtEventLog = (EditText) findViewById(R.id.txtEventLog);
            imgFinger = (ImageView) findViewById(R.id.imgFinger);
            btnSyncCapture = (Button) findViewById(R.id.btnSyncCapture);
//            btnStopCapture = (Button) findViewById(R.id.btnStopCapture);
//            cbFastDetection = (CheckBox) findViewById(R.id.cbFastDetection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onStart() {
        try {
            if (mfs100 == null) {
                mfs100 = new MFS100(this);
                mfs100.SetApplicationContext(MarkAttendance.this);
            } else {
                InitScanner();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onStart();
    }

    protected void onStop() {
        try {
            if (isCaptureRunning) {
                int ret = mfs100.StopAutoCapture();
            }
            Thread.sleep(500);
            //            UnInitScanner();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        try {
            if (mfs100 != null) {
                mfs100.Dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public void onControlClicked(View v) {
        if (SystemClock.elapsedRealtime() - mLastClkTime < Threshold) {
            return;
        }
        mLastClkTime = SystemClock.elapsedRealtime();
        try {
            switch (v.getId()) {
                case R.id.btnInit:
                    InitScanner();
                    break;

                case R.id.btnSyncCapture:
                    scannerAction = MarkAttendance.ScannerAction.Capture;
                    if (!isCaptureRunning) {
                        StartSyncCapture();
                    }
                    break;

//                case R.id.btnMatchISOTemplate:
//                    scannerAction = ScannerAction.Verify;
//                    if (!isCaptureRunning) {
//                        StartSyncCapture();
//                    }
//                    break;
//                case R.id.btnExtractISOImage:
//                    ExtractISOImage();
//                    break;
                case R.id.btnClearLog:
                    ClearLog();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void InitScanner() {
        try {
            int ret = mfs100.Init();
            if (ret != 0) {
                SetTextOnUIThread(mfs100.GetErrorMsg(ret));
            } else {
                SetTextOnUIThread("Init success");
                String info = "Serial: " + mfs100.GetDeviceInfo().SerialNo()
                        + " Make: " + mfs100.GetDeviceInfo().Make()
                        + " Model: " + mfs100.GetDeviceInfo().Model()
                        + "\nCertificate: " + mfs100.GetCertification();
                SetLogOnUIThread(info);
            }
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Init failed, unhandled exception",
                    Toast.LENGTH_LONG).show();
            SetTextOnUIThread("Init failed, unhandled exception");
        }
    }
    private void UnInitScanner() {
        try {
            int ret = mfs100.UnInit();
            if (ret != 0) {
                SetTextOnUIThread(mfs100.GetErrorMsg(ret));
            } else {
                SetLogOnUIThread("Uninit Success");
                SetTextOnUIThread("Uninit Success");
                lastCapFingerData = null;
            }
        } catch (Exception e) {
            Log.e("UnInitScanner.EX", e.toString());
        }
    }
    private void StopCapture() {
        try {
            mfs100.StopAutoCapture();
        } catch (Exception e) {
            SetTextOnUIThread("Error");
        }
    }
    private void ClearLog() {
        txtEventLog.post(new Runnable() {
            public void run() {
                try {
                    txtEventLog.setText("", TextView.BufferType.EDITABLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void ExtractANSITemplate() {
        try {
            if (lastCapFingerData == null) {
                SetTextOnUIThread("Finger not capture");
                return;
            }
            byte[] tempData = new byte[2000]; // length 2000 is mandatory
            byte[] ansiTemplate;
            int dataLen = mfs100.ExtractANSITemplate(lastCapFingerData.RawData(), tempData);
            if (dataLen <= 0) {
                if (dataLen == 0) {
                    SetTextOnUIThread("Failed to extract ANSI Template");
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(dataLen));
                }
            } else {
                ansiTemplate = new byte[dataLen];
                System.arraycopy(tempData, 0, ansiTemplate, 0, dataLen);
                WriteFile("ANSITemplate.ansi", ansiTemplate);
                SetTextOnUIThread("Extract ANSI Template Success");
            }
        } catch (Exception e) {
            Log.e("Error", "Extract ANSI Template Error", e);
        }
    }

    private void ExtractISOImage() {
        try {
            if (lastCapFingerData == null) {
                SetTextOnUIThread("Finger not capture");
                return;
            }
            byte[] tempData = new byte[(mfs100.GetDeviceInfo().Width() * mfs100.GetDeviceInfo().Height()) + 1078];
            byte[] isoImage;

            // ISOType 1 == Regular ISO Image
            // 2 == WSQ Compression ISO Image
            int dataLen = mfs100.ExtractISOImage(lastCapFingerData.RawData(), tempData, 2);
            if (dataLen <= 0) {
                if (dataLen == 0) {
                    SetTextOnUIThread("Failed to extract ISO Image");
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(dataLen));
                }
            } else {
                isoImage = new byte[dataLen];
                System.arraycopy(tempData, 0, isoImage, 0, dataLen);
                WriteFile("ISOImage.iso", isoImage);
                SetTextOnUIThread("Extract ISO Image Success");
            }
        } catch (Exception e) {
            Log.e("Error", "Extract ISO Image Error", e);
        }
    }

    private void ExtractWSQImage() {
        try {
            if (lastCapFingerData == null) {
                SetTextOnUIThread("Finger not capture");
                return;
            }
            byte[] tempData = new byte[(mfs100.GetDeviceInfo().Width() * mfs100.GetDeviceInfo().Height()) + 1078];
            byte[] wsqImage;
            int dataLen = mfs100.ExtractWSQImage(lastCapFingerData.RawData(), tempData);
            if (dataLen <= 0) {
                if (dataLen == 0) {
                    SetTextOnUIThread("Failed to extract WSQ Image");
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(dataLen));
                }
            } else {
                wsqImage = new byte[dataLen];
                System.arraycopy(tempData, 0, wsqImage, 0, dataLen);
                WriteFile("WSQ.wsq", wsqImage);
                SetTextOnUIThread("Extract WSQ Image Success");
            }
        } catch (Exception e) {
            Log.e("Error", "Extract WSQ Image Error", e);
        }
    }

    private long mLastAttTime=0l;
    @Override
    public void OnDeviceAttached(int vid, int pid, boolean hasPermission) {

        if (SystemClock.elapsedRealtime() - mLastAttTime < Threshold) {
            return;
        }
        mLastAttTime = SystemClock.elapsedRealtime();
        int ret;
        if (!hasPermission) {
            SetTextOnUIThread("Permission denied");
            return;
        }
        try {
            if (vid == 1204 || vid == 11279) {
                if (pid == 34323) {
                    ret = mfs100.LoadFirmware();
                    if (ret != 0) {
                        SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                    } else {
                        SetTextOnUIThread("Load firmware success");
                    }
                } else if (pid == 4101) {
                    String key = "Without Key";
                    ret = mfs100.Init();
                    if (ret == 0) {
                        showSuccessLog(key);
                    } else {
                        SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSuccessLog(String key) {
        try {
            SetTextOnUIThread("Init success");
            String info = "\nKey: " + key + "\nSerial: "
                    + mfs100.GetDeviceInfo().SerialNo() + " Make: "
                    + mfs100.GetDeviceInfo().Make() + " Model: "
                    + mfs100.GetDeviceInfo().Model()
                    + "\nCertificate: " + mfs100.GetCertification();
            SetLogOnUIThread(info);
        } catch (Exception e) {
        }
    }
    long mLastDttTime=0l;
    @Override
    public void OnDeviceDetached() {
        try {

            if (SystemClock.elapsedRealtime() - mLastDttTime < Threshold) {
                return;
            }
            mLastDttTime = SystemClock.elapsedRealtime();
            UnInitScanner();

            SetTextOnUIThread("Device removed");
        } catch (Exception e) {
        }
    }

    @Override
    public void OnHostCheckFailed(String err) {
        try {
            SetLogOnUIThread(err);
            Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {
        }
    }


    private void StartSyncCapture() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                SetTextOnUIThread("");
                isCaptureRunning = true;
                try {
                    FingerData fingerData = new FingerData();
                    int ret = mfs100.AutoCapture(fingerData, timeout,false);
                    Log.e("StartSyncCapture.RET", "" + ret);
                    if (ret != 0) {
                        SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                    } else {
                        lastCapFingerData = fingerData;

                        final Bitmap bitmap = BitmapFactory.decodeByteArray(fingerData.FingerImage(), 0,
                                fingerData.FingerImage().length);
                        MarkAttendance.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imgFinger.setImageBitmap(bitmap);
                            }
                        });

//                        Log.e("RawImage", Base64.encodeToString(fingerData.RawData(), Base64.DEFAULT));
//                        Log.e("FingerISOTemplate", Base64.encodeToString(fingerData.ISOTemplate(), Base64.DEFAULT));
                        SetTextOnUIThread("Capture Success");
                        String log = "\nQuality: " + fingerData.Quality()
                                + "\nNFIQ: " + fingerData.Nfiq()
                                + "\nWSQ Compress Ratio: "
                                + fingerData.WSQCompressRatio()
                                + "\nImage Dimensions (inch): "
                                + fingerData.InWidth() + "\" X "
                                + fingerData.InHeight() + "\""
                                + "\nImage Area (inch): " + fingerData.InArea()
                                + "\"" + "\nResolution (dpi/ppi): "
                                + fingerData.Resolution() + "\nGray Scale: "
                                + fingerData.GrayScale() + "\nBits Per Pixal: "
                                + fingerData.Bpp() + "\nWSQ Info: "
                                + fingerData.WSQInfo();
                        SetLogOnUIThread(log);
                        SetData2(fingerData);
                    }
                } catch (Exception ex) {
                    SetTextOnUIThread(ex.toString());
                } finally {
                    isCaptureRunning = false;
                }
            }
        }).start();
    }

    public void setScanFingerprint(String scanFingerprint) {
        this.scanFingerprint = scanFingerprint;
    }
    public ArrayList<String> getList() {
        return list;
    }

    public void SetData2(FingerData fingerData) {
        try {
            if (scannerAction.equals(ScannerAction.Capture)) {

                Verify_Template = new byte[fingerData.ISOTemplate().length];

                Log.e("verify-->", Verify_Template.toString());

                //get all data from database of workers
//                fingerprint = arrayList;




               for(int i=0 ; i<cities.size() ; i++){



                   byte[] byt1 = Base64.decode(cities.get(i),Base64.DEFAULT);
                   Enroll_Template = byt1;

//                   SetLogOnUIThread("Bug 1"+ Enroll_Template.toString());
//                   SetLogOnUIThread("Bug 2"+ Verify_Template.toString());

                   //for first finger print matching
                   int ret = mfs100.MatchISO(Enroll_Template, Verify_Template);

                   if (ret > 0) {



                       SetTextOnUIThread("Error1: " + ret + "(" + mfs100.GetErrorMsg(ret) + ")");
                   } else {
                       ecode = cities.get(1);
                       SetLogOnUIThread(ecode);
                       //if first finger print match
                       if (ret >= 1400) {
                           SetTextOnUIThread("Finger1 matched with score: " + ret);
//                            SetTextOnUIThread(byt1.toString());
                           // setWorkerModel(workerModel);
                           // setVerfiy(true);
                           break;
                       }
                   }

               }










            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            WriteFile("Raw.raw", fingerData.RawData());
            WriteFile("Bitmap.bmp", fingerData.FingerImage());
            WriteFile("ISOTemplate.iso", fingerData.ISOTemplate());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void WriteFile(String filename, byte[] bytes) {
        try {
            String path = Environment.getExternalStorageDirectory()
                    + "//FingerData";
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            path = path + "//" + filename;
            file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream stream = new FileOutputStream(path);
            stream.write(bytes);
            stream.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void WriteFileString(String filename, String data) {
        try {
            String path = Environment.getExternalStorageDirectory()
                    + "//FingerData";
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            path = path + "//" + filename;
            file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream stream = new FileOutputStream(path);
            OutputStreamWriter writer = new OutputStreamWriter(stream);
            writer.write(data);
            writer.flush();
            writer.close();
            stream.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }



    private void SetTextOnUIThread(final String str) {

        lblMessage.post(new Runnable() {
            public void run() {
                try {
                    lblMessage.setText(str);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void SetLogOnUIThread(final String str) {

        txtEventLog.post(new Runnable() {
            public void run() {
                try {
                    txtEventLog.append("\n" + str);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }





}
