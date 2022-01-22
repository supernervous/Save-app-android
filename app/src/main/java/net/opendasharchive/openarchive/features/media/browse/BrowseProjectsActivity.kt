package net.opendasharchive.openarchive.features.media.browse

import android.R
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import net.opendasharchive.openarchive.databinding.ActivityBrowseProjectsBinding
import net.opendasharchive.openarchive.util.Constants
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

class BrowseProjectsActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityBrowseProjectsBinding
    private lateinit var viewModel: BrowseProjectsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityBrowseProjectsBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(
            this,
            BrowseProjectsViewModelProvider(requireNotNull(application))
        ).get(BrowseProjectsViewModel::class.java)
        setContentView(mBinding.root)
        initView()
        registerObservable()
    }

    private fun initView() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = Constants.EMPTY_STRING
        mBinding.rvFolderList.layoutManager = LinearLayoutManager(this)
        viewModel.getFileList()
    }

    private fun setupProjectList(fileList: ArrayList<File>) {
        val adapter = BrowseProjectsAdapter(fileList) { fileName ->
            try {
                viewModel.saveProjectIfNotExist(URLDecoder.decode(fileName, "UTF-8"))
                setResult(RESULT_OK)
                finish()
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        }
        mBinding.rvFolderList.adapter = adapter
    }

    private fun registerObservable() {
        viewModel.fileList.observe(this, Observer {
            setupProjectList(it)
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}