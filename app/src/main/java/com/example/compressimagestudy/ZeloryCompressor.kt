package com.example.compressimagestudy
import android.content.Context
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.size
import java.io.File
class ZeloryCompressor {
    suspend fun compressFile(context: Context, file: File, imageQuality: Int): File {
        return Compressor
            .compress(context, file){
                quality(imageQuality)
            }
    }
}