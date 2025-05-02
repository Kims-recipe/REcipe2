package com.kims.recipe2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import com.kims.recipe2.ml.DetectedFood

class FoodEditAdapter(
    private val items: MutableList<DetectedFood>
) : RecyclerView.Adapter<FoodEditAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName = v.findViewById<TextView>(R.id.tvName)
        val slider = v.findViewById<Slider>(R.id.sliderGram)
        val tvGram = v.findViewById<TextView>(R.id.tvGram)
    }

    override fun onCreateViewHolder(p: ViewGroup, vType: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_food_edit, p, false))

    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val it = items[pos]
        h.tvName.text = it.name
        h.slider.addOnChangeListener { _, value, _ ->
            h.tvGram.text = "${value.toInt()}g"
            items[pos] = it.copy(confidence = value)   // grams 값 임시 저장
        }
    }

    fun toItemMap() = items.map {
        mapOf("foodCode" to it.code, "grams" to it.confidence /*여기선 grams*/ )
    }
}
