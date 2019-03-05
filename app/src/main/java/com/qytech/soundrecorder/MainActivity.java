package com.qytech.soundrecorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.LinkedList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SoundRecorder";

    private Button m_btnExit;
    private int mInBuffSize;
    private AudioRecord mInAudioRecord;
    private byte[] mInBytes;
    private LinkedList<byte[]> mInLinkedList;

    private int mOutBuffSize;
    private AudioTrack mOutAudioTrack;
    private byte[] mOutBytes;

    private Thread mRecordThread;
    private Thread mPlayThread;
    private boolean mFlag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        m_btnExit = findViewById(R.id.btn_exit);
        m_btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFlag = false;
                mInAudioRecord.stop();
                mInAudioRecord = null;
                mOutAudioTrack.stop();
                mOutAudioTrack = null;
                finish();
            }
        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0x01);
        } else {
            startRecord();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecord();
        }
    }

    private void startRecord() {
        init();
        mRecordThread = new Thread(new RecordSoundRunnable());
        mPlayThread = new Thread(new PlayRecordRunnable());
        mRecordThread.start();
        m_btnExit.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPlayThread.start();
            }
        }, 1000);
    }

    private void init() {
        mInBuffSize = AudioRecord.getMinBufferSize(8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        mInAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mInBuffSize);

        mInBytes = new byte[mInBuffSize];
        mInLinkedList = new LinkedList<>();

        mOutBuffSize = AudioTrack.getMinBufferSize(8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        mOutAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mOutBuffSize,
                AudioTrack.MODE_STREAM);
        mOutBytes = new byte[mOutBuffSize];

    }

    // 录音
    private class RecordSoundRunnable implements Runnable {

        @Override
        public void run() {
            Log.d(TAG, "RecordSoundRunnable running: ");
            byte[] bytes;
            mInAudioRecord.startRecording();
            while (mFlag) {
                mInAudioRecord.read(mInBytes, 0, mInBuffSize);
                bytes = mInBytes.clone();
                if (mInLinkedList.size() >= 2) {
                    mInLinkedList.removeFirst();
                } else {
                    mInLinkedList.add(bytes);
                }
            }
        }
    }


    //放音
    private class PlayRecordRunnable implements Runnable {

        @Override
        public void run() {
            Log.d(TAG, "PlayRecordRunnable running: ");
            mOutAudioTrack.play();
            byte[] bytes;

            try {
                while (mFlag) {
                    mOutBytes = mInLinkedList.getFirst();
                    bytes = mOutBytes.clone();
                    mOutAudioTrack.write(bytes, 0, bytes.length);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayThread != null) {
            mPlayThread.interrupt();
            mPlayThread = null;
        }
        if (mRecordThread != null) {
            mRecordThread.interrupt();
            mRecordThread = null;
        }
    }
}
