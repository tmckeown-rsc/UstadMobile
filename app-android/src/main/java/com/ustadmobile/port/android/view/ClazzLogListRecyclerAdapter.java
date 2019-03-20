package com.ustadmobile.port.android.view;

import android.arch.paging.PagedListAdapter;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.Fragment;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.controller.ClazzLogListPresenter;
import com.ustadmobile.core.generated.locale.MessageID;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.util.UMCalendarUtil;
import com.ustadmobile.lib.db.entities.ClazzLogWithScheduleStartEndTimes;
import com.ustadmobile.lib.db.entities.Schedule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * The ClazzLogList's recycler adapter.
 */
public class ClazzLogListRecyclerAdapter extends
        PagedListAdapter<ClazzLogWithScheduleStartEndTimes, ClazzLogListRecyclerAdapter.ClazzLogViewHolder> {

    Context theContext;
    private Fragment theFragment;
    private ClazzLogListPresenter thePresenter;
    private Boolean showImage;

    class ClazzLogViewHolder extends RecyclerView.ViewHolder{
        ClazzLogViewHolder(View itemView){
            super(itemView);
        }
    }

    ClazzLogListRecyclerAdapter(@NonNull DiffUtil.ItemCallback<ClazzLogWithScheduleStartEndTimes>
              diffCallback, Context context, Fragment fragment, ClazzLogListPresenter mPresenter,
                                          boolean imageShow){
        super(diffCallback);
        theContext = context;
        theFragment = fragment;
        thePresenter = mPresenter;
        showImage = imageShow;
    }

    @NonNull
    @Override
    public ClazzLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View clazzLogListItem =
                LayoutInflater.from(theContext).inflate(
                        R.layout.item_clazzlog_log, parent, false);
        return new ClazzLogViewHolder(clazzLogListItem);

    }

    /**
     *
     * Gets the appropriate string of schedule frequency and returns MessageID code for applicable
     * string.
     * @param frequency The Schedule freqency (from Schedule entity's scheduleFrequency field)
     * @return  MessageID code for applicable string.
     */
    private static int frequencyToMessageID(int frequency){
        int frequencyId = 0;
        switch(frequency){
            case Schedule.SCHEDULE_FREQUENCY_DAILY:
                frequencyId = MessageID.daily;
                break;
            case Schedule.SCHEDULE_FREQUENCY_WEEKLY:
                frequencyId = MessageID.weekly;
                break;
            case Schedule.SCHEDULE_FREQUENCY_ONCE:
                frequencyId = MessageID.once;
                break;
            case Schedule.SCHEDULE_FREQUENCY_MONTHLY:
                frequencyId = MessageID.monthly;
                break;
            case Schedule.SCHEDULE_FREQUENCY_YEARLY:
                frequencyId = MessageID.yearly;
                break;
            default:
                break;
        }
        return frequencyId;
    }

    /**
     * This method sets the elements after it has been obtained for that item'th position.
     *
     * For every item part of the recycler adapter, this will be called and every item in it
     * will be set as per this function.
     *
     */
    @Override
    public void onBindViewHolder(@NonNull ClazzLogViewHolder holder, int position){
        ClazzLogWithScheduleStartEndTimes clazzLog = getItem(position);
        assert clazzLog != null;

        Locale currentLocale = theFragment.getResources().getConfiguration().locale;
        UstadMobileSystemImpl impl = UstadMobileSystemImpl.getInstance();

        String prettyDate =
                UMCalendarUtil.getPrettyDateFromLong(clazzLog.getLogDate(), currentLocale);

        //Get frequency of schedule

        String frequencyName =
                impl.getString(frequencyToMessageID(clazzLog.getScheduleFrequency()),
                    theContext);

        //Add time to ClazzLog's date
        long startTimeLong = clazzLog.getSceduleStartTime();
        long endTimeLong = clazzLog.getScheduleEndTime();
        DateFormat formatter = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);

        //start time
        long startMins = startTimeLong / (1000 * 60);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, (int)(startMins / 60));
        cal.set(Calendar.MINUTE, (int)(startMins % 60));
        String startTime = formatter.format(cal.getTime());

        //end time
        long endMins = endTimeLong / (1000 * 60);
        cal.set(Calendar.HOUR_OF_DAY, (int)(endMins / 60));
        cal.set(Calendar.MINUTE, (int)(endMins % 60));
        String endTime = formatter.format(cal.getTime());

        prettyDate = prettyDate + " (" + frequencyName + ", " + startTime + " - " + endTime + ")";

        String prettyShortDay =
                UMCalendarUtil.getSimpleDayFromLongDate(clazzLog.getLogDate(), currentLocale);

        ImageView secondaryTextImageView =
                holder.itemView.findViewById(R.id.item_clazzlog_log_status_text_imageview);

        int presentCount = clazzLog.getNumPresent();
        int absentCount = clazzLog.getNumAbsent();
        int partialCount = clazzLog.getNumPartial();
        String clazzLogAttendanceStatus ;
        if(partialCount > 0){
            clazzLogAttendanceStatus = presentCount + " " +
                    theFragment.getText(R.string.present) + ", " + absentCount + " " +
                    theFragment.getText(R.string.absent)  + ", " + partialCount + " " +
                    theFragment.getText(R.string.partial);
        }else{
            clazzLogAttendanceStatus = presentCount + " " +
                    theFragment.getText(R.string.present) + ", " + absentCount + " " +
                    theFragment.getText(R.string.absent);
        }

        TextView statusTextView = holder.itemView.findViewById(R.id.item_clazzlog_log_status_text);

        AppCompatImageView doneIV = holder.itemView.findViewById(R.id.item_clazzlog_log_done_image);

        if(clazzLog.isDone()){
            //Update doneIV to tick
            doneIV.setImageResource(R.drawable.ic_check_black_24dp);
        }else{
            //Update doneIV to pencil
            doneIV.setImageResource(R.drawable.ic_edit);
        }
        ((TextView)holder.itemView.findViewById(R.id.item_clazzlog_log_date)).setText(prettyDate);
        ((TextView)holder.itemView.findViewById(R.id.item_clazzlog_log_day)).setText(prettyShortDay);
        statusTextView.setText(clazzLogAttendanceStatus);

        if(!showImage){
            secondaryTextImageView.setVisibility(View.INVISIBLE);

            //Change the constraint layout so that the hidden bits are not empty spaces.
            ConstraintLayout cl = holder.itemView.findViewById(R.id.item_clazzlog_log_cl);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(cl);

            constraintSet.connect(R.id.item_clazzlog_log_status_text,
                    ConstraintSet.START, R.id.item_clazzlog_log_calendar_image,
                    ConstraintSet.END, 16);

            constraintSet.applyTo(cl);


        }else{
            secondaryTextImageView.setVisibility(View.VISIBLE);
        }


        holder.itemView.setOnClickListener(v -> thePresenter.goToClazzLogDetailActivity(clazzLog));
    }
}