package com.ustadmobile.core.view;


/**
 * Core View. Screen is for ReportDetail's View
 */
public interface ReportDetailView extends UstadView {


    // This defines the view name that is an argument value in the go() in impl.
    String VIEW_NAME = "ReportDetail";

    //Any argument keys:

    /**
     * Method to finish the screen / view.
     */
    void finish();

    void setTitle(String title);

    void showDownloadButton(boolean show);

    void showAddToDashboardButton(boolean show);

    void setReportType(int reportType);

    void showSalesPerformanceReport();

    void showSalesLogReport();

    void showTopLEsReport();


}
