package net.opendasharchive.openarchive.features.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.databinding.FragmentMediaListSimpleBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.MediaAdapter
import net.opendasharchive.openarchive.db.MediaViewHolder
import net.opendasharchive.openarchive.features.media.list.MediaListFragment

open class MediaReviewListFragment : MediaListFragment() {

    private var mStatus = Media.Status.Local
    private lateinit var mBinding: FragmentMediaListSimpleBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FragmentMediaListSimpleBinding.inflate(layoutInflater, container, false)

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.root.tag = TAG

        mBinding.recyclerView.layoutManager = LinearLayoutManager(activity)
        mBinding.recyclerView.setHasFixedSize(true)

        mediaAdapter = MediaAdapter(requireActivity(),
            { MediaViewHolder.BigRow(it) },
            ArrayList(),
            mBinding.recyclerView,
            object : OnStartDragListener {
                override fun onStartDrag(viewHolder: RecyclerView.ViewHolder?) {
                    //NO-OP
                }
            }, onDelete = {
            }, onUpload = {
            }
        )

        mediaAdapter?.doImageFade = false
        mBinding.recyclerView.adapter = mediaAdapter

        refresh()
    }

    override fun refresh() {
        mediaAdapter?.updateData(ArrayList(Media.getByStatus(listOf(mStatus), Media.ORDER_PRIORITY)))
    }
}