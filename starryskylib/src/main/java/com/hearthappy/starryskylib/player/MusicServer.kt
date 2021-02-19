package com.hearthappy.starryskylib.player

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import com.hearthappy.starryskylib.R


/**
 * Created Date 2021/2/19.
 * @author ChenRui
 * ClassDescription:
 */
class MusicServer: Service() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        if (mediaPlayer == null) {

            // R.raw.mmp是资源文件，MP3格式的
            mediaPlayer = MediaPlayer.create(this, R.raw.confession)
            mediaPlayer?.isLooping = false
            mediaPlayer?.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
    }
}