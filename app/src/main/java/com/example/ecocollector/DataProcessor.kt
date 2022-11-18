package com.example.ecocollector

import kotlin.math.abs

class DataProcessor {


    fun getStd(avg:Double, diff:ArrayList<Int>):Double{
        var std = 0.0
        val size = diff.size
        for(i in 0 until diff.size){
            std += abs(diff[i] - avg)
        }
        std/=size
        return std
    }

    fun getModa(data:ArrayList<Int>):Array<Int>{
        var counter = 0
        var best_count = 0
        var key = data[0]
        var best_key = data[0]

        for(i in 0 until data.size){
            if(data[i] == key){
                counter ++
                if(counter > best_count){
                    best_count = counter
                    best_key = key
                }
            }else {
                key = data[i]
                counter = 1
            }
        }
        return arrayOf(best_key, best_count)
    }
}