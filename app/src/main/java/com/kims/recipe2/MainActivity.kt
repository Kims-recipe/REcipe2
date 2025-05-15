package com.kims.recipe2

import com.google.gson.Gson
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.kims.recipe2.ml.FoodDetector
import com.kims.recipe2.ui.food.FoodDetectActivity
import com.kims.recipe2.util.uploadPhoto
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val btn = findViewById<Button>(R.id.startFoodDetectBtn)
        btn.setOnClickListener {
            val intent = Intent(this, FoodDetectActivity::class.java)
            startActivity(intent)
        }

        // 인셋 처리 (기존 코드 유지)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // 알림 권한 (Android 13+)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        // 카메라 권한
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 10)
        }

        // 최초 실행 시 CameraFragment 로 진입 (간단 네비게이션 대체)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CameraFragment())
                .commit()
        }
    }

    /** CameraFragment 가 호출할 함수 — 앞으로 FoodDetector·Storage 업로드로 확장 */
    fun uploadAndNavigate(file: File) {
        lifecycleScope.launch {
            // 1. FoodDetector
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val detector = FoodDetector()
            val detected = detector.detect(bitmap)            // [{code, name, conf}]

            // 2. Storage 업로드
            val photoUrl = uploadPhoto(file)                  // util 함수

            // 3. MealEditFragment 로 네비게이션
            val bundle = bundleOf(
                "photoUrl" to photoUrl,
                "detected" to Gson().toJson(detected)        // List 직렬화
            )
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MealEditFragment::class.java, bundle)
                .addToBackStack(null)
                .commit()
        }
    }
}
