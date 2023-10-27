package net.opendasharchive.openarchive.features.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import net.opendasharchive.openarchive.databinding.FragmentMainMediaBinding
import net.opendasharchive.openarchive.databinding.ViewSectionBinding
import net.opendasharchive.openarchive.db.Collection
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.MediaAdapter
import net.opendasharchive.openarchive.db.MediaViewHolder
import net.opendasharchive.openarchive.util.extensions.toggle
import kotlin.collections.set

class MainMediaFragment : Fragment() {

    companion object {
        private const val COLUMN_COUNT = 4
        private const val ARG_PROJECT_ID = "project_id"

        fun newInstance(projectId: Long): MainMediaFragment {
            val args = Bundle()
            args.putLong(ARG_PROJECT_ID, projectId)

            val fragment = MainMediaFragment()
            fragment.arguments = args

            return fragment
        }
    }

    private var mAdapters = HashMap<Long, MediaAdapter>()
    private var mSection = HashMap<Long, SectionViewHolder>()
    private var mProjectId = -1L
    private var mCollections = ArrayList<Collection>()

    private lateinit var mBinding: FragmentMainMediaBinding
    private lateinit var mPreviewViewModel: MediaGridListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mProjectId = arguments?.getLong(ARG_PROJECT_ID, -1) ?: -1

        mBinding = FragmentMainMediaBinding.inflate(inflater, container, false)

        mPreviewViewModel =
            MediaGridListViewModel.getInstance(this, requireNotNull(activity?.application))
        mPreviewViewModel.observeValuesForWorkState(activity as AppCompatActivity)

        refresh()

        return mBinding.root
    }

    fun updateItem(mediaId: Long) {
        for (adapter in mAdapters.values) {
            if (adapter.updateItem(mediaId)) break
        }
    }

    fun refresh() {
        mCollections = ArrayList(Collection.getByProject(mProjectId))

        val toDelete = ArrayList<Long>()

        mCollections.forEachIndexed { index, collection ->
            val media = collection.media

            if (media.isEmpty()) {
                toDelete.add(collection.id)
                return@forEachIndexed
            }

            val adapter = mAdapters[collection.id]
            val holder = mSection[collection.id]

            if (adapter != null) {
                adapter.updateData(media)
                holder?.setHeader(collection, media)
            }
            else if (media.isNotEmpty()) {
                val view = createMediaList(collection, media)

                mBinding.mediaContainer.addView(view, 0)
            }

            holder?.bottomSpacing?.toggle(index == mSection.size - 1)
        }

        deleteCollections(toDelete)

        mBinding.addMediaHint.toggle(mCollections.isEmpty())
    }

    fun deleteSelected() {
        val toDelete = ArrayList<Long>()

        mCollections.forEach { collection ->
            if (mAdapters[collection.id]?.deleteSelected() == true) {
                val media = collection.media

                if (media.isEmpty()) {
                    toDelete.add(collection.id)
                }
                else {
                    mSection[collection.id]?.setHeader(collection, media)
                }
            }
        }

        deleteCollections(toDelete)
    }

    private fun createMediaList(collection: Collection, media: List<Media>): View {
        val holder = SectionViewHolder(ViewSectionBinding.inflate(layoutInflater))

        holder.recyclerView.setHasFixedSize(true)
        holder.recyclerView.layoutManager = GridLayoutManager(activity, COLUMN_COUNT)

        holder.setHeader(collection, media)

        val mediaAdapter = MediaAdapter(
            requireActivity(),
            { MediaViewHolder.Box(it) },
            media,
            holder.recyclerView,
            checkSelecting = {
                (activity as? MainActivity)?.updateAfterDelete(mAdapters.values.firstOrNull { it.selecting } == null)
            })

        holder.recyclerView.adapter = mediaAdapter
        mAdapters[collection.id] = mediaAdapter
        mSection[collection.id] = holder

        return holder.root
    }

    private fun deleteCollections(collectionIds: List<Long>) {
        collectionIds.forEach { collectionId ->
            mAdapters.remove(collectionId)

            val holder = mSection.remove(collectionId)
            (holder?.root?.parent as? ViewGroup)?.removeView(holder.root)

            val idx = mCollections.indexOfFirst { it.id == collectionId }

            if (idx > -1 && idx < mCollections.size) {
                val collection = mCollections.removeAt(idx)
                collection.delete()
            }
        }
    }
}
