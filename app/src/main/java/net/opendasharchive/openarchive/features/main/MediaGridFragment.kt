package net.opendasharchive.openarchive.features.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.databinding.FragmentMediaListBinding
import net.opendasharchive.openarchive.databinding.ViewSectionBinding
import net.opendasharchive.openarchive.db.*
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.publish.MediaListFragment
import net.opendasharchive.openarchive.util.extensions.toggle
import java.text.DateFormat
import java.text.NumberFormat
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.listOf
import kotlin.collections.set

class MediaGridFragment : MediaListFragment() {

    private val numberOfColumns = 4
    private var mAdapters = HashMap<Long, MediaAdapter>()
    private var mSection = HashMap<Long, SectionViewHolder>()

    private lateinit var mBinding: FragmentMediaListBinding
    private lateinit var viewModel: MediaGridViewModel
    private lateinit var previewViewModel: MediaGridListViewModel

    private val mNf
        get() = NumberFormat.getIntegerInstance()

    private val mDf
        get() = DateFormat.getDateTimeInstance()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentMediaListBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[MediaGridViewModel::class.java]

        previewViewModel =
            MediaGridListViewModel.getInstance(this, requireNotNull(activity?.application))
        previewViewModel.observeValuesForWorkState(activity as AppCompatActivity)

        observeData()
        viewModel.setAllCollection()
        return mBinding.root
    }

    private fun observeData() {
        viewModel.collections.observe(viewLifecycleOwner) {
            initLayout(it)
        }
    }

    private fun initLayout(collections: List<Collection>?) {
        mAdapters = HashMap()
        mSection = HashMap()

        mBinding.root.tag = TAG

        var addedView = false

        for (collection in collections ?: emptyList()) {
            if (collection.projectId != projectId) continue

            val media = collection.media

            if (media.isEmpty()) continue

            if (!addedView) {
                for (view in mBinding.mediaContainer.children) {
                    if (view != mBinding.addMediaHint) {
                        mBinding.mediaContainer.removeView(view)
                    }
                }

                addedView = true
            }

            mBinding.mediaContainer.addView(createMediaList(collection, media))
        }

        mBinding.addMediaHint.toggle(mBinding.mediaContainer.childCount < 2)
    }

    private fun createMediaList(collection: Collection, media: List<Media>): View {
        val holder = SectionViewHolder(ViewSectionBinding.inflate(layoutInflater))

        holder.recyclerView.setHasFixedSize(true)
        holder.recyclerView.layoutManager = GridLayoutManager(activity, numberOfColumns)

        setSectionHeaders(collection, media, holder)

        val mediaAdapter = MediaAdapter(
            requireActivity(),
            { MediaViewHolder.Box(it) },
            ArrayList(media),
            holder.recyclerView,
            object : OnStartDragListener {
                override fun onStartDrag(viewHolder: RecyclerView.ViewHolder?) {

                }
            }, checkSelecting = {
                (activity as? MainActivity)?.toggleDelete(mAdapters.values.firstOrNull { it.selecting } != null)

                // TODO: Update section header.
            })

        holder.recyclerView.adapter = mediaAdapter
        mAdapters[collection.id] = mediaAdapter
        mSection[collection.id] = holder

        return holder.root
    }

    override fun updateItem(mediaId: Long, progress: Long) {
        for (adapter in mAdapters.values) adapter.updateItem(mediaId, progress)
    }

    override fun refresh() {
        Collection.getByProject(projectId).forEachIndexed { index, collection ->
            val media = if (collection.projectId == projectId) collection.media else listOf()

            val adapter = mAdapters[collection.id]
            val holder = mSection[collection.id]

            if (adapter != null) {
                adapter.updateData(ArrayList(media))
                if (holder != null) setSectionHeaders(collection, media, holder)
            }
            else if (media.isNotEmpty()) {
                val view = createMediaList(collection, media)

                mBinding.mediaContainer.addView(view, 0)
            }

            holder?.bottomSpacing?.toggle(index == mSection.size - 1)
        }

        mBinding.addMediaHint.toggle(mBinding.mediaContainer.childCount < 2)
    }

    fun deleteSelected() {
        mAdapters.values.forEach { adapter ->
            adapter.deleteSelected()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setSectionHeaders(
        collection: Collection,
        media: List<Media>,
        holder: SectionViewHolder
    ) {
        if (media.firstOrNull { it.sStatus == Media.Status.Queued
                    || it.sStatus == Media.Status.Uploading
                    || it.sStatus == Media.Status.Error } != null)
        {
            val uploaded = mNf.format(media.filter {
                it.sStatus == Media.Status.Uploaded
                        || it.sStatus == Media.Status.DeleteRemote
                        || it.sStatus == Media.Status.Published }.size)

            val total = mNf.format(media.size)

            holder.count.text = "${uploaded}/${total}"
        }
        else {
            holder.count.text = mNf.format(media.size)
        }

        val uploadDate = collection.uploadDate

        if (uploadDate != null) {
            holder.timestamp.text = mDf.format(uploadDate)
        }
        else {
            holder.timestamp.text = ""
        }
    }
}
