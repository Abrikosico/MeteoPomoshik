package com.model;

import com.google.gson.annotations.SerializedName;

public class KpIndex {
    @SerializedName("time_tag")
    public String timeTag;

    @SerializedName("kp_index")
    public int kpIndex;
}