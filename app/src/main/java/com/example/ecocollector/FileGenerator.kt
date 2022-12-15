package com.example.ecocollector

import android.app.Activity
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.io.path.name

class FileGenerator(private val ctx: Activity, private val type:Int) {
    private val name = "${android.os.Build.MODEL} ${android.os.Build.ID}";
    private val ROOT_PATH = "/storage/emulated/0/"
    private val PATH = "/Documents"

    private val NAMES = arrayOf("co2.zip", "noise.zip")
    var available = ArrayList<File>()

    companion object{
        val CO2_MEASURE = 50
        val NOISE_MEASURE = 51
    }

    var registry = JSONObject()
    var lat = JSONArray()
    var lon = JSONArray()
    var data = JSONArray()
    var std = JSONArray()
    var moda = JSONArray()

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private var current = LocalDateTime.now().format(formatter)

    init {
        if (Files.exists(Path(ROOT_PATH + PATH))) {
            Log.d("COUT", "Directory exists")
        } else {
            try {
                val f = File(ROOT_PATH + PATH)
                if (f.mkdir())
                    Log.d("COUT", "Ok")

            } catch (ex: Exception) {
                Log.d("COUT", "${ex.message}")
            }
        }
    }

    fun hasFiles():Boolean{
        available = ArrayList<File>()
        var dirs = Files.list(Path(ROOT_PATH + PATH))
        Log.d("COUT", "Files available: ")
        try {
            dirs.forEach {
                for (n in NAMES){
                    if (it.name.contains(n)){
                        available.add(it.toFile())
                        Log.d("COUT", it.name)
                    }
                }
            }
        }catch (ex:Exception){
            Log.d("COUT", "${ex.message}")
        }
        return (available.size > 0)
    }

    fun templates(){
        registry.accumulate("dispositivo", name)

        if (type == CO2_MEASURE){
            registry.accumulate("type", "CO2_MEASURE")
            registry.accumulate("ppm", data)

        }else if(type == NOISE_MEASURE){
            registry.accumulate("type", "NOISE_MEASURE")
            registry.accumulate("dB", data)
            registry.accumulate("std", std)
            registry.accumulate("moda", moda)
        }
        registry.accumulate("lat", lat)
        registry.accumulate("lon", lon)

        registry.accumulate("interval", 30)
        current = LocalDateTime.now().format(formatter)
        registry.accumulate("begin_time", current.toString())
    }

    fun terminate(){
        current = LocalDateTime.now().format(formatter)
        registry.accumulate("end_time", current.toString())
//        Log.d("COUT", "Template: ${registry.toString(4)}")
        writeFile(ROOT_PATH + "Documents" + "/${when(type){
            CO2_MEASURE->"co2"
            NOISE_MEASURE->"noise"
            else -> "_"
        }}")
        release()
    }

    fun release(){
        registry = JSONObject()
        lat = JSONArray()
        lon = JSONArray()
        data = JSONArray()
    }

    fun submitAll():Boolean{
        for(f in available){
            Log.d("COUT", "Uploading: ${f.name}")
            try {
                UploadUtility(ctx).uploadFile(f)
            }catch (ex:Exception){
                Log.d("COUT", "Exception: ${ex.message}")
            }
        }
        return hasFiles()
    }

    fun writeFile(s:String){
        val file = File(ROOT_PATH + "Documents/meta.json" )
        try {
            file.writeText(registry.toString())
            Log.d("COUT", "File saved: meta.json")

            val file = "${ROOT_PATH}Documents/meta.json"
            ZipOutputStream(BufferedOutputStream(FileOutputStream("$s.zip"))).use { out ->
                FileInputStream(file).use { fi ->
                    BufferedInputStream(fi).use { origin ->
                        val entry = ZipEntry("meta.json")
                        out.putNextEntry(entry)
                        origin.copyTo(out, 32)
                    }
                }
            }
            Files.delete(Path(ROOT_PATH + "Documents/meta.json"))
            Log.d("COUT", "File saved: $s.zip")
//            UploadUtility(ctx).uploadFile(File("$s.zip"))
        }catch (ex:Exception){
            Log.d("COUT", "${ex.message}")
        }
    }
}