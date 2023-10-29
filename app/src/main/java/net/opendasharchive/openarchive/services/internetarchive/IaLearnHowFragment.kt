package net.opendasharchive.openarchive.services.internetarchive

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentIaLearnHowBinding

class IaLearnHowFragment : BottomSheetDialogFragment() {
    private lateinit var mBinding: FragmentIaLearnHowBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        // make sure this bottom sheet is expanded on start
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED;
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentIaLearnHowBinding.inflate(inflater)

        mBinding.viewPager.adapter = LearnHowAdapter(
            requireActivity().supportFragmentManager,
            requireActivity().lifecycle,
            requireActivity()
        )
        mBinding.dotsIndicator.attachTo(mBinding.viewPager)

        mBinding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (mBinding.viewPager.currentItem + 1 < mBinding.viewPager.adapter!!.itemCount) {
                    mBinding.nextButton.setText(R.string.next)
                } else {
                    mBinding.nextButton.setText(R.string.done)
                }
            }
        })

        mBinding.nextButton.setOnClickListener {
            if (mBinding.viewPager.currentItem + 1 < mBinding.viewPager.adapter!!.itemCount) {
                mBinding.viewPager.currentItem++
            } else {
                dismiss()
            }
        }

        return mBinding.root
    }

    class LearnHowAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        context: Context
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int {
            return 3
        }

        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return IaLearnHowStepFragment.newInstance(
                    R.string.ia_learn_how_summary_step_1,
                    R.drawable.ia_learn_how_illustration1
                )

                1 -> return IaLearnHowStepFragment.newInstance(
                    R.string.ia_learn_how_summary_step_2,
                    R.drawable.ia_learn_how_illustration2
                )

                2 -> return IaLearnHowStepFragment.newInstance(
                    R.string.ia_learn_how_summary_step_3,
                    R.drawable.ia_learn_how_illustration3
                )
            }
            throw IndexOutOfBoundsException()
        }
    }
}