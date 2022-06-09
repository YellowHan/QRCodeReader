package com.example.qrcodereader

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.qrcodereader.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private val PERMISSIONS_REQUEST_CODE = 1
    private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)
    private var isDetected = false

    override fun onResume() {
        super.onResume()
        isDetected = false
    }

    private fun getImageAnalysis() : ImageAnalysis {

        val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
        val imageAnalysis = ImageAnalysis.Builder().build()

        imageAnalysis.setAnalyzer(cameraExecutor, QRCodeAnalyzer(object : OnDetectListener {
            override fun onDetect(msg: String) {
                if(!isDetected) {
                    isDetected = true

                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(msg)) // 4.
                    intent.putExtra("msg", msg)
                    startActivity(intent)
                }
            }
        }))

        return imageAnalysis
    }


   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

       binding = ActivityMainBinding.inflate(layoutInflater)
       val view = binding.root
       setContentView(view)

       if(!hasPermissions(this)) {
           requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
       } else {
           startCamera()
       }
    }

    private fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) ==
                PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSIONS_REQUEST_CODE) {
            if(PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()) {
                Toast.makeText(this@MainActivity, "권한 요청이 승인되었습니다.", Toast.LENGTH_LONG).show()
                startCamera()
            } else {
                Toast.makeText(this@MainActivity, "권한 요청이 거부되었습니다.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = getPreview()
            val imageAnalysis = getImageAnalysis()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)

        }, ContextCompat.getMainExecutor(this))
    }

    private fun getPreview() : Preview {
        val preview : Preview = Preview.Builder().build()
        preview.setSurfaceProvider(binding.barcodePreview.surfaceProvider)
        return preview
    }
}