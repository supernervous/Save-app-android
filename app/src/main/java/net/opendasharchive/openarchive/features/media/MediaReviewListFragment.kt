package net.opendasharchive.openarchive.features.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentMediaListSimpleBinding
import net.opendasharchive.openarchive.db.Media
import net.opendasharchive.openarchive.db.MediaAdapter
import net.opendasharchive.openarchive.features.media.list.MediaListFragment
import java.util.*

open class MediaReviewListFragment : MediaListFragment() {

    private var mStatus = Media.Status.Local
    private var _mBinding: FragmentMediaListSimpleBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _mBinding = FragmentMediaListSimpleBinding.inflate(layoutInflater, container, false)
        return _mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLayout()
    }

    private fun initLayout() {
        _mBinding?.let { mBinding ->
            mBinding.root.tag = TAG

            mBinding.recyclerview.layoutManager = LinearLayoutManager(activity)
            mBinding.recyclerview.setHasFixedSize(true)

            val media = ArrayList(Media.getByStatus(listOf(mStatus), Media.ORDER_PRIORITY))
            val mediaAdapter = MediaAdapter(requireActivity(),
                R.layout.activity_media_list_row,
                media,
                mBinding.recyclerview,
                object : OnStartDragListener {
                    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder?) {
                        //NO-OP
                    }
                }, onDelete = {

                }, onUpload = {

                }
            )

            mMediaAdapter = mediaAdapter

            mediaAdapter.setDoImageFade(false)
            mBinding.recyclerview.adapter = mediaAdapter
        }
    }

    override fun setStatus(status: Media.Status) {
        mStatus = status
    }

    override fun refresh() {
        if (mMediaAdapter != null) {
            val listMedia = Media.getByStatus(listOf(mStatus), Media.ORDER_PRIORITY)
            val listMediaArray = ArrayList(listMedia)
            mMediaAdapter?.updateData(listMediaArray)
        }
    }
}