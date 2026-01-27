package com.royce.zyora.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.royce.zyora.data.models.HydrationEntry
import com.royce.zyora.databinding.ItemHydrationBinding
import java.text.SimpleDateFormat
import java.util.*

class HydrationAdapter(
    private val entries: List<HydrationEntry>,
    private val onEntryDelete: (HydrationEntry) -> Unit
) : RecyclerView.Adapter<HydrationAdapter.HydrationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HydrationViewHolder {
        val binding = ItemHydrationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HydrationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HydrationViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    inner class HydrationViewHolder(
        private val binding: ItemHydrationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: HydrationEntry) {
            try {
                binding.apply {
                    // Set amount and container type
                    tvAmount.text = "${entry.amount}ml"
                    tvContainerType.text = entry.containerType.displayName
                    
                    // Set time
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    tvTime.text = timeFormat.format(Date(entry.timestamp))
                    
                    // Set container icon color based on type
                    val iconColor = when (entry.containerType) {
                        com.royce.zyora.data.models.ContainerType.GLASS ->
                            android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#00BCD4")
                            )
                        com.royce.zyora.data.models.ContainerType.BOTTLE ->
                            android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#66B2D6")
                            )
                        com.royce.zyora.data.models.ContainerType.CUP ->
                            android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#4CAF50")
                            )
                        com.royce.zyora.data.models.ContainerType.LARGE_BOTTLE ->
                            android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#4A90B8")
                            )
                        com.royce.zyora.data.models.ContainerType.CUSTOM ->
                            android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#5A7A94")
                            )
                    }
                    
                    ivWaterIcon.imageTintList = iconColor
                    
                    // Handle delete button
                    btnDelete.setOnClickListener {
                        onEntryDelete(entry)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HydrationAdapter", "Error binding view holder", e)
            }
        }
    }
}
