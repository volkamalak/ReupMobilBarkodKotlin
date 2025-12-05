package com.example.barkodapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.barkodapp.databinding.ItemBarkodBinding

class BarkodAdapter(
    private val barkodList: MutableList<String>,
    private val onDeleteClick: (String, Int) -> Unit
) : RecyclerView.Adapter<BarkodAdapter.BarkodViewHolder>() {

    inner class BarkodViewHolder(private val binding: ItemBarkodBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(barkod: String, position: Int) {
            binding.tvBarkodNo.text = "Barkod: $barkod"

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
