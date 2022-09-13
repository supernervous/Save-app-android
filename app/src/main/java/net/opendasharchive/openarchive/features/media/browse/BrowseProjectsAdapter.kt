package net.opendasharchive.openarchive.features.media.browse

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.databinding.RvSimpleRowBinding
import java.io.File

class BrowseProjectsAdapter(
    private val fileList: MutableList<File> = mutableListOf(),
    private val onClick: (fileName: String) -> Unit
) : RecyclerView.Adapter<BrowseProjectsAdapter.BrowseProjectsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrowseProjectsViewHolder {
        val binding = RvSimpleRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BrowseProjectsViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: BrowseProjectsViewHolder, position: Int) {
        holder.onBindView(fileList[position].name)
    }

    override fun getItemCount(): Int {
        return fileList.size
    }

    inner class BrowseProjectsViewHolder(val binding: RvSimpleRowBinding, val onClick: (fileName: String) -> Unit) :
        RecyclerView.ViewHolder(binding.root){
        fun onBindView(fileName: String) {
            binding.rvRowTitle.text = fileName
            binding.root.setOnClickListener {
                onClick.invoke(fileName)
            }
        }
    }
}