package org.lenchan139.justbrowser

import android.Manifest
import android.app.Activity
import android.content.*
import android.support.v7.app.AppCompatActivity

import android.support.v4.app.Fragment
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.ValueCallback
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_browse.*
import kotlinx.android.synthetic.main.fragment_browse.*
import kotlinx.android.synthetic.main.fragment_browse.view.*
import org.lenchan139.justbrowser.Adapter.BrowseStateFragmentPageApdapter
import org.lenchan139.justbrowser.Class.*
import org.lenchan139.justbrowser.Fragment.BrowseFragment
import java.io.File
import java.util.*

class BrowseActivity : AppCompatActivity() {
    val FILECHOOSER_RESULTCODE = 859
    var mUploadMessage: ValueCallback<Array<Uri>>? = null
    val BROWSE_ITEM_KEYSTRING = "BROWSE_ITEM_KEYSTRING"
    lateinit var browseAdapter : BrowseStateFragmentPageApdapter
    var arrBrowseFragment = ArrayList<BrowseFragment>()
    var incomeUrlOnStart : String? = null
    lateinit var settings : SharedPreferences
    lateinit var commonStrings : CommonStrings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        commonStrings = CommonStrings(this)
        settings = PreferenceManager.getDefaultSharedPreferences(this)

        val list = ArrayList<String>()
        list.add(BROWSE_ITEM_KEYSTRING)
        browseAdapter = BrowseStateFragmentPageApdapter(supportFragmentManager,list)
        viewPager.adapter = browseAdapter
        initViewPageOnChangeListener()
        incomeUrlOnStart = intent.getStringExtra(getString(R.string.KEY_INURL_INTENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
        }
        permissionChecker()
        initInUrl()
        btnSwitchTabs.setOnClickListener {
            switchTab(this)
        }
        btnSwitchTabs.setOnLongClickListener {
            delTabDialog(this)
            true

        }
        editText.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            // If the event is a key-down event on the "enter" button
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                // Perform action on key press
                loadUrlFromEditTextToFragment()
                hideKeybord()
                return@OnKeyListener true
            }
            false
        })
        editText.setOnClickListener {
            val intent = Intent(this@BrowseActivity, SearchActivity::class.java)
            intent.putExtra("para", editText.text.toString())
            startActivity(intent)
            arrBrowseFragment.get(viewPager.currentItem).rootView.webView.requestFocus()
        }

        editText.isFocusable = false
    }

    override fun onBackPressed() {
        if (arrBrowseFragment.get(browseAdapter.currentPosition).getCurrWebView().canGoBack()) {
            arrBrowseFragment.get(browseAdapter.currentPosition).getCurrWebView().goBack()
        } else {
            exitDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_browse, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        if (id == R.id.action_settings) {
            val intent = Intent(this@BrowseActivity, PrefActivity::class.java)
            startActivity(intent)
            return true
        } else if (id == R.id.menu_home) {
            getCurrentFragment().loadUrl(settings.getString(commonStrings.TAG_pref_home(), ""))
            return true
        } else if (id == R.id.menu_share) {
            shareCurrPage()
            return true
        } else if (id == R.id.menu_external) {
            runToExternal(getCurrentFragment().webView.url)

            return true
        } else if (id == R.id.menu_history) {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)

        }else if(id == R.id.menu_add_NCBookmarks){
            var launchIntent = Intent()
            //launchIntent.addCategory("android.intent.category.LAUNCHER")
            //launchIntent.setAction("org.lenchan139.ncbookmark.v2.addBookmarkAction")
            launchIntent.setComponent(ComponentName("org.lenchan139.ncbookmark","org.lenchan139.ncbookmark.v2.AddBookmarkActivityV2"))
            launchIntent.putExtra("inUrl",getCurrentFragment().webView.url)
            launchIntent.putExtra("inTitle",getCurrentFragment().webView.title)
            try {
                startActivity(launchIntent)//null pointer check in case package name was not found
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                Toast.makeText(this, "NCBookmark not installed!", Toast.LENGTH_SHORT).show()
            }
        } else if (id == R.id.menu_view_ncbookmarks) {
            val launchIntent = packageManager.getLaunchIntentForPackage("org.lenchan139.ncbookmark")
            try {
                startActivity(launchIntent)//null pointer check in case package name was not found
            } catch (e: NullPointerException) {
                Toast.makeText(this, "NCBookmark not installed!", Toast.LENGTH_SHORT).show()
            }

            /*String url3 = settings.getString(commonStrings.TAG_pref_oc_bookmark_url(),"");
            if(!url3.endsWith("/"))
                url3 = url3 + "/";
            String title = webView.getTitle();
            if(url3.startsWith("http")) {
                String outUrl = url3 + "index.php/apps/bookmarks/";
                webView.loadUrl(outUrl);

            }*/
        } else if (id == R.id.menu_exit) {
            exitDialog()
        } else if (id == R.id.menu_refresh) {
            getCurrentFragment().webView.reload()
        }else if(id == R.id.menu_tab){
            switchTab(this@BrowseActivity)
        }else if(id == R.id.menu_find){
            findContent()
        }else if(id == R.id.menu_custom_script){
            openCustomScriptActivity()
        }else if(id == R.id.menu_desktop_mode_switch){
            getCurrentFragment().webView.setDesktopMode(!getCurrentFragment().webView.getDesktopModeStatus())
            getCurrentFragment().webView.reload()
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val tempWebiew = arrBrowseFragment.get(browseAdapter.currentPosition).getCurrWebView()
            val webList = tempWebiew.copyBackForwardList()

            //create String[] for showing
            val items = arrayOfNulls<String>(webList.size)
            //store list to string[] with reverse sorting
            for (i in 0..webList.size - 1) {
                var temp = webList.getItemAtIndex(webList.size - 1 - i).title
                //handling if current tab
                if (i == webList.size - 1 - webList.currentIndex) {
                    //Log.v("test",String.valueOf(webList.getSize() -1 - webList.getCurrentIndex()) );
                    temp = "◆" + temp
                } else {
                    temp = "◇" + temp
                }

                if (temp.length > 50) {
                    temp = temp.substring(0, 50) + " ..."
                }
                //if title too short, use url instead
                if (temp.length > 3) {
                    items[i] = temp
                } else {
                    items[i] = temp
                }
            }

            val dialog = AlertDialog.Builder(this).setTitle("History:")
                    .setItems(items) { dialog, which ->
                        var which = which
                        //Toast.makeText(MainActivity.this, items[which], Toast.LENGTH_SHORT).show();
                        if (which >= 0) {
                            //reverse the number
                            which = webList.size - 1 - which
                            val pushingUrl = webList.getItemAtIndex(which).url
                            //int a1 = which - webView.copyBackForwardList().getCurrentIndex();
                            //Log.v("test", String.valueOf(a1));
                            tempWebiew.goBackOrForward(which - tempWebiew.copyBackForwardList().currentIndex)
                            //webView.loadUrl(pushingUrl);


                        }
                    }.create()
            dialog.show()

            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }
    override fun onPostResume() {
        super.onPostResume()
        initFabButton()
    }

    fun delTabDialog(activity: Activity){
        updateSwitchCount()
        var items = Array<String>(arrBrowseFragment.size, { "" })
        for ((i, item) in arrBrowseFragment.withIndex()) {
            val w = item.getCurrWebView()
            if(i==viewPager.currentItem) {
                items.set(i, "☒ " + w.title + "\n     " + w.url)
            }else{
                items.set(i, "☒ " + w.title + "\n     " + w.url)
            }
        }
        var dialog = AlertDialog.Builder(this).setTitle("Delete Tab...")
                .setItems(items) { dialog, which ->
                    browseAdapter.remove(which)
                    updateSwitchCount()
                }
                .create()
        dialog.show()
    }
    override fun onNewIntent(intent: Intent) {
        val inUrl = intent.getStringExtra(getString(R.string.KEY_INURL_INTENT))
        var InURLFromExternal = intent.getBooleanExtra("InURLFromExternal",false)
        if(InURLFromExternal){
            addTab()
            editText.setText(inUrl)
            loadUrlFromEditTextToFragment()
            intent.putExtra("InURLFromExternal",false)
        }else if (inUrl != null) {
            editText.setText(inUrl)
            loadUrlFromEditTextToFragment()
        } else {
            super.onNewIntent(intent)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data)
        //Log.i("here123","before")
        if (requestCode == FILECHOOSER_RESULTCODE) {
            //Log.i("here123","after")
            if (null == mUploadMessage)
            {
                //mUploadMessage!!.onReceiveValue(null)
                return
            }
            val result = if (data == null || resultCode != Activity.RESULT_OK) null else data.data
            if (result == null) {
                mUploadMessage!!.onReceiveValue(null)
                mUploadMessage = null
                return
            }

            Log.i("UPFILE", "onActivityResult" + result.toString());
            val path = FileUtils.getPath(this, result)
            if (TextUtils.isEmpty(path)) {
                mUploadMessage!!.onReceiveValue(null)
                mUploadMessage = null
                return
            }
            val uri = Uri.fromFile(File(path!!))
            Log.i("UPFILE", "onActivityResult after parser uri:" + uri.toString());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mUploadMessage!!.onReceiveValue(arrayOf(uri))
            } else {
                mUploadMessage!!.onReceiveValue(null)
            }
            mUploadMessage = null
        }
    }
    protected fun exitDialog() {
        val items = arrayOf("Yes", "No")
        val dialog = AlertDialog.Builder(this).setTitle("Exit the Browser?")
                .setPositiveButton("Exit") { dialog, which ->
                    //Toast.makeText(MainActivity.this, items[which], Toast.LENGTH_SHORT).show();
                    finish()
                    android.os.Process.killProcess(android.os.Process.myPid())
                }.setNegativeButton("Cancel", null)
                .create()
        dialog.show()
    }
    fun openCustomScriptActivity(){
        val intent = Intent(this,CustomScriptActivity::class.java)
        startActivity(intent)
    }

    fun getCurrentFragment():BrowseFragment{
        return arrBrowseFragment.get(viewPager.currentItem)
    }

    fun shareCurrPage() {

        val sendIntent = Intent()
        var sfType = settings.getString(CommonStrings(baseContext).TAG_pref_sharing_format_int(),getString(R.string.common_string_array_sharing_format_0))
        var  sfContent = ""
        val currWebView = getCurrentFragment().webView
        if(sfType == getString(R.string.common_string_array_sharing_format_0)){
            sfContent = currWebView.url
        }else if(sfType == getString(R.string.common_string_array_sharing_format_1)){
            sfContent = currWebView.title + "\n" + currWebView.url
        }
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, sfContent)
        sendIntent.type = "text/plain"
        startActivity(Intent.createChooser(sendIntent, "Send to..."))
    }

    fun findContent(){
        val dialog = AlertDialog.Builder(this)
        var editText = ClearableEditText(this)
        editText.setHint("your keyword...")
        editText.setSingleLine()
        dialog.setView(editText)
        dialog.setTitle("Find...")

        dialog.setPositiveButton("Find", DialogInterface.OnClickListener { dialog, which ->
            if(editText.text == null){

            } else {
                getCurrentFragment().startFindContent(editText.text.toString())

            }
        })
        dialog.setNegativeButton("Cancel",null)

        dialog.create().show()
    }
    fun updateEditTextFromCurrentPage(section_number:Int, url:String?):Boolean{
        editText.setText(arrBrowseFragment.get(viewPager.currentItem).getWebUrl())
        return true
    }
    fun loadUrlFromEditTextToFragment() {
        arrBrowseFragment.get(viewPager.currentItem).loadUrl(editText.text.toString())


    }
    fun hideKeybord() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
    fun initFabButton() {
        val fab = findViewById(R.id.fab) as FloatingActionButton
        val curr = settings.getString(commonStrings.TAG_pref_fab(), null)
        if (curr == commonStrings.ARRAY_pref_fab()[1]) {
            fab.visibility = View.VISIBLE
            fab.setImageResource(R.drawable.fab_home)
        } else if (curr == commonStrings.ARRAY_pref_fab()[2]) {
            fab.visibility = View.VISIBLE
            fab.setImageResource(R.drawable.fab_refresh)
        } else if (curr == commonStrings.ARRAY_pref_fab()[3]) {
            fab.visibility = View.VISIBLE
            fab.setImageResource(R.drawable.fab_share)
        } else if (curr == commonStrings.ARRAY_pref_fab()[4]){
            fab.visibility = View.VISIBLE
            fab.setImageResource(R.drawable.fab_context_menu)
        }else if (curr == commonStrings.ARRAY_pref_fab()[5]){
            fab.visibility = View.VISIBLE
            fab.setImageResource(R.drawable.fab_tab_switch)
        }else if (curr == commonStrings.ARRAY_pref_fab()[6]){
            fab.visibility = View.VISIBLE
            fab.setImageResource(R.drawable.fab_open_with)
        }else {
            fab.visibility = View.GONE
        }

    }

    fun updateSwitchCount(){
        btnSwitchTabs.text = browseAdapter.count.toString()
    }
    fun switchTab(activity:Activity) {
        updateSwitchCount()
        var items = Array<String>(arrBrowseFragment.size, { "" })
        for (i in 0..arrBrowseFragment.size-1) {

             val w = arrBrowseFragment.get(i).rootView.webView
            if(i==viewPager.currentItem) {
                items.set(i, "▶" + w.title + "\n" + w.url)
            }else{
                items.set(i, "▷" + w.title + "\n" + w.url)
            }
        }
        var dialog = AlertDialog.Builder(this).setTitle("Tabs:")
                .setItems(items) { dialog, which ->
                    viewPager.setCurrentItem(which)
                    updateSwitchCount()
                }
                .setPositiveButton("New", DialogInterface.OnClickListener { dialog, which ->
                    addTab()
                }).create()
        dialog.show()




    }
    fun addTab(){
        browseAdapter.add(BROWSE_ITEM_KEYSTRING)
        updateSwitchCount()
        viewPager.setCurrentItem(arrBrowseFragment.size-1)
    }
    fun initInUrl(){
        val inUrl = intent.getStringExtra(getString(R.string.KEY_INURL_INTENT))
        intent.putExtra(getString(R.string.KEY_INURL_INTENT), "")
        if (inUrl != null && inUrl != "") {

        } else {

        }
    }
    fun initViewPageOnChangeListener(){
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }
            override fun onPageSelected(position: Int) {
                updateEditTextFromCurrentPage(viewPager.currentItem,arrBrowseFragment.get(viewPager.currentItem).getWebUrl())
            }

        })
    }
    fun permissionChecker(){
        try {
            if (ContextCompat.checkSelfPermission(this@BrowseActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this@BrowseActivity,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.
                    Toast.makeText(this, "This Appp need permission for Downloading, please allow it.", Toast.LENGTH_LONG).show()
                    val dialog = AlertDialog.Builder(this)
                    dialog.setTitle("Storage Access Required")
                            .setMessage("This Appp need internal storage permission for Download File, please allow it.")
                            .setCancelable(false)
                            .setPositiveButton("Grant", DialogInterface.OnClickListener { dialogInterface, i ->
                                val STORAGE_PERMISSION_ID = 112
                                ActivityCompat.requestPermissions(this@BrowseActivity,
                                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                        STORAGE_PERMISSION_ID)

                            }).create().show()
                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "You are running Android 5 or lower, Skip Permission Checking.", Toast.LENGTH_SHORT).show()
        }


    }
    fun installApk(context :Context, apkFile : File){
        val intent =  Intent(Intent.ACTION_VIEW)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            val contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", apkFile);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        }else{
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
    }

    fun runToExternal(url: String) {
        val preIntent = Intent(Intent.ACTION_VIEW,Uri.parse(url))
        val targetedShareIntents = ArrayList<Intent>()
        val browserIntent = Intent.createChooser(preIntent,"Open with...")
        val resInfo = packageManager.queryIntentActivities(preIntent, PackageManager.MATCH_ALL)

        for (resolveInfo in resInfo){
            Log.v("listV",resolveInfo.activityInfo.packageName)
            val packageName = resolveInfo.activityInfo.packageName
            val targetedShareIntent = Intent(Intent.ACTION_VIEW,Uri.parse(url))
            targetedShareIntent.setPackage(packageName)
            if(!packageName.contains("org.lenchan139.lightbrowser")){
                targetedShareIntents.add(targetedShareIntent)
                Log.v("listVTureFalse","True")
            }
        }
        if(targetedShareIntents.size > 1 ) {
            val chooserIntent = Intent.createChooser(
                    targetedShareIntents.removeAt(targetedShareIntents.size - 1), "Open with...")

            chooserIntent.putExtra(
                    Intent.EXTRA_INITIAL_INTENTS, JavaUtils().listToPracelable(targetedShareIntents))
            startActivity(chooserIntent)
        }else if(targetedShareIntents.size == 1) {
            val dialog = AlertDialog.Builder(this)
            val theIntent = packageManager.queryIntentActivities(targetedShareIntents.get(0), 0).get(0)
            dialog.setTitle("Open in " + theIntent.loadLabel(packageManager) + " ?")
                    .setIcon(theIntent.loadIcon(packageManager))
                    .setNegativeButton("Cancel" , DialogInterface.OnClickListener { dialogInterface, i ->

                    })
                    .setPositiveButton("Go!", DialogInterface.OnClickListener { dialogInterface, i ->
                        startActivity(targetedShareIntents.get(0))
                    }).create().show()
        }else{
            Toast.makeText(this,"No Handler here.",Toast.LENGTH_SHORT).show()
        }

    }
}
