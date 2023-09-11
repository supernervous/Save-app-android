package net.opendasharchive.openarchive.features.media.grid

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentMediaListBinding
import net.opendasharchive.openarchive.databinding.FragmentMediaListSectionBinding
import net.opendasharchive.openarchive.db.*
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.features.media.SectionViewHolder
import net.opendasharchive.openarchive.features.media.list.MediaListFragment
import net.opendasharchive.openarchive.features.media.preview.PreviewMediaListActivity
import net.opendasharchive.openarchive.features.media.preview.PreviewMediaListViewModel
import net.opendasharchive.openarchive.features.media.preview.PreviewMediaListViewModelFactory
import net.opendasharchive.openarchive.util.Prefs
import net.opendasharchive.openarchive.util.extensions.cloak
import net.opendasharchive.openarchive.util.extensions.toggle
import java.text.DateFormat
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.List
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
    private lateinit var previewViewModel: PreviewMediaListViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentMediaListBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[MediaGridViewModel::class.java]

        val context = requireNotNull(activity?.application)
        val viewModelFactory = PreviewMediaListViewModelFactory(context)
        previewViewModel = ViewModelProvider(this, viewModelFactory)[PreviewMediaListViewModel::class.java]
        previewViewModel.observeValuesForWorkState(activity as AppCompatActivity)

        observeData()
        viewModel.getAllCollection()
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
            if (collection.projectId != getProjectId()) continue

            val media = collection.media

            if (media.isEmpty()) continue

            if (!addedView) {
                for (view in mBinding.mediacontainer.children) {
                    if (view != mBinding.addMediaHint) {
                        mBinding.mediacontainer.removeView(view)
                    }
                }

                addedView = true
            }

            mBinding.mediacontainer.addView(createMediaList(collection, media))
        }

        mBinding.addMediaHint.toggle(mBinding.mediacontainer.childCount < 2)
    }

    private fun performBatchUpload(listMedia: List<Media>) {
        for (media in listMedia) {
            media.sStatus = Media.Status.Queued
            media.save()
        }
        previewViewModel.applyMedia()
    }

    private fun createMediaList(collection: Collection, listMedia: List<Media>): View {
        val holder = SectionViewHolder(FragmentMediaListSectionBinding.inflate(layoutInflater))

        holder.apply {
            mediaSection.recyclerview.setHasFixedSize(true)
            mediaSection.recyclerview.layoutManager = GridLayoutManager(activity, numberOfColumns)

            setSectionHeaders(collection, listMedia, this)

            val listMediaArray = ArrayList(listMedia)
            val mediaAdapter = MediaAdapter(
                requireContext(),
                R.layout.activity_media_list_square,
                listMediaArray,
                mediaSection.recyclerview,
                object : OnStartDragListener {
                    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder?) {

                    }
                }, onDelete = {
                    refresh()
                }, onUpload = {
                    if (Space.current?.tType == Space.Type.WEBDAV) {
                        // TODO: WTF is this DUPLICATED special casing here?!?
                        if (Space.current?.host?.contains("https://sam.nl.tab.digital") == true) {
                            val availableSpace = getAvailableStorageSpace(it)
                            val totalUploadsContent = availableSpace.first
                            val totalStorage = availableSpace.second
                            if(totalStorage < totalUploadsContent) {
                                Toast.makeText(activity, getString(R.string.upload_files_error), Toast.LENGTH_SHORT).show()
                            }
                            else {
                                performBatchUpload(it)
                            }
                        }
                        else {
                            //for NON nextcloud providers
                            performBatchUpload(it)
                        }
                    }
                    else {
                        //for non webDAV protocols.
                        performBatchUpload(it)
                    }
                })

            mediaSection.recyclerview.adapter = mediaAdapter
            mAdapters[collection.id] = mediaAdapter
            mSection[collection.id] = this
        }

        return holder.mediaSection.root
    }

    private fun getAvailableStorageSpace(listMedia: List<Media>): Pair<Double, Long> {
        val nextCloudModel = Gson().fromJson(Prefs.nextCloudModel, WebDAVModel::class.java)
        var totalUploadsContent = 0.0
        for (media in listMedia) {
            totalUploadsContent += media.contentLength
        }

        val totalStorage = nextCloudModel.ocs.data.quota.total - nextCloudModel.ocs.data.quota.used
        return Pair(totalUploadsContent, totalStorage)
    }

    override fun updateItem(mediaId: Long, progress: Long) {
        for (adapter in mAdapters.values) adapter.updateItem(mediaId, progress)
    }

    override fun refresh() {
        Collection.getAll().forEach { collection ->
            val media = if (collection.projectId == getProjectId()) collection.media else listOf()

            val adapter = mAdapters[collection.id]
            val holder = mSection[collection.id]

            if (adapter != null) {
                adapter.updateData(ArrayList(media))
                if (holder != null) setSectionHeaders(collection, media, holder)
            }
            else if (media.isNotEmpty()) {
                val view = createMediaList(collection, media)

                mBinding.mediacontainer.addView(view, 0)
            }
        }

        mBinding.addMediaHint.toggle(mBinding.mediacontainer.childCount < 2)
    }

    @SuppressLint("SetTextI18n")
    private fun setSectionHeaders(
        collection: Collection,
        listMedia: List<Media>?,
        holder: SectionViewHolder
    ) {
        holder.sectionStatus.text = ""
        holder.sectionTimestamp.text = ""

        val df = DateFormat.getDateTimeInstance()

        listMedia?.forEach { media ->
            when (media.sStatus) {
                Media.Status.Local -> {
                    holder.sectionStatus.text = getString(R.string.status_ready_to_upload)
                    holder.sectionTimestamp.text = "${listMedia.size} ${getString(R.string.label_items)}"
                    holder.action.cloak()
                    holder.action.setOnClickListener {
                        startActivity(Intent(requireActivity(), PreviewMediaListActivity::class.java))
                    }

                    return@forEach
                }
                Media.Status.Queued, Media.Status.Uploading -> {
                    holder.sectionStatus.text = getString(R.string.header_uploading)

                    val count = listMedia.filter { it.sStatus == Media.Status.Uploaded }.size

                    holder.sectionTimestamp.text = getString(R.string.__out_of___items_uploaded, count, listMedia.size)
                    holder.action.cloak()

                    return@forEach
                }
                Media.Status.Uploaded -> {
                    val count = listMedia.filter { it.sStatus == Media.Status.Uploaded }.size

                    if (count == listMedia.size) {
                        holder.sectionStatus.text = getString(R.string.__items_uploaded, listMedia.size)
                        holder.action.cloak()
                    }
                    else {
                        holder.sectionStatus.text = getString(R.string.__out_of___items_uploaded, count, listMedia.size)
                        holder.action.cloak()
                    }

                    collection.uploadDate?.let { holder.sectionTimestamp.text = df.format(it) }
                }
                Media.Status.Error -> {
                    val count = listMedia.filter { it.sStatus == Media.Status.Error }.size

                    if (count == listMedia.size) {
                        holder.sectionStatus.text = getString(R.string.__items_failed_to_upload, listMedia.size)
                    }
                    else {
                        holder.sectionStatus.text = getString(R.string.__out_of___items_uploaded, count, listMedia.size)
                    }

                    collection.uploadDate?.let { holder.sectionTimestamp.text = df.format(it) }

                    holder.action.cloak()
                }
                else -> {
                    holder.sectionStatus.text = getString(R.string.__items_uploaded, listMedia.size)

                    collection.uploadDate?.let { holder.sectionTimestamp.text = df.format(it) }
                        ?: run {
                            listMedia.firstOrNull()?.uploadDate?.let {
                                holder.sectionTimestamp.text = df.format(it)
                            }
                        }

                    holder.action.cloak()
                }
            }
        }
    }
}
