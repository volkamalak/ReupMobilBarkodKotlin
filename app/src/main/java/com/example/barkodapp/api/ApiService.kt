package com.example.barkodapp.api

import com.example.barkodapp.model.BarkodRequest
import com.example.barkodapp.model.BarkodResponse
import com.example.barkodapp.model.Machine
import com.example.barkodapp.model.MachineSlotStatus
import com.example.barkodapp.model.Personel
import com.example.barkodapp.model.SendBarcodesRequest
import com.example.barkodapp.model.SendBarcodesResponse
import com.example.barkodapp.model.KontrolBarkodDepoRequest
import com.example.barkodapp.model.KontrolBarkodDepoResponse
import com.example.barkodapp.model.StockTransInsertRequest
import com.example.barkodapp.model.StockTransRequest
import com.example.barkodapp.model.StockTransResponse
import com.example.barkodapp.model.Warehouse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Personel bilgisi getir - PersNo ile personeli sorgular
    @GET("Personel/{persNo}")
    suspend fun getPersonel(
        @Path("persNo") persNo: String
    ): Response<Personel>

    // Tüm makine listesini getir (giriş için)
    @GET("Machine/GetAllMachineGiris")
    suspend fun getAllMachines(): Response<List<Machine>>

    // Seçili makineye ait slot/iş emri durumunu getir
    @GET("MachineSlotStatus")
    suspend fun getMachineSlotStatus(
        @Query("machineId") machineId: Int
    ): Response<List<MachineSlotStatus>>

    // Barkod doğrulama servisi (iş emri + barkod kontrolü)
    @POST("KontrolIsEmriBarkod")
    suspend fun kontrolIsEmriBarkod(
        @Body request: BarkodRequest
    ): Response<BarkodResponse>

    // Barkod listesi gönderme servisi
    @POST("GoodsMovement/SendBarcodes")
    suspend fun sendBarcodes(
        @Body request: SendBarcodesRequest
    ): Response<SendBarcodesResponse>

    // Makine barkod okuma durumu güncelle (sayfa açılınca true, kapanınca false)
    @PUT("MachineBarkodStatus/Update")
    suspend fun updateMachineBarkodStatus(
        @Query("machineId") machineId: Int,
        @Query("statu") statu: Boolean
    ): Response<Any>

    // Depo listesini getir
    @GET("Warehouse")
    suspend fun getWarehouses(): Response<List<Warehouse>>

    // Barkodun depoda stok kontrolü
    @POST("StockTrans/kontrol")
    suspend fun checkStockTrans(
        @Body request: StockTransRequest
    ): Response<StockTransResponse>

    // Transfer sonrası stok hareketi kaydı
    @POST("StockTrans/insert")
    suspend fun insertStockTrans(
        @Body request: StockTransInsertRequest
    ): Response<Any>

    // Depo transfer barkod kontrolü (kaynak depo)
    @POST("KontrolBarkodDepo")
    suspend fun kontrolBarkodDepo(
        @Body request: KontrolBarkodDepoRequest
    ): Response<KontrolBarkodDepoResponse>
}
