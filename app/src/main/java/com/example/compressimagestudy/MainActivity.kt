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
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.quality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var compressedImage: String? = null
    private var compressedZelory: File? = null
    private var notCompressedImage: String? = null
    private val nativeCompressor = NativeCompressor()
    private val utils = Utils()

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
                            compressedZelory = Compressor.compress(this@MainActivity, imageUri) {
                                quality(imageSizePercent) // Adjust the quality as needed
                            }
                            compressedZelory?.copyTo(compressedOutputFile, overwrite = true)
                            compressedImage = compressedOutputFile.absolutePath
                            // Display the images
                            binding.imageViewNormal.setImageBitmap(BitmapFactory.decodeFile(imageUri.absolutePath))
                            binding.imageViewCompress.setImageBitmap(BitmapFactory.decodeFile(compressedOutputFile.absolutePath))
                            // Calculate the image sizes
                            val originalSize = utils.getImageSizeInKB(imageUri)
                            val compressedSize = utils.getImageSizeInKB(compressedOutputFile)
                            // Display the image sizes
                            binding.tvNormalImageSize.text = "Tamanho da imagem original: $originalSize KB"
                            binding.tvCompressedImageSize.text = "Tamanho da imagem comprimida: $compressedSize KB"

                            // Save the images to the gallery
                            saveImageToGallery(imageUri, "original_image.jpg")
                            saveImageToGallery(compressedOutputFile, "compressed_image.jpg")
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

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
                    notCompressedImage = nativeCompressor.compressImageFile(originalFile, 1.0, notCompressedOutputFile)
                    binding.imageViewNormal.setImageBitmap(BitmapFactory.decodeFile(notCompressedOutputFile.absolutePath))

                    // Compress the image according to the user input
                    val compressedOutputFile = File(cacheDir, "compressed_image.jpg")
                    compressedImage = nativeCompressor.compressImageFile(originalFile, imageSizePercent, compressedOutputFile)
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
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
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

