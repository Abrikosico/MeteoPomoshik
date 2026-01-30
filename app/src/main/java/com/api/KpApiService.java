package com.api;

import com.model.KpIndex;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface KpApiService {
    @GET("json/planetary_k_index_1m.json")
    Call<List<KpIndex>> getKpIndex();
}