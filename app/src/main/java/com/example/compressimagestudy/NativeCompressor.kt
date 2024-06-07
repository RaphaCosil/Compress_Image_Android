package com.example.compressimagestudy

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class NativeCompressor {
    private val utils = Utils()
        fun compressImageFile(imageFile: File, percentage: Double, outputFile: File): String {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeStream(FileInputStream(imageFile), null, options)

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            val previewWidth = (originalWidth * percentage).toInt()
            val previewHeight = (originalHeight * percentage).toInt()

            options.inJustDecodeBounds = false
            options.inSampleSize = utils.calculateInSampleSize(options, previewWidth, previewHeight)

            val bitmap = BitmapFactory.decodeStream(FileInputStream(imageFile), null, options)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap!!, previewWidth, previewHeight, true)

            val byteArrayOutputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)

            val bytes = byteArrayOutputStream.toByteArray()
            FileOutputStream(outputFile).use {
                it.write(bytes)
            }

            return Base64.encodeToString(bytes, Base64.DEFAULT)
        }
    }