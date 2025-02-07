package com.scottyab.rootbeer.sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.scottyab.rootbeer.RootBeer
import com.scottyab.rootbeer.sample.extensions.hide
import com.scottyab.rootbeer.sample.extensions.show
import com.scottyab.rootbeer.sample.ui.RootItemAdapter
import com.scottyab.rootbeer.sample.ui.ScopedActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ScopedActivity() {

    private var infoDialog: AlertDialog? = null
    private var detailsDialog: AlertDialog? = null
    private val rootItemAdapter = RootItemAdapter()
    private val checkForRoot = CheckForRootWorker(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        resetView()
    }

    private fun initView() {
        setSupportActionBar(toolbar)
        fab.setOnClickListener { checkForRoot() }
        rootResultsRecycler.layoutManager = LinearLayoutManager(this)
        rootResultsRecycler.adapter = rootItemAdapter
    }

    private fun resetView() {
        progressView.max = 100
        progressView.beerProgress = 0
        progressView.show()
        isRootedTextView.hide()
        rootItemAdapter.clear()
    }

    private fun checkForRoot() {
        resetView()
        fab.hide()

        launch {
            val results = checkForRoot.invoke()
            animateResults(results)
        }
    }

    /**
     * There's probably a much easier way of doing this using View Property animators? :S
     */
    private fun animateResults(results: List<RootItemResult>) {
        val isRooted = results.any { it.result }
        // this allows us to increment the progress bar for x number of times for each of the results
        // all in the effort to smooth the animation
        val multiplier = 10
        progressView.max = results.size * multiplier

        launch {
            withContext(Dispatchers.IO) {
                results.forEachIndexed { index, rootItemResult ->
                    for (i in 1..multiplier) {
                        // arbitrary delay, 50 millis seems to look ok when testing with 12 results
                        delay(50)
                        // post the UI updates in the UI thread
                        withContext(Dispatchers.Main) {
                            progressView.beerProgress = progressView.beerProgress + 1

                            // only add to the once we hit the multiplier
                            if (i == multiplier) {
                                rootItemAdapter.add(rootItemResult)
                            }
                            //is it the end of the results
                            if (index == results.size - 1) {
                                onRootCheckFinished(isRooted = isRooted)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_github -> {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(GITHUB_LINK)
                startActivity(i)
                true
            }
            R.id.action_info -> {
                showInfoDialog()
                true
            }
            R.id.action_details -> {
                showDetailsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showInfoDialog() {
        //do nothing if already showing any dialog
        if (infoDialog?.isShowing != true && detailsDialog?.isShowing != true) {
            infoDialog = AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.info_details)
                .setCancelable(true)
                .setPositiveButton("ok") { dialog, _ -> dialog.dismiss() }
                .setNegativeButton("More info") { dialog, _ ->
                    dialog.dismiss()
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(GITHUB_LINK)
                        )
                    )
                }
                .create()
            infoDialog?.show()
        }
    }

    private fun showDetailsDialog() {
        //do nothing if already showing any dialog or there is no last error
        if (infoDialog?.isShowing != true && detailsDialog?.isShowing != true
            && RootBeer.LAST_FOUND_VULNERABILITY_DETAILS != "") {
            detailsDialog = AlertDialog.Builder(this)
                .setTitle(R.string.details_dialog_title)
                .setMessage("Last encountered error details: ${RootBeer.LAST_FOUND_VULNERABILITY_DETAILS}")
                .setCancelable(true)
                .setPositiveButton("ok") { dialog, _ -> dialog.dismiss() }
                .create()
            detailsDialog?.show()
        }
    }

    private fun onRootCheckFinished(isRooted: Boolean) {
        fab.show()
        isRootedTextView.update(isRooted = isRooted)
        isRootedTextView.show()
    }

    companion object {
        private const val GITHUB_LINK = "https://github.com/scottyab/rootbeer"
    }
}

