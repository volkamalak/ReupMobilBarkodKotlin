package com.example.barkodapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.barkodapp.databinding.ItemBarkodBinding

class BarkodAdapter(
    private val barkodList: MutableList<String>,
    private val barkodWeights: Map<String, Double> = emptyMap(),
    private val isFactory87: Boolean = false,
    private val onDeleteClick: (String, Int) -> Unit,
    private val onBolClick: ((barkod: String, originalWeight: Double) -> Unit)? = null
) : RecyclerView.Adapter<BarkodAdapter.BarkodViewHolder>() {

    inner class BarkodViewHolder(private val binding: ItemBarkodBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(barkod: String, position: Int) {
            val weight = barkodWeights[barkod]
            if (weight != null && weight > 0) {
                binding.tvBarkodNo.text = "Barkod: $barkod  (%.1f kg)".format(weight)
            } else {
                binding.tvBarkodNo.text = "Barkod: $barkod"
            }

            if (isFactory87 && weight != null && weight > 0) {
                binding.btnBol.visibility = View.VISIBLE
                binding.btnBol.setOnClickListener {
                    onBolClick?.invoke(barkod, weight)
                }
            } else {
                binding.btnBol.visibility = View.GONE
            }

            binding.btnSil.setOnClickListener {
                onDeleteClick(barkod, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarkodViewHolder {
        val binding = ItemBarkodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BarkodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BarkodViewHolder, position: Int) {
        holder.bind(barkodList[position], position)
    }

    override fun getItemCount(): Int = barkodList.size

    fun addBarkod(barkod: String) {
        barkodList.add(barkod)
        notifyItemInserted(barkodList.size - 1)
    }

    fun removeBarkod(position: Int) {
        barkodList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, barkodList.size)
    }

    fun clearAll() {
        val size = barkodList.size
        barkodList.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun getBarkodList(): List<String> = barkodList.toList()
}
