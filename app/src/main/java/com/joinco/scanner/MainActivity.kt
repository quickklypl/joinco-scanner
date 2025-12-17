package com.joinco.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var tvStatus: TextView
    private lateinit var tvBarcode: TextView
    private lateinit var btnRestart: Button
    
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null
    private var isScanning = true
    private var lastScannedCode: String? = null
    
    companion object {
        private const val TAG = "JoincoScanner"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val JOINCO_URL = "https://joinco.qalcwise.com/AppInstance/Create/0-App-COURIER_DELIVERY?BARCODE="
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Handle intent first
            handleIntent(intent)
            
            setContentView(R.layout.activity_main)
            
            viewFinder = findViewById(R.id.viewFinder)
            tvStatus = findViewById(R.id.tvStatus)
            tvBarcode = findViewById(R.id.tvBarcode)
            btnRestart = findViewById(R.id.btnRestart)
            
            cameraExecutor = Executors.newSingleThreadExecutor()
            
            btnRestart.setOnClickListener {
                restartScanning()
            }

            if (allPermissionsGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "onCreate error", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.let { 
            handleIntent(it)
            // Restart scanning when app is reopened via intent
            if (::tvStatus.isInitialized) {
                restartScanning()
            }
        }
    }
    
    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val data = intent.data
        
        Log.d(TAG, "Intent received - Action: $action, Data: $data")
        
        // Log for debugging
        if (data != null) {
            Log.d(TAG, "Deep link opened: $data")
        }
        
        if (Intent.ACTION_VIEW == action && data != null) {
            Log.d(TAG, "App opened via deep link")
            Toast.makeText(this, "Scanner opened", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.surfaceProvider)
                    }

                imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                            onBarcodeDetected(barcode)
                        })
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
                
                Log.d(TAG, "Camera started successfully")
                
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                runOnUiThread {
                    tvStatus.text = "Camera error: ${exc.message}"
                    Toast.makeText(this, "Camera failed: ${exc.message}", Toast.LENGTH_LONG).show()
                }
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun onBarcodeDetected(barcode: Barcode) {
        if (!isScanning) return
        
        val rawValue = barcode.rawValue ?: return
        
        if (rawValue == lastScannedCode) return
        lastScannedCode = rawValue
        
        isScanning = false
        
        runOnUiThread {
            tvStatus.text = "✓ Barcode detected!"
            tvBarcode.text = rawValue
            
            try {
                val vibrator = getSystemService(android.os.Vibrator::class.java)
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (e: Exception) {
                Log.e(TAG, "Vibration error", e)
            }
            
            viewFinder.postDelayed({
                openJoincoUrl(rawValue)
            }, 500)
        }
    }

    private fun openJoincoUrl(barcode: String) {
        val url = JOINCO_URL + Uri.encode(barcode)
        Log.d(TAG, "Opening URL: $url")
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error opening URL", e)
            Toast.makeText(this, "Error opening JOINCO: ${e.message}", Toast.LENGTH_SHORT).show()
            restartScanning()
        }
    }

    private fun restartScanning() {
        isScanning = true
        lastScannedCode = null
        tvStatus.text = "Scanning..."
        tvBarcode.text = "Position barcode in frame"
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private inner class BarcodeAnalyzer(
        private val onBarcodeDetected: (Barcode) -> Unit
    ) : ImageAnalysis.Analyzer {
        
        private val scanner = BarcodeScanning.getClient()

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                try {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                onBarcodeDetected(barcode)
                            }
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "Barcode scanning failed", it)
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Image processing error", e)
                    imageProxy.close()
                }
            } else {
                imageProxy.close()
            }
        }
    }
}