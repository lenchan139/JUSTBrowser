package org.lenchan139.lightbrowser.Fragment

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.fragment_browse.*
import kotlinx.android.synthetic.main.fragment_browse.view.*
import org.lenchan139.lightbrowser.BrowseActivity
import org.lenchan139.lightbrowser.Class.CommonStrings
import org.lenchan139.lightbrowser.Class.WebViewOverride
import org.lenchan139.lightbrowser.CustomScript.CustomScriptUtil
import org.lenchan139.lightbrowser.History.HistroySQLiteController
import org.lenchan139.lightbrowser.MainActivity
import org.lenchan139.lightbrowser.R
import java.io.File


/**
 * A placeholder fragment containing a simple view.
 */
class BrowseFragment : Fragment() {
    lateinit var rootView  : View
    lateinit var settings : SharedPreferences
    lateinit var commonStrings : CommonStrings
    lateinit var activity : BrowseActivity
    private var back = false
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_browse, container, false)
        activity = getActivity() as BrowseActivity
        commonStrings = CommonStrings(activity)
        settings = PreferenceManager.getDefaultSharedPreferences(activity)


        initWebView(rootView.webView).requestFocus()

        activity.arrBrowseFragment.add(this)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val homeUrl = settings.getString(commonStrings.TAG_pref_home(), commonStrings.URL_DDG())
        loadUrl(homeUrl)
    }

    fun getWebTitle():String{
        return rootView.webView.title
    }
    fun getWebUrl():String{
        return rootView.webView.url
    }
    fun loadUrl (url:String){
        rootView.webView.loadUrl(url)
    }
    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(sectionNumber: Int): BrowseFragment {
            val fragment = BrowseFragment()
            val args = Bundle()
            args.putInt(ARG_SECTION_NUMBER, sectionNumber)
            fragment.arguments = args
            return fragment
        }
    }
    fun isUrlVaildRedirect(url: String):Boolean{
        if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("javascript:")) {
            return true
        } else {
            return false
        }
    }
    fun getCurrWebView():WebViewOverride{return rootView.webView}
    fun initWebView(webView : WebViewOverride):WebViewOverride{
        webView.settings.javaScriptEnabled = true
        webView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        settings.edit().putString(commonStrings.TAG_pref_custom_user_agent_default(),webView.settings.userAgentString).commit()
        var default = webView.settings.userAgentString
        webView.settings.userAgentString = settings.getString(commonStrings.TAG_pref_custom_user_agent(),default)

        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        webView.setFindListener ( object : WebView.FindListener{
            override fun onFindResultReceived(activeMatchOrdinal: Int, numberOfMatches: Int, isDoneCounting: Boolean) {

            }
        }


        )
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val request = DownloadManager.Request(
                    Uri.parse(url))
            val cm = CookieManager.getInstance().getCookie(url)
            val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimetype)
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
            var downloadId : Long = -1

            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) //Notify client once download is completed!
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,  fileName)
            request.addRequestHeader("Cookie", cm)
            var  onComplete   = object : BroadcastReceiver(){

                override fun onReceive(contxt: Context?, intent: Intent?) {
                    val action = intent?.getAction()
                    val manager = activity?.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
                    if(DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action) && (downloadId >= 0) ){
                        val query = DownloadManager.Query()
                        query.setFilterById(downloadId)
                        val c = manager.query(query)
                        if (c.moveToFirst()) {
                            val columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                            if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                                var uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                                uriString = uriString.replace("file:///","")
                                val apkFile = File(uriString)

                                Log.v("onDownloadConplete",uriString)
                                if(apkFile.extension == "apk"){
                                    activity.installApk(activity,apkFile)
                                }
                                Toast.makeText(activity, "download success", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }

            //registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            request.setDescription("[Download task from Light Browser]")
            request.allowScanningByMediaScanner()
            val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = dm.enqueue(request)
            Log.v("downloadMimeType", mimetype)
            Toast.makeText(activity, "Downloading...", //To notify the Client that the file is being downloaded
                    Toast.LENGTH_LONG).show()
        }
        webView.setWebChromeClient(object : WebChromeClient() {
            override fun onCloseWindow(window: WebView) {
                activity.onBackPressed()
                super.onCloseWindow(window)
            }

            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                //newTab(this@MainActivity)
                return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                super.onReceivedTitle(view, title)
                Log.v("currWebViewTitle", title)
            }

            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                if (activity.mUploadMessage != null) {
                    activity.mUploadMessage!!.onReceiveValue(null)
                }
                Log.i("UPFILE", "file chooser params：" + fileChooserParams!!.toString())
                activity.mUploadMessage = filePathCallback
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)

                Log.v("acceptType", fileChooserParams.acceptTypes[0].toString())
                if (fileChooserParams != null && fileChooserParams.acceptTypes != null
                        && fileChooserParams.acceptTypes.size > 0) {
                    i.type = fileChooserParams.acceptTypes[0]
                    i.type = "*/*"
                } else {
                    i.type = "*/*"
                }
                startActivityForResult(Intent.createChooser(i, "File Chooser"), activity.FILECHOOSER_RESULTCODE)

                return true
            }

            //Android 5.0+ Uploads
            fun onShowFileChooser1(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: WebChromeClient.FileChooserParams?): Boolean {
                return false
            }

            override fun onProgressChanged(view: WebView, progress: Int) {
                Log.v("webview " + view.id , "[onProgressChanged triggered]")
                if (progress < 100) {
                    rootView.progressL.visibility = ProgressBar.VISIBLE
                    rootView.progressL.progress = progress
                } else if (progress >= 100) {
                    rootView.progressL.progress = progress

                    try {
                        Thread.sleep(300)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    //progLoading.setVisibility(ProgressBar.INVISIBLE);
                    rootView.progressL.progress = 0
                    rootView.progressL.visibility = ProgressBar.GONE
                }

            }
        })
        webView.setWebViewClient(object : WebViewClient() {
            internal var loadingFinish: Boolean? = true
            internal var redirectPage: Boolean? = false

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                loadingFinish = false
                super.onPageStarted(view, url, favicon)
                webView.requestFocus()
                activity.editText.setText(url)
                Log.v("onPageLoadUrl",url)
                if (isUrlVaildRedirect(url!!)) {
                    //addToBack(url);
                } else {
                    back = true
                    view!!.stopLoading()
                    activity.runToExternal(url)
                    activity.editText.setText(webView.url)
                }

                var cm: String? = CookieManager.getInstance().getCookie(url)
                if (cm == null) {
                    cm = ""
                }

            }



            var isInitDone = false
            override fun onPageFinished(view: WebView, url: String) : Unit{

                if (!redirectPage!!) {
                    loadingFinish = true

                }

                if (loadingFinish!! && (!redirectPage!!)) {
                    //HIDE LOADING IT HAS FINISHED
                    //addToBack(url,view.getTitle());
                    val hs = HistroySQLiteController(activity)
                    if(view.title.isNotEmpty() && view.title.isNotEmpty()) {
                        hs.addHistory(view.title, view.url)
                    }
                    //runCustomScript(s)
                    val runscripts = CustomScriptUtil().getScriptsToRun(activity,url)
                    if(runscripts.size > 0 && isInitDone){
                        for(i in runscripts){
                            view.evaluateJavascript(i,null)
                        }
                    }
                    isInitDone = true
                } else {
                    redirectPage = false
                }

                super.onPageFinished(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                Log.v("loadingUrl",request!!.url.toString())
                return !(isUrlVaildRedirect(request!!.url.toString()))
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if ((!loadingFinish!!)) {
                    redirectPage = true
                }
                loadingFinish = false
                view.loadUrl(url)
                //addToBack(url);

                return true
            }
        })


        return webView
    }
}