package com.example.diplom.data.model

import com.google.gson.annotations.SerializedName

data class MissingGroup(
    @SerializedName("group_id") val groupId: Int,
    @SerializedName("group_number") val groupNumber: Int
)

