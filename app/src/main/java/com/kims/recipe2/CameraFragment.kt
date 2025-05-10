//package com.kims.recipe2
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.Bundle
//import android.util.Log
//import android.view.View
//import android.widget.Toast
//import androidx.camera.core.*
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import com.kims.recipe2.databinding.FragmentCameraBinding
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import java.io.File
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//
//class CameraFragment : Fragment(R.layout.fragment_camera) {
//
//    private var _bind: FragmentCameraBinding? = null
//    private val bind get() = _bind!!
//
//    private var imageCapture: ImageCapture? = null
//    private lateinit var cameraExecutor: ExecutorService
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        _bind = FragmentCameraBinding.bind(view)
//        cameraExecutor = Executors.newSingleThreadExecutor()
//
//        if (allPermissionsGranted()) startCamera() else
//            requestPermissions(REQUIRED_PERMISSIONS, 10)
//
//        bind.captureBtn.setOnClickListener { takePhoto() }
//    }
//
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//            val preview = Preview.Builder().build().also {
//                it.setSurfaceProvider(bind.previewView.surfaceProvider)
//            }
//            imageCapture = ImageCapture.Builder().build()
//
//            cameraProvider.unbindAll()
//            cameraProvider.bindToLifecycle(
//                viewLifecycleOwner,
//                CameraSelector.DEFAULT_BACK_CAMERA,
//                preview,
//                imageCapture
//            )
//        }, ContextCompat.getMainExecutor(requireContext()))
//    }
//
//    private fun takePhoto() {
//        val imgCap = imageCapture ?: return
//        val photoFile = File(requireContext().cacheDir,
//            "meal_${System.currentTimeMillis()}.jpg")
//
//        val output = ImageCapture.OutputFileOptions.Builder(photoFile).build()
//
//        imgCap.takePicture(
//            output,
//            cameraExecutor,
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onImageSaved(r: ImageCapture.OutputFileResults) {
//                    lifecycleScope.launch(Dispatchers.Main) {
//                        (activity as? MainActivity)?.uploadAndNavigate(photoFile)
//                    }
//                }
//                override fun onError(e: ImageCaptureException) {
//                    Toast.makeText(requireContext(), "촬영 실패", Toast.LENGTH_SHORT).show()
//                }
//            }
//        )   // ← takePicture 닫는 괄호
//    }
//
//
//    private fun allPermissionsGranted(): Boolean =
//        REQUIRED_PERMISSIONS.all {
//            ContextCompat.checkSelfPermission(requireContext(), it) ==
//                    PackageManager.PERMISSION_GRANTED
//        }
//
//    companion object {
//        private val REQUIRED_PERMISSIONS =
//            mutableListOf(
//                Manifest.permission.CAMERA
//            ).apply {
//                if (Build.VERSION.SDK_INT <= 28)
//                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//            }.toTypedArray()
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _bind = null
//        cameraExecutor.shutdown()
//    }
//}
