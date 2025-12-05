package com.example.barkodapp.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    // Login servisi - Sicil numarasını kontrol eder
    @GET("CheckPersonnel")
    suspend fun loginOperator(
        @Query("sicil") sicilNo: String
    ): Response<Boolean>

    // Barkod doğrulama servisi
    @GET("IsBarcode")
    suspend fun validateBarcode(
        @Query("sicil") barcode: String
    ): Response<Boolean>

    // Barkod listesi gönderme servisi
    @POST("SendBarcodes")
    suspend fun sendBarcodes(
        @Body barcodes: List<String>
    ): Response<Boolean>
}
