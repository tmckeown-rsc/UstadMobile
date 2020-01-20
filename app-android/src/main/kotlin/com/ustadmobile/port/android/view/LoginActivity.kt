package com.ustadmobile.port.android.view

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.toughra.ustadmobile.R
import com.ustadmobile.core.controller.LoginPresenter
import com.ustadmobile.core.impl.AppConfig
import com.ustadmobile.core.impl.UMAndroidUtil.bundleToMap
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.view.LoginView
import com.ustadmobile.port.android.sync.UmAppDatabaseSyncWorker
import org.acra.util.ToastSender
import java.util.concurrent.TimeUnit
import android.widget.LinearLayout
import com.ustadmobile.core.impl.UMAndroidUtil
import com.ustadmobile.core.impl.UmAccountManager

class LoginActivity : UstadBaseActivity(), LoginView {

    private var mPresenter: LoginPresenter? = null

    private var mServerUrl: String? = null

    private var mUsernameTextView: TextView? = null

    private var mPasswordTextView: TextView? = null

    private var mErrorTextView: TextView? = null

    private var mVersionTextView: TextView? = null

    private var mProgressBar: ProgressBar? = null

    private var mLoginButton: Button? = null

    private lateinit var registerMessage : TextView

    private lateinit var registerNow: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //reset back ?
        //finishAffinity()

        setUMToolbar(R.id.um_toolbar)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val showTB = UstadMobileSystemImpl.instance.getAppConfigBoolean(
                AppConfig.LOGIN_TOOLBAR_VISIBILITY, this)
        if(!showTB) {
            umToolbar.visibility = View.GONE
        }

        registerMessage = findViewById(R.id.activity_register_label)
        registerNow = findViewById(R.id.activity_register_now)
        mUsernameTextView = findViewById(R.id.activity_login_username)
        mPasswordTextView = findViewById(R.id.activity_login_password)
        mLoginButton = findViewById(R.id.activity_login_button_login)
        mErrorTextView = findViewById(R.id.activity_login_errormessage)
        mVersionTextView = findViewById(R.id.activity_login_version)
        mProgressBar = findViewById(R.id.progressBar)

        val repository = UmAccountManager.getRepositoryForActiveAccount(this)
        mPresenter = LoginPresenter(this, bundleToMap(intent.extras),
                this, repository, UstadMobileSystemImpl.instance)
        mPresenter!!.onCreate(bundleToMap(savedInstanceState))

        mProgressBar!!.isIndeterminate = true
        mProgressBar!!.scaleY = 3f
        findViewById<View>(R.id.activity_login_button_login).setOnClickListener { evt ->
            mPresenter!!.handleClickLogin(mUsernameTextView!!.text.toString(),
                    mPasswordTextView!!.text.toString(), mServerUrl!!)
        }

        registerNow.setOnClickListener {
            mPresenter!!.handleClickCreateAccount()
        }
    }

    override fun setInProgress(inProgress: Boolean) {
        mProgressBar!!.visibility = if (inProgress) View.VISIBLE else View.GONE
        mPasswordTextView!!.isEnabled = !inProgress
        mUsernameTextView!!.isEnabled = !inProgress
        mLoginButton!!.isEnabled = !inProgress
        mLoginButton!!.background.alpha = if (inProgress) 128 else 255
    }

    override fun setMessage(message: String) {
        Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT
        ).show()
    }

    override fun setErrorMessage(errorMessage: String) {
        mErrorTextView!!.setTextColor(getResources().getColor(android.R.color.holo_red_dark))
        mErrorTextView!!.visibility = View.VISIBLE
        mErrorTextView!!.text = errorMessage
    }

    override fun setServerUrl(serverUrl: String) {
        this.mServerUrl = serverUrl
    }

    override fun setUsername(username: String) {
        mUsernameTextView!!.text = username
    }

    override fun setPassword(password: String) {
        mPasswordTextView!!.text = password
    }

    override fun updateVersionOnLogin(version: String) {
        mVersionTextView!!.visibility = View.VISIBLE
        mVersionTextView!!.setTextColor(getResources().getColor(R.color.text_primary))
        mVersionTextView!!.text = version
    }

    override fun setFinishAfficinityOnView() {
        runOnUiThread { finishAffinity() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun showRegisterCodeDialog(title: String, okButtonText: String, cancelButtonText: String) {

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        val pixels = UMAndroidUtil.convertDpToPixel(24)
        lp.setMargins(pixels, 0, pixels, 0)
        val input = EditText(this)
        input.layoutParams = lp
        input.requestLayout()
        container.addView(input, lp)

        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setPositiveButton(okButtonText){  dialogInterface , _ ->
            mPresenter!!.handleRegisterCodeDialogEntered(input.text.toString())
        }
        builder.setNegativeButton(cancelButtonText){ dialogInterface, _ ->
            dialogInterface.cancel()
        }
        builder.setView(container)
        builder.show()

    }

    override fun setRegistrationLinkVisible(visible: Boolean) {
        val visibility = if(visible) View.VISIBLE else View.GONE
        registerMessage.visibility = visibility
        registerNow.visibility = visibility
    }

    override fun forceSync() {
        WorkManager.getInstance().cancelAllWorkByTag(UmAppDatabaseSyncWorker.TAG)
        UmAppDatabaseSyncWorker.queueSyncWorkerWithPolicy(100, TimeUnit.MILLISECONDS,
                ExistingWorkPolicy.APPEND)
        ToastSender.sendToast(applicationContext, "Sync started", 42)
        WorkManager.getInstance().getWorkInfosByTagLiveData(UmAppDatabaseSyncWorker.TAG).observe(
                this, Observer{ workInfos ->
            for (wi in workInfos) {
                if (wi.getState().isFinished()) {
                    ToastSender.sendToast(applicationContext, "Sync finished", 42)
                }
            }
        })
    }
}
