package com.ustadmobile.port.android.view;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.view.ClassLogListView;

/**
 * ClassLogListFragment Android fragment extends UstadBaseFragment
 */
public class ClassLogListFragment extends UstadBaseFragment implements ClassLogListView,
        View.OnClickListener, View.OnLongClickListener {

    View rootContainer;
    //RecyclerView
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mRecyclerLayoutManager;
    private RecyclerView.Adapter mAdapter;


    /**
     * Generates a new Fragment for a page fragment
     * TODO: Add any args if needed
     *
     * @return A new instance of fragment ClassLogListFragment.
     */
    public static ClassLogListFragment newInstance() {
        ClassLogListFragment fragment = new ClassLogListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * On Create of the fragment.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * On Create of the View fragment . Part of Android's Fragment Override
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return the root container
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // TODO: Inflate the layout for this fragment
        rootContainer = inflater.inflate(R.layout.fragment_class_log_list, container, false);
        setHasOptionsMenu(true);

        /*
        // TODO: Set mRecyclerView..
        mRecyclerView = rootContainer.findViewById(R.id.fragment_class_log_list_recyclerview);

        // TODO: Use Layout: set layout manager. Change defaults
        mRecyclerLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mRecyclerLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                LinearLayoutManager.VERTICAL);
        mRecyclerView.addItemDecoration(dividerItemDecoration);

        // TODO: Specify the mAdapter
        ////mAdapter = new CustomAdapter(getContext(), some_data);
        mRecyclerView.setAdapter(mAdapter);
        */

        //return container
        return rootContainer;
    }

    // This event is triggered soon after onCreateView().
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup any handles to view objects here

    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

}
