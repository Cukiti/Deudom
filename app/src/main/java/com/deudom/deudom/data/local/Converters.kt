package com.deudom.deudom.data.local

import androidx.room.TypeConverter
import com.deudom.deudom.data.Payment
import com.deudom.deudom.data.Installment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromPaymentList(value: List<Payment>?): String? {
        val gson = Gson()
        val type = object : TypeToken<List<Payment>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toPaymentList(value: String?): List<Payment>? {
        val gson = Gson()
        val type = object : TypeToken<List<Payment>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromInstallmentList(value: List<Installment>?): String? {
        val gson = Gson()
        val type = object : TypeToken<List<Installment>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toInstallmentList(value: String?): List<Installment>? {
        val gson = Gson()
        val type = object : TypeToken<List<Installment>>() {}.type
        return gson.fromJson(value, type)
    }
}
