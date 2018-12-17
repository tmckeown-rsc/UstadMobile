package com.ustadmobile.port.android.view;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.controller.Login2Presenter;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.view.Login2View;
import com.ustadmobile.core.view.Register2View;
import com.ustadmobile.port.android.util.UMAndroidUtil;

import java.util.Hashtable;

public class Login2Activity extends UstadBaseActivity implements Login2View {

    private Login2Presenter mPresenter;

    private String mServerUrl;

    private TextView mUsernameTextView;

    private TextView mPasswordTextView;

    private TextView mErrorTextView;

    private ProgressBar progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login2);

        setUMToolbar(R.id.um_toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        Hashtable args = UMAndroidUtil.bundleToHashtable(getIntent().getExtras());
        mPresenter = new Login2Presenter(this, args, this);
        mPresenter.onCreate(UMAndroidUtil.bundleToHashtable(savedInstanceState));
        mUsernameTextView = findViewById(R.id.activity_login_username);
        mPasswordTextView = findViewById(R.id.activity_login_password);
        mErrorTextView = findViewById(R.id.activity_login_errormessage);
        progressDialog = findViewById(R.id.progressBar);
        progressDialog.setIndeterminate(true);
        progressDialog.setScaleY(3f);


        findViewById(R.id.activity_login_button_login).setOnClickListener(
                (evt) -> mPresenter.handleClickLogin(mUsernameTextView.getText().toString(),
                        mPasswordTextView.getText().toString(), mServerUrl));
        findViewById(R.id.user_registration).setOnClickListener(
                v -> UstadMobileSystemImpl.getInstance().go(Register2View.VIEW_NAME,args,
                        getContext()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == android.R.id.home){
            finish();
        }
        return true;
    }

    @Override
    public void setInProgress(boolean inProgress) {
        progressDialog.setVisibility(inProgress ? View.VISIBLE:View.GONE);
    }

    @Override
    public void setErrorMessage(String errorMessage) {
        mErrorTextView.setText(errorMessage);
    }

    @Override
    public void setServerUrl(String serverUrl) {
        this.mServerUrl = serverUrl;
    }

    @Override
    public void setUsername(String username) {
        mUsernameTextView.setText(username);
    }

    @Override
    public void setPassword(String password) {
        mPasswordTextView.setText(password);
    }
}
