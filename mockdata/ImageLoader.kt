package com.metromessages.mockdata

import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder

object ImageLoader {
    fun createWithSvgSupport(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }
}