/*
   Copyright 2018 Ryosuke839

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
@file:Suppress("DEPRECATION")

package jp.ddo.hotmist.unicodepad

import android.app.AlertDialog
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.ClipboardManager
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.preference.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class SettingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                .replace(android.R.id.content, MyPreferenceFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class MyPreferenceFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener {
        private val adCompat: AdCompat = AdCompatImpl()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.setting)
            fun setEntry(it: ListPreference) = run {
                it.onPreferenceChangeListener = this
                it.summary = it.entry
            }

            fun setValue(it: ListPreference) = run {
                it.onPreferenceChangeListener = this
                it.summary = it.value
            }

            fun setText(it: EditTextPreference) = run {
                it.onPreferenceChangeListener = this
                it.summary = it.text
            }
            setEntry(findPreference("universion")!!)
            setEntry(findPreference("emojicompat")!!)
            findPreference<Preference>("download")!!.also {
                it.setOnPreferenceClickListener {
                    openPage(getString(R.string.download_uri))
                }
            }
            findPreference<Preference>("export")!!.also {
                it.setOnPreferenceClickListener {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "application/json"
                    startActivityForResult(intent, SETTING_EXPORT_CODE)
                    true
                }
            }
            findPreference<Preference>("import")!!.also {
                it.setOnPreferenceClickListener {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "application/json"
                    startActivityForResult(intent, SETTING_IMPORT_CODE)
                    true
                }
            }
            setEntry(findPreference("theme")!!)
            setText(findPreference("textsize")!!)
            setValue(findPreference("column")!!)
            setValue(findPreference("columnl")!!)
            findPreference<Preference>("tabs")!!.also {
                it.setOnPreferenceClickListener {
                    startActivity(Intent(activity, TabsActivity::class.java))
                    true
                }
            }
            setText(findPreference("padding")!!)
            setText(findPreference("gridsize")!!)
            setText(findPreference("viewsize")!!)
            setText(findPreference("checker")!!)
            setText(findPreference("recentsize")!!)
            setEntry(findPreference("scroll")!!)
            findPreference<Preference>("process_text")!!.onPreferenceChangeListener = this
            findPreference<Preference>("legal_app")!!.also {
                it.setOnPreferenceClickListener {
                    openPage("https://github.com/Ryosuke839/UnicodePad")
                }
            }
            findPreference<Preference>("legal_uni")!!.also {
                it.setOnPreferenceClickListener {
                    openPage("https://unicode.org/")
                }
            }
            if (!adCompat.showAdSettings) {
                findPreference<CheckBoxPreference>("no-ad")!!.also {
                    if (Build.VERSION.SDK_INT >= 26) {
                        it.parent?.removePreference(it)
                    } else {
                        it.isEnabled = false
                        it.isChecked = true
                    }
                }
            }
            preferenceScreen.let {
                for (i in 0 until it.preferenceCount) {
                    val pref = it.getPreference(i)
                    pref.isIconSpaceReserved = false
                    if (pref is PreferenceGroup) {
                        for (j in 0 until pref.preferenceCount) {
                            pref.getPreference(j).isIconSpaceReserved = false
                        }
                    }
                }
            }
            activity.setResult(RESULT_OK)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        }

        override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
            if (preference.hasKey()) {
                val key = preference.key
                try {
                    if (key == "column" || key == "padding" || key == "recentsize") newValue.toString().toInt()
                    if (key == "textsize" || key == "gridsize" || key == "viewsize" || key == "checker") newValue.toString().toFloat()
                } catch (e: NumberFormatException) {
                    return false
                }
                if (key == "theme") {
                    activity.recreate()
                }
                if (key == "emojicompat") {
                    Toast.makeText(activity, R.string.theme_title, Toast.LENGTH_SHORT).show()
                    activity.setResult(RESULT_FIRST_USER)
                }
                if (key == "process_text") {
                    val packageName = activity.packageName
                    val newState =
                        if (newValue as Boolean) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    activity.packageManager.setComponentEnabledSetting(
                        ComponentName(packageName, "$packageName.UnicodeActivityAlias"),
                        newState, PackageManager.DONT_KILL_APP
                    )
                    return true
                }
            }
            preference.summary = if (preference is ListPreference) preference.entries[preference.findIndexOfValue(newValue.toString())] else newValue.toString()
            return true
        }

        private fun openPage(uri: String): Boolean {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            if (activity.packageManager.queryIntentActivities(intent, 0).size > 0) {
                // Show web page
                startActivity(intent)
            } else {
                // Copy URI
                (activity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).text = uri
                Toast.makeText(activity, R.string.copied, Toast.LENGTH_SHORT).show()
            }
            return true
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == SETTING_EXPORT_CODE) if (resultCode == RESULT_OK && data != null) {
                val uri = data.data ?: return
                val pref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this.activity)
                val padding = (resources.displayMetrics.density * 8f).toInt()
                val cbSetting = CheckBox(this.activity).also {
                    it.setText(R.string.data_setting)
                    it.isChecked = true
                    it.setPadding(padding, padding, padding, padding)
                }
                val cbHistory = CheckBox(this.activity).also {
                    it.text = resources.getString(R.string.data_history, pref.getString("rec", null)?.let { str -> str.codePointCount(0, str.length) } ?: 0)
                    it.isChecked = true
                    it.setPadding(padding, padding, padding, padding)
                }
                val cbFavorite = CheckBox(this.activity).also {
                    it.text = resources.getString(R.string.data_favorite, pref.getString("fav", null)?.let { str -> str.codePointCount(0, str.length) } ?: 0)
                    it.isChecked = true
                    it.setPadding(padding, padding, padding, padding)
                }
                val cbScroll = CheckBox(this.activity).also {
                    it.setText(R.string.data_scroll)
                    it.isChecked = true
                    it.setPadding(padding, padding, padding, padding)
                }
                AlertDialog.Builder(this.activity).setView(LinearLayout(this.activity).also { hl ->
                    hl.orientation = LinearLayout.VERTICAL
                    hl.addView(cbSetting, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    hl.addView(cbHistory, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    hl.addView(cbFavorite, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    hl.addView(cbScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }).setTitle(R.string.exported_data).setPositiveButton(R.string.export_confirm) { _: DialogInterface, _: Int ->
                    try {
                        (this.activity.contentResolver.openOutputStream(uri) ?: throw IOException()).use { stream ->
                            stream.write(JSONObject().also { obj ->
                                obj.put("unicodepad_version", 47)
                                if (cbSetting.isChecked) {
                                    obj.put("setting", JSONObject().also {
                                        it.put("universion", pref.getString("universion", null))
                                        it.put("emojicompat", pref.getString("emojicompat", null))
                                        it.put("theme", pref.getString("theme", null))
                                        it.put("no-ad", if (pref.contains("no-ad")) pref.getBoolean("no-ad", false) else null)
                                        it.put("skip_guide", if (pref.contains("skip_guide")) pref.getBoolean("skip_guide", false) else null)
                                        it.put("cnt_shown", if (pref.contains("cnt_shown")) pref.getInt("cnt_shown", 0) else null)
                                        for (key in arrayOf("rec", "list", "emoji", "find", "fav", "edt")) {
                                            it.put("ord_$key", if (pref.contains("ord_$key")) pref.getInt("ord_$key", 0) else null)
                                            it.put("single_$key", pref.getString("single_$key", null))
                                        }
                                        it.put("page", if (pref.contains("page")) pref.getInt("page", 0) else null)
                                        it.put("gridsize", pref.getString("gridsize", null))
                                        it.put("padding", pref.getString("padding", null))
                                        it.put("column", pref.getString("column", null))
                                        it.put("columnl", pref.getString("columnl", null))
                                        it.put("textsize", pref.getString("textsize", null))
                                        it.put("viewsize", pref.getString("viewsize", null))
                                        it.put("lines", if (pref.contains("lines")) pref.getBoolean("lines", true) else null)
                                        it.put("shrink", if (pref.contains("shrink")) pref.getBoolean("shrink", true) else null)
                                        it.put("ime", if (pref.contains("ime")) pref.getBoolean("ime", true) else null)
                                        it.put("clear", if (pref.contains("clear")) pref.getBoolean("clear", true) else null)
                                        it.put("buttons", if (pref.contains("buttons")) pref.getBoolean("buttons", true) else null)
                                        it.put("process_text", if (pref.contains("process_text")) pref.getBoolean("process_text", true) else null)
                                        it.put("scroll", pref.getString("scroll", null))
                                        it.put("recentsize", pref.getString("recentsize", null))
                                    })
                                }
                                if (cbHistory.isChecked) {
                                    obj.put("rec", pref.getString("rec", null))
                                }
                                if (cbFavorite.isChecked) {
                                    obj.put("fav", pref.getString("fav", null))
                                }
                                if (cbScroll.isChecked) {
                                    obj.put("scroll", JSONObject().also {
                                        it.put("find", pref.getString("find", null))
                                        it.put("list", if (pref.contains("list")) pref.getInt("list", 0) else null)
                                        it.put("emoji", if (pref.contains("emoji")) pref.getInt("emoji", 0) else null)
                                        it.put("locale", pref.getString("locale", null))
                                    })
                                }
                            }.toString(2).encodeToByteArray())
                        }
                        Toast.makeText(this.activity, R.string.exported_notice, Toast.LENGTH_SHORT).show()
                    } catch (e: IOException) {
                        Toast.makeText(this.activity, resources.getString(R.string.export_fail_io, e), Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }
                }.setNegativeButton(android.R.string.cancel) { _, _ -> }.create().show()
            }
            if (requestCode == SETTING_IMPORT_CODE) if (resultCode == RESULT_OK && data != null) {
                val uri = data.data ?: return
                try {
                    (this.activity.contentResolver.openInputStream(uri) ?: throw IOException()).use { stream ->
                        try {
                            val obj = JSONObject(stream.readBytes().decodeToString())
                            if (!obj.has("unicodepad_version")) {
                                Toast.makeText(this.activity, "This file is not exported from UnicodePad.", Toast.LENGTH_LONG).show()
                                return@use
                            }
                            if (obj.getInt("unicodepad_version") > 47) {
                                Toast.makeText(this.activity, "This file is exported from newer version of UnicodePad. Update and try again.", Toast.LENGTH_LONG).show()
                                return@use
                            }
                            val padding = (resources.displayMetrics.density * 8f).toInt()
                            val cbSetting = CheckBox(this.activity).also {
                                it.setText(R.string.data_setting)
                                if (obj.has("setting"))
                                    it.isChecked = true
                                else
                                    it.isEnabled = false
                                it.setPadding(padding, padding, padding, padding)
                            }
                            val cbHistoryMerge = CheckBox(this.activity).also {
                                it.setText(R.string.data_merge)
                                it.setPadding(padding * 2, padding, padding * 2, padding)
                            }
                            val cbHistory = CheckBox(this.activity).also {
                                it.text = resources.getString(R.string.data_history, (obj.opt("rec") as? String)?.let { str -> str.codePointCount(0, str.length) } ?: 0)
                                it.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
                                    cbHistoryMerge.isEnabled = checked
                                }
                                if (obj.has("rec"))
                                    it.isChecked = true
                                else
                                    it.isEnabled = false
                                it.setPadding(padding, padding, padding, padding)
                            }
                            val cbFavoriteMerge = CheckBox(this.activity).also {
                                it.setText(R.string.data_merge)
                                it.setPadding(padding * 2, padding, padding * 2, padding)
                            }
                            val cbFavorite = CheckBox(this.activity).also {
                                it.text = resources.getString(R.string.data_favorite, (obj.opt("fav") as? String)?.let { str -> str.codePointCount(0, str.length) } ?: 0)
                                it.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
                                    cbFavoriteMerge.isEnabled = checked
                                }
                                if (obj.has("fav"))
                                    it.isChecked = true
                                else
                                    it.isEnabled = false
                                it.setPadding(padding, padding, padding, padding)
                            }
                            val cbScroll = CheckBox(this.activity).also {
                                it.setText(R.string.data_scroll)
                                if (obj.has("scroll"))
                                    it.isChecked = true
                                else
                                    it.isEnabled = false
                                it.setPadding(padding, padding, padding, padding)
                            }
                            AlertDialog.Builder(this.activity).setView(LinearLayout(this.activity).also { hl ->
                                hl.orientation = LinearLayout.VERTICAL
                                hl.addView(cbSetting, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                                hl.addView(cbHistory, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                                hl.addView(cbHistoryMerge, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                                hl.addView(cbFavorite, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                                hl.addView(cbFavoriteMerge, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                                hl.addView(cbScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                            }).setTitle(R.string.imported_data).setPositiveButton(R.string.import_confirm) { _: DialogInterface, _: Int ->
                                val pref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this.activity)
                                val edit = pref.edit()
                                if (cbSetting.isChecked) {
                                    obj.optJSONObject("setting")?.also {
                                        (it.opt("universion") as? String)?.let { str -> edit.putString("universion", str) }
                                        (it.opt("emojicompat") as? String)?.let { str -> edit.putString("emojicompat", str) }
                                        (it.opt("theme") as? String)?.let { str -> edit.putString("theme", str) }
                                        (it.opt("no-ad") as? Boolean)?.let { bool -> edit.putBoolean("no-ad", bool) }
                                        (it.opt("skip_guide") as? Boolean)?.let { bool -> edit.putBoolean("skip_guide", bool) }
                                        (it.opt("cnt_shown") as? Int)?.let { int -> edit.putInt("cnt_shown", int) }
                                        for (key in arrayOf("rec", "list", "emoji", "find", "fav", "edt")) {
                                            (it.opt("ord_$key") as? Int)?.let { int -> edit.putInt("ord_$key", int) }
                                            (it.opt("single_$key") as? String)?.let { str -> edit.putString("single_$key", str) }
                                        }
                                        it.put("page", if (pref.contains("page")) pref.getInt("page", 0) else null)
                                        (it.opt("gridsize") as? String)?.let { str -> edit.putString("gridsize", str) }
                                        (it.opt("padding") as? String)?.let { str -> edit.putString("padding", str) }
                                        (it.opt("column") as? String)?.let { str -> edit.putString("column", str) }
                                        (it.opt("columnl") as? String)?.let { str -> edit.putString("columnl", str) }
                                        (it.opt("textsize") as? String)?.let { str -> edit.putString("textsize", str) }
                                        (it.opt("viewsize") as? String)?.let { str -> edit.putString("viewsize", str) }
                                        (it.opt("lines") as? Boolean)?.let { bool -> edit.putBoolean("lines", bool) }
                                        (it.opt("shrink") as? Boolean)?.let { bool -> edit.putBoolean("shrink", bool) }
                                        (it.opt("ime") as? Boolean)?.let { bool -> edit.putBoolean("ime", bool) }
                                        (it.opt("clear") as? Boolean)?.let { bool -> edit.putBoolean("clear", bool) }
                                        (it.opt("buttons") as? Boolean)?.let { bool -> edit.putBoolean("buttons", bool) }
                                        (it.opt("process_text") as? Boolean)?.let { bool -> edit.putBoolean("process_text", bool) }
                                        (it.opt("scroll") as? String)?.let { str -> edit.putString("scroll", str) }
                                        (it.opt("recentsize") as? String)?.let { str -> edit.putString("recentsize", str) }
                                    }
                                }
                                if (cbHistory.isChecked) {
                                    (obj.opt("rec") as? String)?.let {
                                        val str = (if (cbHistoryMerge.isChecked) pref.getString("rec", "") else "") + it
                                        val list = mutableListOf<Int>()
                                        var i = 0
                                        while (i < str.length) {
                                            val code = str.codePointAt(i)
                                            i += Character.charCount(code)
                                            list.add(code)
                                        }
                                        edit.putString("rec", list.reversed().distinct().reversed().joinToString("") { code -> String(Character.toChars(code)) })
                                    }
                                }
                                if (cbFavorite.isChecked) {
                                    (obj.opt("fav") as? String)?.let {
                                        val str = (if (cbFavoriteMerge.isChecked) pref.getString("fav", "") else "") + it
                                        val list = mutableListOf<Int>()
                                        var i = 0
                                        while (i < str.length) {
                                            val code = str.codePointAt(i)
                                            i += Character.charCount(code)
                                            list.add(code)
                                        }
                                        edit.putString("fav", list.reversed().distinct().reversed().joinToString("") { code -> String(Character.toChars(code)) })
                                    }
                                }
                                if (cbScroll.isChecked) {
                                    obj.optJSONObject("scroll")?.also {
                                        (it.opt("find") as? String)?.let { str -> edit.putString("find", str) }
                                        (it.opt("list") as? Int)?.let { int -> edit.putInt("list", int) }
                                        (it.opt("emoji") as? Int)?.let { int -> edit.putInt("emoji", int) }
                                        (it.opt("locale") as? String)?.let { str -> edit.putString("locale", str) }
                                    }
                                }
                                edit.apply()
                                Toast.makeText(this.activity, R.string.imported_notice, Toast.LENGTH_SHORT).show()
                                this.activity.setResult(RESULT_FIRST_USER)
                                this.activity.finish()
                            }.setNegativeButton(android.R.string.cancel) { _, _ -> }.create().show()
                        } catch (e: JSONException) {
                            Toast.makeText(this.activity, resources.getString(R.string.import_fail_json, e), Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: IOException) {
                    Toast.makeText(this.activity, resources.getString(R.string.import_fail_io, e), Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        private const val SETTING_EXPORT_CODE = 43
        private const val SETTING_IMPORT_CODE = 44
    }
}