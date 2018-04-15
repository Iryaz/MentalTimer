package com.iryaz.mentaltimer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import java.io.IOException;

/**
 * Created by ilya on 07.04.18.
 */

public class SoundBox implements SoundPool.OnLoadCompleteListener {

    public final int REPEAT_INFINITY = -1;

    public SoundBox(Context context) {
        mPlayer = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mAssets = context.getAssets();
    }

    public void play(String sound) {
        try {
            mFileDescriptor = mAssets.openFd(sound);
            mPlayer.setOnLoadCompleteListener(this);
            mSoundId = mPlayer.load(mFileDescriptor, 1);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void stop() {
        mPlayer.stop(mSoundId);
    }

    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        mPlayer.play(mSoundId, 1, 1, 0, 10, 1);
        Log.i(TAG, "Play");
    }

    private SoundPool mPlayer;
    private AssetManager mAssets;
    private int mSoundId;
    private AssetFileDescriptor mFileDescriptor;
    private final String TAG = "SoundBox";

}
