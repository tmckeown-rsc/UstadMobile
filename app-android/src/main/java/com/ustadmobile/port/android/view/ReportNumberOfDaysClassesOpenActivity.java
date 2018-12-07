package com.ustadmobile.port.android.view;


import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.ustadmobile.core.controller.ReportNumberOfDaysClassesOpenPresenter;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.ustadmobile.core.util.UMCalendarUtil;
import com.ustadmobile.lib.db.entities.UMCalendar;
import com.ustadmobile.port.android.util.UMAndroidUtil;
import com.toughra.ustadmobile.R;


import com.ustadmobile.core.view.ReportNumberOfDaysClassesOpenView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ru.dimorinny.floatingtextbutton.FloatingTextButton;


/**
 * The ReportNumberOfDaysClassesOpen activity.
 * <p>
 * This Activity extends UstadBaseActivity and implements ReportNumberOfDaysClassesOpenView
 */
public class ReportNumberOfDaysClassesOpenActivity extends UstadBaseActivity
        implements ReportNumberOfDaysClassesOpenView, PopupMenu.OnMenuItemClickListener {

    private Toolbar toolbar;

    private ReportNumberOfDaysClassesOpenPresenter mPresenter;
    BarChart barChart;
    TableLayout tableLayout;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Setting layout:
        setContentView(R.layout.activity_report_number_of_days_classes_open);

        //Toolbar:
        toolbar = findViewById(R.id.activity_report_number_of_days_classes_open_toolbar);
        toolbar.setTitle(R.string.number_of_days_classes_open);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.number_of_days_classes_open);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tableLayout = findViewById(R.id.activity_report_number_of_days_classes_open_table);

        //Call the Presenter
        mPresenter = new ReportNumberOfDaysClassesOpenPresenter(this,
                UMAndroidUtil.bundleToHashtable(getIntent().getExtras()), this);
        mPresenter.onCreate(UMAndroidUtil.bundleToHashtable(savedInstanceState));



        //FAB and its listener
        //eg:
        FloatingTextButton fab = findViewById(R.id.activity_report_number_of_days_classes_open_fab);
        fab.setOnClickListener(v -> showPopup(v));


    }

    public void setUpChart(){
        toolbar.setTitle(R.string.number_of_days_classes_open);
        barChart = findViewById(R.id.activity_report_number_of_days_classes_open_bar_chart);
        barChart.setMinimumHeight(dpToPx(BAR_CHART_HEIGHT));


        barChart.setMinimumWidth(10);
        barChart.getXAxis().setValueFormatter((value, axis) ->
                UMCalendarUtil.getPrettyDateSuperSimpleFromLong((long) (value)));
        barChart.setTouchEnabled(false);
    }


    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_export, popup.getMenu());
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.menu_export_csv) {
            mPresenter.dataToCSV();
            return true;
        } else if (i == R.id.menu_export_xls) {
            mPresenter.dataToXLS();
            return true;
        } else if (i == R.id.menu_export_json) {
            mPresenter.dataToJSON();
            return true;
        } else {
            return false;
        }
    }

    class AttendanceBarDataSet extends BarDataSet {

        AttendanceBarDataSet(List<BarEntry> yVals, String label) {
            super(yVals, label);
        }

        @Override
        public int getColor(int index) {
            return mColors.get(0);
        }

    }

    @Override
    public void updateBarChart(LinkedHashMap<Float, Float> dataMap) {

        Boolean hasSomething = false;

        //RENDER HERE

        List<BarEntry> barDataEntries = new ArrayList<>();
        for (Map.Entry<Float, Float> nextEntry : dataMap.entrySet()) {
            hasSomething = true;
            BarEntry anEntry = new BarEntry(nextEntry.getKey(),
                    nextEntry.getValue());
            barDataEntries.add(anEntry);
        }


        //Create Bar color
        BarDataSet dataSetBar1 = new BarDataSet(barDataEntries, BAR_LABEL);
        dataSetBar1.setValueTextColor(Color.BLACK);
        dataSetBar1.setDrawValues(true);
        dataSetBar1.setColor(Color.parseColor(BAR_CHART_BAR_COLOR));
        dataSetBar1.setValueFormatter(
                (value, entry, dataSetIndex, viewPortHandler) -> "" + ((int) value));


        BarData barData = new BarData(dataSetBar1);



        List<View> addThese = generateAllViewRowsForTable(dataMap);

        Boolean finalHasSomething = hasSomething;
        runOnUiThread(() -> {
            setUpChart();
            if(finalHasSomething){
                barChart.setData(barData);
                barChart.invalidate();
            }else{
                barChart.setData(null);
                barChart.invalidate();
            }

            for(View everyRow: addThese){
                tableLayout.addView(everyRow);

            }

        });
    }


    /**
     * Converts dp to pixels (used in MPAndroid charts)
     *
     * @param dp    dp number
     * @return      pixels number
     */
    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public List<View> generateAllViewRowsForTable(LinkedHashMap<Float, Float> dataTableMaps ){

        List<View> addThese = new ArrayList<>();


        //LAYOUT
        TableRow.LayoutParams rowParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);

        TableRow.LayoutParams everyItemParam = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        everyItemParam.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));


        //HEADING
        TableRow headingRow = new TableRow(getApplicationContext());
        headingRow.setLayoutParams(rowParams);

        TextView dateHeading = new TextView(getApplicationContext());
        dateHeading.setTextColor(Color.BLACK);
        dateHeading.setLayoutParams(everyItemParam);
        dateHeading.setText(R.string.date);

        TextView valueHeading = new TextView(getApplicationContext());
        valueHeading.setTextColor(Color.BLACK);
        valueHeading.setLayoutParams(everyItemParam);
        valueHeading.setTextColor(R.string.day);

        //ADD HEADING
        addThese.add(headingRow);


        List<Float> dates = new ArrayList<>();
        dates.addAll(dataTableMaps.keySet());

        for(Float everyDate: dates){
            String everyDateString =
                    UMCalendarUtil.getPrettyDateSuperSimpleFromLong(everyDate.longValue());

            TableRow everyDateRow = new TableRow(getApplicationContext());
            everyDateRow.setLayoutParams(rowParams);

            TextView dateView = new TextView(getApplicationContext());
            dateView.setTextColor(Color.BLACK);
            dateView.setLayoutParams(everyItemParam);
            dateView.setText(everyDateString);

            TextView valueView = new TextView(getApplicationContext());
            valueView.setTextColor(Color.BLACK);
            valueView.setLayoutParams(everyItemParam);
            valueView.setText(String.valueOf(dataTableMaps.get(everyDate)));

            everyDateRow.addView(dateView);
            everyDateRow.addView(valueView);

            addThese.add(everyDateRow);
        }

        return addThese;

    }

}
