package com.tobeygronow.android.greenspot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tobeygronow.android.greenspot.databinding.ListItemPlantBinding
import java.text.DateFormat
import java.util.UUID

class PlantHolder(
    val binding: ListItemPlantBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(plant: Plant, onPlantClicked: (plantId: UUID) -> Unit) {
        binding.plantTitle.text = plant.title

        val df = DateFormat.getDateInstance(DateFormat.LONG)
        binding.plantDate.text = df.format(plant.date)

        binding.plantPlace.text = plant.place

        binding.root.setOnClickListener {
            onPlantClicked(plant.id)
        }

//        binding.plantSolved.visibility = if (plant.isSolved) {
//            View.VISIBLE
//        } else {
//            View.GONE
//        }
    }
}

class PlantListAdapter(
    private val plants: List<Plant>,
    private val onPlantClicked: (plantId: UUID) -> Unit
) : RecyclerView.Adapter<PlantHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) : PlantHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemPlantBinding.inflate(inflater, parent, false)
        return PlantHolder(binding)
    }
    override fun onBindViewHolder(holder: PlantHolder, position: Int) {
        val plant = plants[position]

        holder.bind(plant, onPlantClicked)
    }
    override fun getItemCount() = plants.size
}