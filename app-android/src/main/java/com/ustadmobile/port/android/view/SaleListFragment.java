package com.ustadmobile.port.android.view;

import android.arch.lifecycle.LiveData;
import android.arch.paging.DataSource;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.controller.SaleListPresenter;
import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.core.view.SaleListView;
import com.ustadmobile.lib.db.entities.SaleListDetail;
import com.ustadmobile.port.android.util.UMAndroidUtil;

import java.util.Objects;

import ru.dimorinny.floatingtextbutton.FloatingTextButton;

public class SaleListFragment extends UstadBaseFragment implements SaleListView {


    View rootContainer;
    private RecyclerView mRecyclerView;
    private SaleListPresenter mPresenter;
    private FloatingTextButton fab;

    private Spinner sortSpinner;
    String[] sortSpinnerPresets;

    private Button allSalesButton, preOrdersButton, paymentsDueButton;

    private TextView allSalesCounter, preOrderCounter, paymentsDueCounter;

    public static SaleListFragment newInstance(){
        SaleListFragment fragment = new SaleListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void goToSearch(){
        mPresenter.handleClickSearch();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        rootContainer =
                inflater.inflate(R.layout.fragment_sale_list, container, false);
        setHasOptionsMenu(true);

        mRecyclerView = rootContainer.findViewById(R.id.activity_sale_list_recyclerview);
        RecyclerView.LayoutManager mRecyclerLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mRecyclerLayoutManager);

        fab = rootContainer.findViewById(R.id.activity_sale_list_fab);

        sortSpinner = rootContainer.findViewById(R.id.fragment_sale_list_sort_by_spinner);


        allSalesCounter = rootContainer.findViewById(R.id.fragment_sale_list_filter_all_sales_counter);
        preOrderCounter = rootContainer.findViewById(R.id.fragment_sale_list_filter_pre_orders_counter);
        paymentsDueCounter = rootContainer.findViewById(R.id.fragment_sale_list_filter_payments_due_counter);

        //set up Presenter
        mPresenter = new SaleListPresenter(getContext(),
                UMAndroidUtil.bundleToHashtable(getArguments()), this);
        mPresenter.onCreate(UMAndroidUtil.bundleToHashtable(savedInstanceState));

        //Button
        allSalesButton = rootContainer.findViewById(R.id.fragment_sale_list_filter_all_sales);
        preOrdersButton = rootContainer.findViewById(R.id.fragment_sale_list_filter_pre_orders);
        paymentsDueButton = rootContainer.findViewById(R.id.fragment_sale_list_filter_payments_due);

        paymentsDueButton.setVisibility(View.VISIBLE);

        allSalesButton.setOnClickListener(v -> {
            disableAllButtonSelected();
            mPresenter.filterAll();
            getTintedDrawable(allSalesButton.getBackground(), R.color.fab);

        });
        preOrdersButton.setOnClickListener(v -> {
            disableAllButtonSelected();
            mPresenter.filterPreOrder();
            getTintedDrawable(preOrdersButton.getBackground(), R.color.fab);
        });
        paymentsDueButton.setOnClickListener(v -> {
            disableAllButtonSelected();
            mPresenter.filterPaymentDue();
            getTintedDrawable(paymentsDueButton.getBackground(), R.color.fab);
        });

        //Sort handler
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPresenter.handleChangeSortOrder(id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        fab.setOnClickListener(v -> mPresenter.handleClickPrimaryActionButton());

        allSalesButton.callOnClick();

        return rootContainer;
    }

    @Override
    public void updateSortSpinner(String[] presets) {
        this.sortSpinnerPresets = presets;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(Objects.requireNonNull(getContext()),
                R.layout.item_simple_spinner, sortSpinnerPresets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);
    }

    @Override
    public void updatePreOrderCounter(int count) {
        if(count == 0){
            preOrderCounter.setVisibility(View.INVISIBLE);
        }else{
            preOrderCounter.setText(String.valueOf(count));
            preOrderCounter.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void updatePaymentDueCounter(int count) {
        if(count == 0){
            paymentsDueCounter.setVisibility(View.INVISIBLE);
        }else{
            paymentsDueCounter.setText(String.valueOf(count));
            paymentsDueCounter.setVisibility(View.VISIBLE);
        }
    }

    public void disableAllButtonSelected(){
        runOnUiThread(() -> {

            getTintedDrawable(allSalesButton.getBackground(), R.color.color_gray);
            getTintedDrawable(preOrdersButton.getBackground(), R.color.color_gray);
            getTintedDrawable(paymentsDueButton.getBackground(), R.color.color_gray);

        });
    }

    /**
     * Tints the drawable to the color. This method supports the Context compat tinting on drawables.
     *
     * @param drawable  The drawable to be tinted
     * @param color     The color of the tint
     */
    public void getTintedDrawable(Drawable drawable, int color) {
        drawable = DrawableCompat.wrap(drawable);
        int tintColor = ContextCompat.getColor(Objects.requireNonNull(getContext()), color);
        DrawableCompat.setTint(drawable, tintColor);
    }

    @Override
    public void finish() {

    }

    /**
     * The DIFF CALLBACK
     */
    public static final DiffUtil.ItemCallback<SaleListDetail> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SaleListDetail>() {
                @Override
                public boolean areItemsTheSame(SaleListDetail oldItem,
                                               SaleListDetail newItem) {
                    return oldItem.getSaleUid() == newItem.getSaleUid();
                }

                @Override
                public boolean areContentsTheSame(SaleListDetail oldItem,
                                                  SaleListDetail newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @Override
    public void setListProvider(UmProvider<SaleListDetail> listProvider, boolean paymentsDueTab,
                                boolean preOrderTab) {

        SaleListRecyclerAdapter recyclerAdapter =
                new SaleListRecyclerAdapter(DIFF_CALLBACK, mPresenter,paymentsDueTab, preOrderTab,
                        this, getContext());
        //A warning is expected
        DataSource.Factory<Integer, SaleListDetail> factory =
                (DataSource.Factory<Integer, SaleListDetail>)listProvider.getProvider();
        LiveData<PagedList<SaleListDetail>> data =
                new LivePagedListBuilder<>(factory, 20).build();
        data.observe(this, recyclerAdapter::submitList);

        mRecyclerView.setAdapter(recyclerAdapter);
    }


}