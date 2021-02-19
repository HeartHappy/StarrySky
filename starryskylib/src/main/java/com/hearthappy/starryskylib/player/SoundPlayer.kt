package com.hearthappy.starryskylib.player

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import com.hearthappy.starryskylib.R
import kotlin.properties.Delegates
import kotlin.random.Random


/**
 * Created Date 2021/2/19.
 * @author ChenRui
 * ClassDescription:
 */
class SoundPlayer {
    private var music: MediaPlayer by Delegates.notNull()
    private var soundPool: SoundPool by Delegates.notNull()

    private var musicSt = true //音乐开关

    private var soundSt = true //音效开关

    private var context: Context? = null

    private val musicId = intArrayOf(R.raw.confession)
    private var soundMap //音效资源id与加载过后的音源id的映射关系表
            : MutableMap<Int, Int> by Delegates.notNull()

    fun init(c: Context?) {
        context = c
        initMusic()
        initSound()
    }

    //初始化音效播放器
    private fun initSound() {
        soundPool = SoundPool(10, AudioManager.STREAM_MUSIC, 100)
        soundMap = HashMap()
        (soundMap as HashMap<Int, Int>)[R.raw.confession] = soundPool.load(context, R.raw.confession, 1)
    }

    //初始化音乐播放器
    private fun initMusic() {
        val r: Int = Random.nextInt(musicId.size)
        music = MediaPlayer.create(context, musicId[r])
        this.music.isLooping = true
    }


    fun playSound(resId: Int) {
        if (!soundSt) return
        val soundId = soundMap[resId]
        if (soundId != null) soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    /**
     * 暂停音乐
     */
    fun pauseMusic() {
        if (music.isPlaying) music.pause()
    }

    /**
     * 播放音乐
     */
    fun startMusic() {
        if (musicSt) music.start()
    }

    /**
     * 切换一首音乐并播放
     */
    fun changeAndPlayMusic() {
        music.release()
        initMusic()
        startMusic()
    }

    /**
     * 获得音乐开关状态
     * @return
     */
    fun isMusicSt(): Boolean {
        return musicSt
    }

    /**
     * 设置音乐开关
     * @param musicSt
     */
    fun setMusicSt(musicSt: Boolean) {
        this.musicSt = musicSt
        if (musicSt) music.start() else music.stop()
    }

    /**
     * 获得音效开关状态
     * @return
     */
    fun isSoundSt(): Boolean {
        return soundSt
    }

    /**
     * 设置音效开关
     * @param soundSt
     */
    fun setSoundSt(soundSt: Boolean) {
        this.soundSt = soundSt
    }
}