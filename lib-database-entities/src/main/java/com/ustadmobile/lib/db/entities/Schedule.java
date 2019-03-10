package com.ustadmobile.lib.db.entities;

import com.ustadmobile.lib.database.annotation.UmEntity;
import com.ustadmobile.lib.database.annotation.UmPrimaryKey;
import com.ustadmobile.lib.database.annotation.UmSyncLastChangedBy;
import com.ustadmobile.lib.database.annotation.UmSyncLocalChangeSeqNum;
import com.ustadmobile.lib.database.annotation.UmSyncMasterChangeSeqNum;

@UmEntity(tableId = 21)
public class Schedule {

    public static final int SCHEDULE_FREQUENCY_DAILY = 1;
    public static final int SCHEDULE_FREQUENCY_WEEKLY = 2;

    public static final int SCHEDULE_FREQUENCY_ONCE = 3;
    public static final int SCHEDULE_FREQUENCY_MONTHLY = 4;
    public static final int SCHEDULE_FREQUENCY_YEARLY = 5;

    public static final int DAY_SUNDAY = 1;
    public static final int DAY_MONDAY = 2;
    public static final int DAY_TUESDAY = 3;
    public static final int DAY_WEDNESDAY = 4;
    public static final int DAY_THURSDAY = 5;
    public static final int DAY_FRIDAY = 6;
    public static final int DAY_SATURDAY = 7;


    public static final int MONTH_JANUARY = 1;
    public static final int MONTH_FEBUARY = 2;
    public static final int MONTH_MARCH = 3;
    public static final int MONTH_APRIL = 4;
    public static final int MONTH_MAY = 5;
    public static final int MONTH_JUNE = 6;
    public static final int MONTH_JULY = 7;
    public static final int MONTH_AUGUST = 8;
    public static final int MONTH_SEPTEMBER = 9;
    public static final int MONTH_OCTOBER = 10;
    public static final int MONTH_NOVEMBER = 11;
    public static final int MONTH_DECEMBER = 12;


    @UmPrimaryKey(autoGenerateSyncable = true)
    private long scheduleUid;

    //Start time
    private long sceduleStartTime;

    //End time
    private long scheduleEndTime;

    //What day for this frequency
    private int scheduleDay;

    //What month for this frequency
    private int scheduleMonth;

    // Frequency - Once, Daily, Every Week, Every Month, Every Year
    private int scheduleFrequency;

    //The Calendar this will be set to.
    private long umCalendarUid;

    //What clazz is this Schedule for
    private long scheduleClazzUid;

    @UmSyncMasterChangeSeqNum
    private long scheduleMasterChangeSeqNum;

    @UmSyncLocalChangeSeqNum
    private long scheduleLocalChangeSeqNum;

    @UmSyncLastChangedBy
    private int scheduleLastChangedBy;

    //active or removed
    private boolean scheduleActive;

    public Schedule() {

    }


    public boolean isScheduleActive() {
        return scheduleActive;
    }

    public void setScheduleActive(boolean scheduleActive) {
        this.scheduleActive = scheduleActive;
    }

    public long getScheduleClazzUid() {
        return scheduleClazzUid;
    }

    public void setScheduleClazzUid(long scheduleClazzUid) {
        this.scheduleClazzUid = scheduleClazzUid;
    }

    /**
     * Get the time of day that this schedule is to begin. This should be in ms from the beginning of
     * the day. E.g. 14:30 = (14.5 * 60 * 60 * 1000) ms
     *
     * @return time of the day that class is to begin for this scheduled instance
     */
    public long getSceduleStartTime() {
        return sceduleStartTime;
    }

    /**
     * Set the time of day that this schedule is to begin. This should be in ms from the beginning of
     * the day. E.g. 14:30 = (14.5 * 60 * 60 * 1000) ms
     *
     * @param sceduleStartTime time of the day that class is to begin for this scheduled instance
     */
    public void setSceduleStartTime(long sceduleStartTime) {
        this.sceduleStartTime = sceduleStartTime;
    }

    /**
     *
     * @return
     */
    public long getScheduleEndTime() {
        return scheduleEndTime;
    }

    public void setScheduleEndTime(long scheduleEndTime) {
        this.scheduleEndTime = scheduleEndTime;
    }

    public int getScheduleDay() {
        return scheduleDay;
    }

    public void setScheduleDay(int scheduleDay) {
        this.scheduleDay = scheduleDay;
    }

    public int getScheduleMonth() {
        return scheduleMonth;
    }

    public void setScheduleMonth(int scheduleMonth) {
        this.scheduleMonth = scheduleMonth;
    }


    public int getScheduleFrequency() {
        return scheduleFrequency;
    }

    public void setScheduleFrequency(int scheduleFrequency) {
        this.scheduleFrequency = scheduleFrequency;
    }

    public long getUmCalendarUid() {
        return umCalendarUid;
    }

    public void setUmCalendarUid(long umCalendarUid) {
        this.umCalendarUid = umCalendarUid;
    }

    public long getScheduleUid() {
        return scheduleUid;
    }

    public void setScheduleUid(long scheduleUid) {
        this.scheduleUid = scheduleUid;
    }

    public long getScheduleMasterChangeSeqNum() {
        return scheduleMasterChangeSeqNum;
    }

    public void setScheduleMasterChangeSeqNum(long scheduleMasterChangeSeqNum) {
        this.scheduleMasterChangeSeqNum = scheduleMasterChangeSeqNum;
    }

    public long getScheduleLocalChangeSeqNum() {
        return scheduleLocalChangeSeqNum;
    }

    public void setScheduleLocalChangeSeqNum(long scheduleLocalChangeSeqNum) {
        this.scheduleLocalChangeSeqNum = scheduleLocalChangeSeqNum;
    }

    public int getScheduleLastChangedBy() {
        return scheduleLastChangedBy;
    }

    public void setScheduleLastChangedBy(int scheduleLastChangedBy) {
        this.scheduleLastChangedBy = scheduleLastChangedBy;
    }
}