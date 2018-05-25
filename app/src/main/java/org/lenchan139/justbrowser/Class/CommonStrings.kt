package org.lenchan139.justbrowser.Class

import android.content.Context
import org.lenchan139.justbrowser.R


/**
 * Created by len on 10/15/16.
 */

class CommonStrings constructor(context:Context) {
    var context = context
    fun searchHeader(): String {
        return "https://duckduckgo.com/?q="
    }

    fun mobilizeHeader(): String {
        return "https://googleweblight.com/?lite_url="
    }

    fun homePage(): String {
        return "https://ddg.gg"
    }

    fun TAG_setting(): String {
        return "org.lenchan139.lightbrowser.settings"
    }

    fun TAG_pref_home(): String {
        return "org.lenchan139.lightbrowser.Home"
    }

    fun TAG_pref_fab(): String {
        return "org.lenchan139.lightbrowser.Fab"
    }

    fun ARRAY_pref_fab(): Array<String> {
        return arrayOf(context.getString(R.string.common_string_array_pref_fab_0),
                        context.getString(R.string.common_string_array_pref_fab_1), 
                        context.getString(R.string.common_string_array_pref_fab_2), 
                        context.getString(R.string.common_string_array_pref_fab_3),
                        context.getString(R.string.common_string_array_pref_fab_4),
                        context.getString(R.string.common_string_array_pref_fab_5),
                        context.getString(R.string.common_string_array_pref_fab_6))
    }
    fun ARRAY_pref_Sharing_Format(): Array<String> {
        return arrayOf( context.getString(R.string.common_string_array_sharing_format_0),
                context.getString(R.string.common_string_array_sharing_format_1))
    }

    fun ARRAY_pref_Search_Engine_Default(): Array<SearchEngineItem> {
        return arrayOf( SearchEngineItem("DuckDuckGo","https://duckduckgo.com/?q=!@keywoard"),
                        SearchEngineItem("Google","https://www.google.com/search?q=!@keywoard"))
    }

    fun STR_INTENT_MainToSetting(): String {
        return "org.lenchan139.lightbrowser.STR_INTENT_MainToSetting"
    }

    fun TAG_pref_oc_bookmark_url(): String {
        return "org.lenchan139.lightbrowser.TAG_pref_oc_bookmark_url"
    }
    fun TAG_pref_custom_user_agent(): String {
        return "org.lenchan139.lightbrowser.TAG_pref_custom_user_agent"
    }
    fun TAG_pref_custom_user_agent_default(): String {
        return "org.lenchan139.lightbrowser.TAG_pref_custom_user_agent_default"
    }
    fun TAG_pref_sharing_format_int(): String {
        return "org.lenchan139.lightbrowser.sharingformat.pref"
    }
    fun TAG_pref_sharing_format_string(): String {
        return "org.lenchan139.lightbrowser.TAG_pref_sharing_format_string"
    }
    fun TAG_pref_Search_Engine_Url(): String {
        return "org.lenchan139.lightbrowser.TAG_pref_Search_Engine_Url"
    }

    fun TAG_pref_Search_Engine_Name(): String {
        return "org.lenchan139.lightbrowser.TAG_pref_Search_Engine_Name"
    }

    fun URL_DDG(): String {
        return "https://duckduckgo.com"
    }
    fun TAG_custom_script_isEdit():String{
        return "TAG_custom_script_isEdit"
    }

    fun TAG_custom_script_editTitle():String{
        return "TAG_custom_script_editTitle"
    }


    class SearchEngineItem(val name: String, val url: String)
}
