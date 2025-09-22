

package com.romankozak.forwardappmobile.domain.wifirestapi

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    
    @POST("/api/v1/files")
    suspend fun uploadFileAsJson(
        @Body body: FileDataRequest,
    ): Response<Unit>

    
    
    
}
