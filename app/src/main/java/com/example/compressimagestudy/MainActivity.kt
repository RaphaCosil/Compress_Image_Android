package com.example.compressimagestudy

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.compressimagestudy.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var compressedImage: String? = null
    private var notCompressedImage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        var previewWidth: Int = 0

        val pickImage: ActivityResultLauncher<Intent> = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                if (it.data != null) {
                    val imageUri = it.data?.data
                    try {
                        val inputStream = imageUri?.let { uri -> contentResolver.openInputStream(uri) }
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.imageViewNormal.setImageBitmap(bitmap)
                        notCompressedImage = compressImage(bitmap, bitmap.width)
                        setImage(notCompressedImage!!, binding.imageViewNormal)

                        compressedImage = compressImage(bitmap, previewWidth)
                        setImage(compressedImage!!, binding.imageViewCompress)

                        // Exibir tamanho das imagens
                        val originalSize = getImageSizeInKB(bitmap)
                        val compressedSize = getImageSizeInKB(decodeImage(compressedImage!!))
                        binding.tvNormalImageSize.text = "Tamanho da Imagem Original: $originalSize KB"
                        binding.tvCompressedImageSize.text = "Tamanho da Imagem Comprimida: $compressedSize KB"
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        binding.button.setOnClickListener {
            val tamanhoImagem = binding.tamanhoImg.text

            if(tamanhoImagem != null){
                try{
                    previewWidth = Integer.parseInt(tamanhoImagem.toString())
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    pickImage.launch(intent)
                }catch(e: Exception){
                    Toast.makeText(this, "Insira um valor númerico", Toast.LENGTH_SHORT).show()
                }
            }else{
                    Toast.makeText(this, "Insira um valor", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun compressImage(bitmap: Bitmap, previewWidth: Int): String {
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private fun setImage(encodedImage: String, imageView: ImageView): Bitmap? {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        imageView.setImageBitmap(bitmap)
        return bitmap
    }

    private fun decodeImage(encodedImage: String): Bitmap {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun getImageSizeInKB(bitmap: Bitmap): Int {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return bytes.size / 1024
    }
}