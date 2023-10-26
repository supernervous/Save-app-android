package net.opendasharchive.openarchive.features.main

import net.opendasharchive.openarchive.databinding.ViewSectionBinding

data class SectionViewHolder(
    private val binding: ViewSectionBinding
) {

    val root
        get() = binding.root

    val timestamp
        get() = binding.timestamp

    val count
        get() = binding.count

    val recyclerView
        get() = binding.recyclerView

    val bottomSpacing
        get() = binding.bottomSpacing
}