package com.example.ecocollector

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/*
MediaRecorder es una clase considerada obsoleta, pero no puede ser
reemplazada por MediaRecorder(context:Context) debido a que esta última requiere de
la API 31 de Android como mínimo para funcionar (A partir de Android 12)
*/
class AudioRecorder(private val context: Activity): MediaRecorder() {

    private var path:String = Environment.getExternalStorageDirectory().absolutePath + "/Recordings/"
    private var output:String = path + "recording.m4a"

    /*
    * check_permission() permite verificar si la aplicación cuenta con los permisos
    * de lectura, escritura y acceso al microfono. Devuelve un booleano si es así
    */


    fun checkPermission():Boolean{
        val recAudPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        val writeExSt = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readExSt = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        val permGranted = PackageManager.PERMISSION_GRANTED
        return (recAudPerm != permGranted || writeExSt != permGranted || readExSt != permGranted)
    }
    /*
     * request_permission() se encarga de realizar la solicitud de permisos de la app
     * se definió de forma separada para no llamarla explícitamente cada vez que se
     * realice la verificación de permisos
     */

    fun requestPermission(){
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        ActivityCompat.requestPermissions(context, permissions, 0)
    }

    /*
    * setup_media() se encarga de configurar el controlador para capturar el audio
    * del micrófono, darle formato y almacenarlo
    * */

    fun setupMedia(){
        setAudioSource(AudioSource.MIC)
        setOutputFormat(OutputFormat.MPEG_4)
        setAudioEncoder(AudioEncoder.AAC)
        setAudioChannels(1)
        setAudioSamplingRate(44100)
        setAudioEncodingBitRate(96000)
        setOutputFile(output)
    }

    /*
    start_recording() se encarga de inicializar la grabación
    * */
    fun startRecording(){
        try{
            prepare()
            start()
        }catch (e:Exception){
            Log.d("COUT", e.message.toString())
        }
    }
    /*
    * stop_recording() detiene el proceso de grabación y libera los recursos
    * utilizados para la grabación
    * */
    fun stopRecording(){
        stop()
        release()
    }

    fun getPath():String{
        return path
    }


}