package net.opendasharchive.openarchive.features.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.opendasharchive.openarchive.MainActivity
import net.opendasharchive.openarchive.databinding.FragmentNewFolderBinding

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NewProjectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
class NewFolderFragment : Fragment() {

    private lateinit var mBinding: FragmentNewFolderBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mBinding = FragmentNewFolderBinding.inflate(inflater, container, false)

        mBinding.tvAddProject.setOnClickListener {
            (activity as? MainActivity)?.addProject()
        }

        return mBinding.root
    }
}