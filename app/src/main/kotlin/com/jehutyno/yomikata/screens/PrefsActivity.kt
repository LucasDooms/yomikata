package com.jehutyno.yomikata.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.YomikataZKApplication
import com.jehutyno.yomikata.repository.database.YomikataDatabase
import com.jehutyno.yomikata.util.Category
import com.jehutyno.yomikata.util.FileUtils
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.RestartDialogMessage
import com.jehutyno.yomikata.util.backupProgress
import com.jehutyno.yomikata.util.getBackupLauncher
import com.jehutyno.yomikata.util.getLevelDownloadVersion
import com.jehutyno.yomikata.util.getRestartDialog
import com.jehutyno.yomikata.util.getRestoreLauncher
import com.jehutyno.yomikata.util.restoreProgress
import com.wooplr.spotlight.prefs.PreferencesManager
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import mu.KLogging
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.okButton


/**
 * Created by valentin on 30/11/2016.
 */
class PrefsActivity : AppCompatActivity() {

    companion object : KLogging()

    private lateinit var backupLauncher : ActivityResultLauncher<Intent>
    private lateinit var restoreLauncher : ActivityResultLauncher<Intent>


    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase, YomikataZKApplication.viewPump))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        AppCompatDelegate.setDefaultNightMode(pref.getInt(Prefs.DAY_NIGHT_MODE.pref, AppCompatDelegate.MODE_NIGHT_YES))
        supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, PrefsFragment()).commit()

        backupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
                                                    { result -> getBackupLauncher(result) }

        restoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            result ->
                pref.edit().putBoolean(Prefs.DB_RESTORE_ONGOING.pref, true).apply()
                getRestoreLauncher(result)
        }

    }

    class PrefsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }

        private fun getResetAlert() : AlertDialog {
            return requireContext().alertDialog {
                messageResource = R.string.prefs_reinit_sure
                okButton {
                    YomikataDatabase.resetDatabase(requireContext())
                    YomikataDatabase.forceLoadDatabase(requireContext())
                    requireActivity().getRestartDialog(RestartDialogMessage.RESET) {
                        YomikataDatabase.restoreLocalBackup(requireContext())
                    }.show()
                }
                cancelButton()
            }
        }

        private fun getDeleteVoicesAlert() : AlertDialog {
            return requireContext().alertDialog {
                messageResource = R.string.prefs_delete_voices_sure
                okButton {
                    FileUtils.deleteFolder(activity, "Voices")
                    val pref = PreferenceManager.getDefaultSharedPreferences(context)
                    for (i in 0 until 7) {
                        pref.edit().putBoolean(
                                "${Prefs.VOICE_DOWNLOADED_LEVEL_V.pref}${getLevelDownloadVersion(i)}_$i", false
                        ).apply()
                    }
                    val toast = Toast.makeText(context, R.string.voices_reinit_done, Toast.LENGTH_LONG)
                    toast.show()
                    requireActivity().finish()
                }
                cancelButton()
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                // Quiz Settings
                "font_size" -> {
                    return true
                }
                "input_change" -> {
                    return true
                }
                "speed" -> {
                    return true
                }
                "length" -> {
                    return true
                }
                "tap_to_reveal" -> {
                    return true
                }
                "play_start" -> {
                    return true
                }
                "play_end" -> {
                    return true
                }

                // Tutorials
                "reset_tuto" -> {
                    PreferencesManager(activity).resetAll()
                    // tell quizzes activity to start in home screen fragment
                    val intent = Intent()
                    intent.putExtra("gotoCategory", Category.HOME)
                    requireActivity().setResult(RESULT_OK, intent)
                    requireActivity().finish()
                    return true
                }

                // Backup and Restore
                "backup" -> {
                    backupProgress((activity as PrefsActivity).backupLauncher)
                    return true
                }
                "restore" -> {
                    (activity as PrefsActivity).restoreProgress((activity as PrefsActivity).restoreLauncher)
                    return true
                }
                "reset" -> {
                    getResetAlert().show()
                    return true
                }
                "delete_voices" -> {
                    getDeleteVoicesAlert().show()
                    return true
                }

                // Others
                "privacy" -> {
                    val privacyString = "https://cdn.rawgit.com/jehutyno/privacy-policies/56b6fcf3/PrivacyPolicyYomikataZ.html"
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyString))
                    startActivity(browserIntent)
                    return true
                }

                else -> {
                    throw Error("unknown preference key: ${preference.key}")
                }
            }
        }
    }

}
