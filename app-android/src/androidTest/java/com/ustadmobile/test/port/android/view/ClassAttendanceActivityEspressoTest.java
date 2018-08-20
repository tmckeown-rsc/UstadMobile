package com.ustadmobile.test.port.android.view;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.AllOf.allOf;
import static android.support.test.espresso.assertion.ViewAssertions.matches;


/**
 * ClassAttendanceActivity's Espresso UI Test for Android
 */
@RunWith(AndroidJUnit4.class)
public class ClassAttendanceActivityEspressoTest {

    /**
     * This sets the activity that we want floating around
     */

    @BeforeClass
    public static void beforeClass() {
        //Before class stuff here
    }

    @Before
    public void beforeTest() {
        //Before here..
    }

    @Test
    public void givenAppLoads_whenClassAttendanceActivityStarts_shouldTakeAttendance() {
        //TODO: Write tests

        //Assert loads with details
        //Assert can fill in details and toggle
        //Assert Click Done closes the activity.
        //Assert attendance updated in the DAO.
        //Assert outstanding attendanve is gone from the Feed

    }

    @Test
    public void givenAttendanceForClassTakenLoads_whenDetailsChanged_shouldUpdateDetails(){
        //TODO: this

        //Assert attendane open for already taken.
        //Assert details filled in as per current attendance entry in Dao
        //Change details and click done assert by opening same attendance for class+date again.
        //
    }

    @Test
    public void givenAttendanceForClassLoads_whenDateTabSwiped_showConsecutiveDateAttendance(){
        //TODO this

        //Assert can swipe towards left to show previous day's attendance.
        //Assert show previous dates attendance details if taken or not.
        //Assert can swipe towards right to show next day's attendance if current page is
        // at most yesterday's date
        //Assert can't swipe future date's attendance.
        //Assert can take attendance for previous date. Check via DAOs.
    }

}
