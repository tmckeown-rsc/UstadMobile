package com.ustadmobile.port.android.view;

import android.content.Context;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.view.ReportAtRiskStudentsView;
import com.ustadmobile.core.view.ReportAttendanceGroupedByThresholdsView;
import com.ustadmobile.core.view.ReportNumberOfDaysClassesOpenView;
import com.ustadmobile.core.view.ReportOverallAttendanceView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExpandableListDataReports {


    Integer icon;
    String name;
    List<ExpandableListDataReports> children;
    String reportLink;
    Boolean showThreshold;
    Boolean showRadioGroup;
    Boolean showGenderDisaggregate;
    String desc;

    ExpandableListDataReports(){

    }

    ExpandableListDataReports(String name, Integer icon, String reportLink, boolean showThreshold,
                              boolean showRadioGroup, boolean showGenderDisaggregate){
        this.name = name;
        this.icon = icon;
        this.reportLink = reportLink;
        this.showThreshold = showThreshold;
        this.showRadioGroup = showRadioGroup;
        this.showGenderDisaggregate = showGenderDisaggregate;
    }

    ExpandableListDataReports(String name, Integer icon, String reportLink, boolean showThreshold,
                              boolean showRadioGroup, boolean showGenderDisaggregate,
                              String description){
        this.name = name;
        this.icon = icon;
        this.reportLink = reportLink;
        this.showThreshold = showThreshold;
        this.showRadioGroup = showRadioGroup;
        this.showGenderDisaggregate = showGenderDisaggregate;
        this.desc = description;
    }

    public static HashMap<String, ExpandableListDataReports> getDataAll(Context context){
        HashMap<String,ExpandableListDataReports> expandableListDetail = new HashMap<>();

        List<ExpandableListDataReports> attendanceReport = new ArrayList<>();
        attendanceReport.add(new ExpandableListDataReports(
                context.getText(R.string.overall_attendance).toString(),
                -1, ReportOverallAttendanceView.VIEW_NAME, false,
                false, true,
                context.getText(R.string.overall_attendance).toString()));
        attendanceReport.add(new ExpandableListDataReports(
                context.getText(R.string.attendance_grouped_by_threshold).toString(),
                -1, ReportAttendanceGroupedByThresholdsView.VIEW_NAME, true,
                true, true,
                context.getText(R.string.attendance_grouped_by_threshold).toString()));
        attendanceReport.add(new ExpandableListDataReports(
                context.getText(R.string.at_risk_students).toString(),
                -1, ReportAtRiskStudentsView.VIEW_NAME, false,
                false, true,
                context.getText(R.string.at_risk_report_desc).toString()));



        List<ExpandableListDataReports> operationsReport = new ArrayList<>();
        operationsReport.add(new ExpandableListDataReports(
                context.getText(R.string.number_of_days_classes_open).toString(),
                -1, ReportNumberOfDaysClassesOpenView.VIEW_NAME,
                false, false, false,
                context.getText(R.string.number_of_days_classes_open).toString()));

        List<ExpandableListDataReports> ircMasterListReport = new ArrayList<>();

        List<ExpandableListDataReports> selReport = new ArrayList<>();


        ExpandableListDataReports attendanceObj = new ExpandableListDataReports();
        attendanceObj.icon = R.drawable.ic_assignment_turned_in_bcd4_24dp;
        attendanceObj.name = context.getText(R.string.attendance_report).toString();
        attendanceObj.children = attendanceReport;
        attendanceObj.desc = attendanceObj.name;

        ExpandableListDataReports operationsObj = new ExpandableListDataReports();
        operationsObj.icon = R.drawable.ic_account_balance_cyan_24dp;
        operationsObj.name = context.getText(R.string.attendance_report).toString();
        operationsObj.children = operationsReport;
        operationsObj.desc = operationsObj.name;

        ExpandableListDataReports ircMasterObj = new ExpandableListDataReports();
        ircMasterObj.icon = R.drawable.ic_event_cyan_24dp;
        ircMasterObj.name = context.getText(R.string.irc_master_list_report).toString();
        ircMasterObj.children = ircMasterListReport;
        ircMasterObj.desc = ircMasterObj.name;
        //TODO: Sprint 5
        //ircMasterObj.reportLink = ReportMasterView.VIEW_NAME;
        ircMasterObj.showGenderDisaggregate = false;
        ircMasterObj.showRadioGroup = false;
        ircMasterObj.showThreshold = false;

        ExpandableListDataReports selObj = new ExpandableListDataReports();
        selObj.icon = R.drawable.ic_tag_faces_cyan_24dp;
        selObj.name = context.getText(R.string.sel_report).toString();
        //TODO: Sprint 5
        //selObj.reportLink = ReportSELReportView.VIEW_NAME;
        selObj.showGenderDisaggregate = false;
        selObj.showRadioGroup = false;
        selObj.showThreshold = false;
        selObj.children = selReport;
        selObj.desc = selObj.name;

        expandableListDetail.put(context.getText(R.string.sel_report).toString(),
                selObj);
        expandableListDetail.put(context.getText(R.string.irc_master_list_report).toString(),
                ircMasterObj);
        expandableListDetail.put(context.getText(R.string.operations_report).toString(),
                operationsObj);
        expandableListDetail.put(context.getText(R.string.attendance_report).toString(),
                attendanceObj);


        return expandableListDetail;
    }

}
