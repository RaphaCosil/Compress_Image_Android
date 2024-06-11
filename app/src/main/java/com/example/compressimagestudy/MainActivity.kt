package com.example.compressimagestudy

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.compressimagestudy.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    //This variable will be used to launch the activity
    private val nativeCompressor = NativeCompressor()
    private val zeloryCompressor = ZeloryCompressor()
    private val utils = Utils()
    //These variables will be used to launch the activity
    private var compressedImage: String? = null
    private var compressedZelory: File? = null
    private var notCompressedImage: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonLoadNative.setOnClickListener {
            val imageSize = binding.imageSize.text
            if (imageSize != null) {
                try {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    pickImageNativeCompressor.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Insert a number 0-100", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.buttonLoadZelory.setOnClickListener {
            val imageSize = binding.imageSize.text
            if (imageSize != null) {
            try{
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                pickImageZeloryCompressor.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Insert a number 0-100", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    //**//**// This function will launch the Zelory lib compressor in a activity //**//**//
    @SuppressLint("SetTextI18n")
    private val pickImageZeloryCompressor: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            if (it.data != null) {
                val imageUri = it.data?.data?.let { uri -> uriToFile(uri) }
                if (imageUri != null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val imageSizePercent = (binding.imageSize.text.toString()).toInt()
                            val compressedOutputFile = File(cacheDir, "compressed_image.jpg")
                            // Compress the image
                            compressedZelory = zeloryCompressor.compressFile(
                                this@MainActivity, imageUri, imageSizePercent)

                            compressedZelory?.copyTo(compressedOutputFile, overwrite = true)
                            compressedImage = compressedOutputFile.absolutePath
                            // Display the images
                            binding.imageViewNormal.setImageBitmap(BitmapFactory.decodeFile(imageUri.absolutePath))
                            binding.imageViewCompress.setImageBitmap(BitmapFactory.decodeFile(compressedOutputFile.absolutePath))
                            // Calculate the image sizes
                            val originalSize = utils.getImageSizeInKB(imageUri)
                            val compressedSize = utils.getImageSizeInKB(compressedOutputFile)
                            // Display the image sizes
                            binding.tvNormalImageSize.text = "Original Image Size: $originalSize KB"
                            binding.tvCompressedImageSize.text = "Compressed Image Size: $compressedSize KB"

                            // Save the images to the gallery
                            saveImageToGallery(compressedOutputFile, "compressed_image.jpg")
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
    //**//**// This function will launch the native compressor in a activity //**//**//
    @SuppressLint("SetTextI18n")
    private val pickImageNativeCompressor: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            if (it.data != null) {
                val imageUri = it.data?.data
                try {
                    // Load the original image
                    val imageSizePercent = (binding.imageSize.text.toString()).toDouble() / 100
                    val inputStream = imageUri?.let { uri -> contentResolver.openInputStream(uri) }
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    // Save the original image to a temporary file
                    val originalFile = File(cacheDir, "original_image.jpg")
                    val fos = FileOutputStream(originalFile)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    fos.flush()
                    fos.close()

                    // Compress the original image
                    val notCompressedOutputFile = File(cacheDir, "not_compressed_image.jpg")
                    notCompressedImage = nativeCompressor.compressImageFile(originalFile, 1.0, 100, notCompressedOutputFile)
                    binding.imageViewNormal.setImageBitmap(BitmapFactory.decodeFile(notCompressedOutputFile.absolutePath))

                    // Compress the image according to the user input
                    val compressedOutputFile = File(cacheDir, "compressed_image.jpg")
                    compressedImage = nativeCompressor.compressImageFile(originalFile, imageSizePercent, 85,compressedOutputFile)
                    binding.imageViewCompress.setImageBitmap(BitmapFactory.decodeFile(compressedOutputFile.absolutePath))

                    // Show the sizes of the images
                    val originalSize = utils.getImageSizeInKB(BitmapFactory.decodeFile(originalFile.absolutePath))
                    val compressedSize = utils.getImageSizeInKB(BitmapFactory.decodeFile(compressedOutputFile.absolutePath))
                    binding.tvNormalImageSize.text = "Original Image Size: $originalSize KB"
                    binding.tvCompressedImageSize.text = "Compressed Image Size: $compressedSize KB"

                    // Save the images to the gallery
                    saveImageToGallery(originalFile, "original_image.jpg")
                    saveImageToGallery(compressedOutputFile, "compressed_image.jpg")
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }

    //**//**// Helper functions //**//**//
    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri) ?: throw FileNotFoundException("Unable to open URI: $uri")
        val tempFile = File(cacheDir, "temp_image")
        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    private fun saveImageToGallery(imageFile: File, imageName: String) {
        val timestamp = System.currentTimeMillis()
        val uniqueImageName = "${timestamp}_$imageName"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, uniqueImageName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CompressedImages")
        }

        val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
            outputStream?.use {
                imageFile.inputStream().use { input ->
                    input.copyTo(it)
                }
            }
        }
    }

}