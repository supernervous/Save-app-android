package net.opendasharchive.openarchive.features.media
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.FragmentNavigationDrawerBinding

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
class NavigationDrawerFragment : Fragment() {

    /**
     * Remember the position of the selected item.
     */
    private val STATE_SELECTED_POSITION = "selected_navigation_drawer_position"

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private var mCallbacks: NavigationDrawerCallbacks? = null

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private val mDrawerToggle: ActionBarDrawerToggle? = null

    private var mCurrentSelectedPosition = 0
    private var mFromSavedInstanceState = false
    private var mDrawerLayout: DrawerLayout? = null
    private var mFragmentContainerView: View? = null

    private var _mBinding: FragmentNavigationDrawerBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _mBinding = FragmentNavigationDrawerBinding.inflate(inflater, container, false)
        return _mBinding?.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION)
            mFromSavedInstanceState = true
        }
        selectItem(mCurrentSelectedPosition)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLayout()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle?.onConfigurationChanged(newConfig)
    }

    private fun initLayout() {
        _mBinding?.let { mBinding ->
            mBinding.drawerList.setOnItemClickListener { parent, view, position, id ->
                selectItem(position)
            }

            getActionBar()?.let {
                mBinding.drawerList.adapter = ArrayAdapter(
                    it.themedContext,
                    android.R.layout.simple_list_item_activated_1,
                    android.R.id.text1,
                    resources.getStringArray(R.array.ar_title_menu_sections)
                )
            }

            mBinding.drawerList.setItemChecked(mCurrentSelectedPosition, true)
        }
    }

    private fun selectItem(position: Int) {
        mCurrentSelectedPosition = position
        _mBinding?.drawerList?.setItemChecked(position, true)
        mDrawerLayout?.closeDrawer(mFragmentContainerView!!)
        mCallbacks?.onNavigationDrawerItemSelected(position)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mCallbacks = try {
            activity as NavigationDrawerCallbacks
        } catch (e: ClassCastException) {
            throw ClassCastException("Activity must implement NavigationDrawerCallbacks.")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mCallbacks = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (mDrawerToggle?.onOptionsItemSelected(item) == true) {
            true
        } else super.onOptionsItemSelected(item)
    }

    private fun getActionBar(): ActionBar? {
        return (activity as AppCompatActivity).supportActionBar
    }

    interface NavigationDrawerCallbacks {
        fun onNavigationDrawerItemSelected(position: Int)
    }
}