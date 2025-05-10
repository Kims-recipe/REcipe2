//package com.kims.recipe2.ml
//
//import android.graphics.Bitmap
//import androidx.core.graphics.scale
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.label.ImageLabeler
//import com.google.mlkit.vision.label.ImageLabeling
//import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
//import kotlinx.coroutines.suspendCancellableCoroutine
//import kotlin.coroutines.resume
//
//data class DetectedFood(
//    val code: String,       // FOOD_CD (임시 = label.name)
//    val name: String,       // 라벨 이름
//    val confidence: Float
//)
//
//class FoodDetector {
//
//    private val labeler: ImageLabeler by lazy {
//        ImageLabeling.getClient(
//            ImageLabelerOptions.Builder()
//                .setConfidenceThreshold(0.5f)
//                .build()
//        )
//    }
//
//    /** Bitmap → 라벨 리스트 */
//    suspend fun detect(bmp: Bitmap): List<DetectedFood> =
//        suspendCancellableCoroutine { cont ->
//            // 기본 모델은 224×224 권장 (없어도 되지만 리사이즈로 속도 ↑)
//            val img = InputImage.fromBitmap(bmp.scale(224, 224, false), 0)
//            labeler.process(img)
//                .addOnSuccessListener { labels ->
//                    val res = labels.map {
//                        // 라벨명을 그대로 코드·이름에 넣고 confidence 전달
//                        DetectedFood(it.text, it.text, it.confidence)
//                    }
//                    cont.resume(res)
//                }
//                .addOnFailureListener { cont.resume(emptyList()) }
//        }
//}
