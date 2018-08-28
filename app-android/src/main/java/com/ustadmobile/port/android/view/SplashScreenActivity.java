/*
    This file is part of Ustad Mobile.

    Ustad Mobile Copyright (C) 2011-2014 UstadMobile Inc.

    Ustad Mobile is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version with the following additional terms:

    All names, links, and logos of Ustad Mobile and Toughra Technologies FZ
    LLC must be kept as they are in the original distribution.  If any new
    screens are added you must include the Ustad Mobile logo as it has been
    used in the original distribution.  You may not create any new
    functionality whose purpose is to diminish or remove the Ustad Mobile
    Logo.  You must leave the Ustad Mobile logo as the logo for the
    application to be used with any launcher (e.g. the mobile app launcher).

    If you want a commercial license to remove the above restriction you must
    contact us.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    Ustad Mobile is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

 */

package com.ustadmobile.port.android.view;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.db.dao.ClazzDao;
import com.ustadmobile.core.db.dao.ClazzLogDao;
import com.ustadmobile.core.db.dao.ClazzMemberDao;
import com.ustadmobile.core.db.dao.PersonDao;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.lib.db.entities.Clazz;
import com.ustadmobile.lib.db.entities.ClazzMember;
import com.ustadmobile.lib.db.entities.Person;

import java.util.Calendar;


public class SplashScreenActivity extends AppCompatActivity implements DialogInterface.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback{

    public static final int EXTERNAL_STORAGE_REQUESTED = 1;

    public static final String[] REQUIRED_PERMISSIONS = new String[]{
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    };

    boolean rationalesShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        addDummyData();
    }

    public void checkPermissions() {
        boolean hasRequiredPermissions = true;
        for(int i = 0; i < REQUIRED_PERMISSIONS.length; i++) {
            hasRequiredPermissions &= ContextCompat.checkSelfPermission(this, REQUIRED_PERMISSIONS[i]) == PackageManager.PERMISSION_GRANTED;
        }

        if(!hasRequiredPermissions){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && !rationalesShown) {
                //show an alert
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("File permissions required").setMessage("This app requires file permissions on the SD card to download and save content");
                builder.setPositiveButton("OK", this);
                AlertDialog dialog = builder.create();
                dialog.show();
                rationalesShown = true;
                return;
            }else {
                rationalesShown = false;
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, EXTERNAL_STORAGE_REQUESTED);
                return;
            }
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                UstadMobileSystemImpl.getInstance().startUI(SplashScreenActivity.this);
            }
        }, 0);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        checkPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean allGranted = permissions.length == 2;
        for(int i = 0; i < grantResults.length; i++) {
            allGranted &= grantResults[i] == PackageManager.PERMISSION_GRANTED;
        }

        if(allGranted) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    UstadMobileSystemImpl.getInstance().startUI(SplashScreenActivity.this);
                }
            }, 0);
        }else {
            //avoid possibly getting into an infinite loop if we had no user interaction and permission was denied
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    try { Thread.sleep(500); }
                    catch(InterruptedException e) {}
                    return null;
                }

                @Override
                protected void onPostExecute(Void o) {
                    SplashScreenActivity.this.checkPermissions();
                }
            }.execute();
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        checkPermissions();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_leavecontainer) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void addDummyData(){
        String createStatus = UstadMobileSystemImpl.getInstance().getAppPref("dummydata",
                getApplicationContext());
        if(createStatus != null)
            return;

        ClazzDao clazzDao =
                UmAppDatabase.getInstance(getApplicationContext()).getClazzDao();
        ClazzMemberDao clazzMemberDao = UmAppDatabase.getInstance(getApplicationContext())
                .getClazzMemberDao();
        PersonDao personDao =
                UmAppDatabase.getInstance(getApplicationContext()).getPersonDao();

        //Get today's date



        new Thread(() -> {

            Long currentLogDate = -1L;
            Calendar attendanceDate = Calendar.getInstance();
            attendanceDate.setTimeInMillis(System.currentTimeMillis());
            attendanceDate.set(Calendar.HOUR_OF_DAY, 0);
            attendanceDate.set(Calendar.MINUTE, 0);
            attendanceDate.set(Calendar.SECOND, 0);
            attendanceDate.set(Calendar.MILLISECOND, 0);
            currentLogDate = attendanceDate.getTimeInMillis();

            for(int i = 0; i < 5; i++) {
                Person person = new Person();
                person.setFirstName("Ahmed");
                person.setLastName("Khalil" + i);

                long thisPersonUid = personDao.insert(person);
                person.setPersonUid(thisPersonUid);

            }

            Clazz clazz1 = new Clazz();
            clazz1.setClazzName("Class 1");
            clazz1.setAttendanceAverage(42);
            long thisClazzUid = clazzDao.insert(clazz1);
            clazz1.setClazzUid(thisClazzUid);

            for(int i = 0; i < 5; i++) {

                ClazzMember member = new ClazzMember();
                member.setRole(ClazzMember.ROLE_STUDENT);
                member.setClazzMemberPersonUid(i);
                member.setClazzMemberClazzUid(thisClazzUid);
                member.setAttendancePercentage(42L);
                clazzMemberDao.insertAsync(member, null);
            }

            //Create a ClassLog for today for Class 1
            ClazzLogDao clazzLogDao = UmAppDatabase.getInstance(getApplicationContext()).getClazzLogDao();

            clazzLogDao.createClazzLogForDate(clazz1.getClazzUid(), currentLogDate, new UmCallback<Long>() {
                @Override
                public void onSuccess(Long result) {
                    System.out.println("Success in creating ClazzLog for class1");
                }

                @Override
                public void onFailure(Throwable exception) {
                    System.out.println(exception);
                }
            });

            UstadMobileSystemImpl.getInstance().setAppPref("dummydata", "created",
                    getApplicationContext());
        }).start();



    }


}
