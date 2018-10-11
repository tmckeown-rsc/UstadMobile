package com.ustadmobile.port.android.view;


import com.ustadmobile.core.controller.SELQuestionPresenter;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.ustadmobile.port.android.util.UMAndroidUtil;
import com.toughra.ustadmobile.R;


import com.ustadmobile.core.view.SELQuestionView;

import ru.dimorinny.floatingtextbutton.FloatingTextButton;


/**
 * The SELQuestion activity.
 * <p>
 * This Activity extends UstadBaseActivity and implements SELQuestionView
 */
public class SELQuestionActivity extends UstadBaseActivity implements SELQuestionView {

    private Toolbar toolbar;

    //RecyclerView
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mRecyclerLayoutManager;
    private RecyclerView.Adapter mAdapter; //replaced with object in set view provider method.
    private SELQuestionPresenter mPresenter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Setting layout:
        setContentView(R.layout.activity_sel_question);

        //Toolbar:
        toolbar = findViewById(R.id.activity_sel_question_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Recycler View:
        mRecyclerView = (RecyclerView) findViewById(
                R.id.activity_sel_question_recyclerview);
        mRecyclerLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mRecyclerLayoutManager);

        //Call the Presenter
        mPresenter = new SELQuestionPresenter(this,
                UMAndroidUtil.bundleToHashtable(getIntent().getExtras()), this);
        mPresenter.onCreate(UMAndroidUtil.bundleToHashtable(savedInstanceState));

        //FAB and its listener
        //eg:
        FloatingTextButton fab = findViewById(R.id.activity_sel_question_fab);
        fab.setOnClickListener(v -> mPresenter.handleClickPrimaryActionButton(-1));


    }


}
