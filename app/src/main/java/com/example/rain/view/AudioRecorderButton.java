package com.example.rain.view;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.example.rain.recorder.R;

import java.io.IOException;

/**
 * Created by rain on 2016/7/19.
 */
public class AudioRecorderButton extends Button implements AudioManager.AudioStateListener {
    private static final int STATE_NORMAL=1;
    private static final int STATE_RECORDING=2;
    private static final int STATE_WANT_CANCEL=3;
    private int mCurState=STATE_NORMAL;
    private boolean isRecording=false;
    private static final int DISTANCE_Y_CANCEL=50;
    private DialogManager mDialogManager;
    private AudioManager mAudioManager;
    private float mTime;
    private static final int MSG_AUDIO_PREPARED=0X110;
    private static final int MSG_VOICE_CHANGED=0X111;
    private static final int MSG_DIALOG_DISMISS=0X112;
    private boolean mReady;
    private Runnable mGetVoiceLevelRunnable=new Runnable() {
        @Override
        public void run() {
            while(isRecording){
                try {
                    Thread.sleep(100);
                    mTime+=0.1f;
                    mHandler.sendEmptyMessage(MSG_VOICE_CHANGED);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_AUDIO_PREPARED:
                    mDialogManager.showRecordingDialog();
                    isRecording=true;
                    new Thread(mGetVoiceLevelRunnable).start();
                    break;
                case MSG_VOICE_CHANGED:
                    mDialogManager.updateVoiceLevel(mAudioManager.getVoiceLevel(7));
                    break;
                case MSG_DIALOG_DISMISS:

                    break;
            }
        }
    };
    public AudioRecorderButton(Context context) {
        this(context, null);
    }

    public AudioRecorderButton(Context context, AttributeSet attrs) {
        super(context,attrs);
        mDialogManager=new DialogManager(getContext());
        String dir= Environment.getExternalStorageDirectory()+"/DIY_AUDIO";
        mAudioManager=AudioManager.getmInstance(dir);
        mAudioManager.setOnAudioStateListener(this);
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                try {
                    mReady=true;
                    mAudioManager.prepareAudio();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
    }

    public interface AudioFinishRecorderListener{
        void onFinish(float seconds,String filePath);
    }

    private AudioFinishRecorderListener mListener;

    public void setAudioFinishRecorderListener(AudioFinishRecorderListener listener){
        mListener=listener;
    }
    @Override
    public void wellPrepared() {
        mHandler.sendEmptyMessage(MSG_AUDIO_PREPARED);
    }

    public boolean onTouchEvent(MotionEvent event){
        int action =event.getAction();
        int x=(int)event.getX();
        int y=(int)event.getY();

        switch (action){
            case MotionEvent.ACTION_DOWN:
                changeState(STATE_RECORDING);
                break;
            case MotionEvent.ACTION_MOVE:
                if(isRecording){
                    if(wantToCancel(x,y)){
                        changeState(STATE_WANT_CANCEL);
                    }else{
                        changeState(STATE_RECORDING);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if(!mReady){
                    reset();
                    return super.onTouchEvent(event);
                }
                if(mTime<0.6f||!isRecording){
                    mDialogManager.tooShort();
                    mAudioManager.cancel();
                    mHandler.sendEmptyMessageDelayed(MSG_DIALOG_DISMISS,1300);
                }else if(mCurState==STATE_RECORDING){
                    mDialogManager.dimissDialog();
                    mAudioManager.release();

                    if(mListener!=null){
                        mListener.onFinish(mTime,mAudioManager.getCurrentFilePath());
                    }
                }else if(mCurState==STATE_WANT_CANCEL){
                    mDialogManager.dimissDialog();
                    mAudioManager.cancel();
                }
                reset();
                break;
        }

        return super.onTouchEvent(event);
    }
    private  void reset(){
        isRecording=false;
        mReady=false;
        mTime=0;
        changeState(STATE_NORMAL);
    }
    private  boolean wantToCancel(int x,int y){
        if(x<0||x>getWidth()){
            return true;
        }
        if(y<-DISTANCE_Y_CANCEL||y>getHeight()+DISTANCE_Y_CANCEL){
            return true;
        }
        return false;
    }
    private void changeState(int state){
        if(mCurState!=state){
            mCurState=state;
            switch (state){
                case STATE_NORMAL:
                    setBackgroundResource(R.drawable.btn_recorder_normal);
                    setText(R.string.str_recorder_normal);
                    break;
                case STATE_RECORDING:
                    setBackgroundResource(R.drawable.btn_recorder_recording);
                    setText(R.string.str_recorder_recording);
                    if(isRecording){
                        mDialogManager.recording();
                    }
                    break;
                case STATE_WANT_CANCEL:
                    setBackgroundResource(R.drawable.btn_recorder_recording);
                    setText(R.string.str_recorder_want_cancel);
                    mDialogManager.wantToCancel();
                    break;
            }
        }
    }

}
