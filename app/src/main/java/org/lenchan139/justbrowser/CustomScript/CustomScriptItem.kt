package org.lenchan139.justbrowser.CustomScript

/**
 * Created by len on 14/4/2017.
 */

class CustomScriptItem(var title: String?, var url: String?, var script: String?) {

    override fun toString(): String {
        return "title=$title,url=$url,script=$script"
    }
}
