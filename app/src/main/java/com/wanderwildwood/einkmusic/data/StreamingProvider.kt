package com.wanderwildwood.einkmusic.data

enum class StreamingProvider {
    YOUTUBE,
    ;

    companion object {
        fun fromStored(value: String?): StreamingProvider = YOUTUBE

        fun toStored(provider: StreamingProvider): String = "YOUTUBE"
    }
}