package com.ustadmobile.core.db.dao;

import com.ustadmobile.core.db.UmLiveData;
import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.lib.database.annotation.UmDao;
import com.ustadmobile.lib.database.annotation.UmInsert;
import com.ustadmobile.lib.database.annotation.UmQuery;
import com.ustadmobile.lib.database.annotation.UmRepository;
import com.ustadmobile.lib.database.annotation.UmUpdate;
import com.ustadmobile.lib.database.annotation.UmSyncCheckIncomingCanUpdate;
import com.ustadmobile.lib.db.entities.Clazz;
import com.ustadmobile.lib.db.entities.ClazzAverage;
import com.ustadmobile.lib.db.entities.ClazzMember;
import com.ustadmobile.lib.db.entities.ClazzWithEnrollment;
import com.ustadmobile.lib.db.entities.ClazzWithNumStudents;
import com.ustadmobile.lib.db.entities.Location;
import com.ustadmobile.lib.db.entities.Role;
import com.ustadmobile.lib.db.sync.UmSyncExistingEntity;
import com.ustadmobile.lib.db.sync.dao.SyncableDao;

import java.util.List;

import static com.ustadmobile.core.db.dao.ClazzDao.ENTITY_LEVEL_PERMISSION_CONDITION1;
import static com.ustadmobile.core.db.dao.ClazzDao.ENTITY_LEVEL_PERMISSION_CONDITION2;
import static com.ustadmobile.core.db.dao.ClazzDao.TABLE_LEVEL_PERMISSION_CONDITION1;
import static com.ustadmobile.core.db.dao.ClazzDao.TABLE_LEVEL_PERMISSION_CONDITION2;

@UmDao(
selectPermissionCondition = ENTITY_LEVEL_PERMISSION_CONDITION1 + Role.PERMISSION_CLAZZ_SELECT +
        ENTITY_LEVEL_PERMISSION_CONDITION2,
updatePermissionCondition = ENTITY_LEVEL_PERMISSION_CONDITION1 + Role.PERMISSION_CLAZZ_UPDATE +
        ENTITY_LEVEL_PERMISSION_CONDITION2,
insertPermissionCondition = TABLE_LEVEL_PERMISSION_CONDITION1 + Role.PERMISSION_CLAZZ_INSERT +
        TABLE_LEVEL_PERMISSION_CONDITION2)
@UmRepository
public abstract class ClazzDao implements SyncableDao<Clazz, ClazzDao> {

    protected static final String ENTITY_LEVEL_PERMISSION_CONDITION1 =
            " (SELECT admin FROM Person WHERE personUid = :accountPersonUid) OR " +
            "EXISTS(SELECT PersonGroupMember.groupMemberPersonUid FROM PersonGroupMember " +
            "JOIN EntityRole ON EntityRole.erGroupUid = PersonGroupMember.groupMemberGroupUid " +
            "JOIN Role ON EntityRole.erRoleUid = Role.roleUid " +
            "WHERE PersonGroupMember.groupMemberPersonUid = :accountPersonUid " +
            " AND (" +
            "(EntityRole.ertableId = " + Clazz.TABLE_ID +
            " AND EntityRole.erEntityUid = Clazz.clazzUid) " +
            "OR" +
            "(EntityRole.ertableId = " + Location.TABLE_ID +
            " AND EntityRole.erEntityUid IN (SELECT locationAncestorAncestorLocationUid " +
            " FROM LocationAncestorJoin WHERE locationAncestorChildLocationUid = " +
                    " Clazz.clazzLocationUid))" +
            ") AND (Role.rolePermissions & ";

    protected static final String ENTITY_LEVEL_PERMISSION_CONDITION2 = ") > 0)";

    protected static final String TABLE_LEVEL_PERMISSION_CONDITION1 =
            "(SELECT admin FROM Person WHERE personUid = :accountPersonUid) " +
            "OR " +
            "EXISTS(SELECT PersonGroupMember.groupMemberPersonUid FROM PersonGroupMember " +
            " JOIN EntityRole ON EntityRole.erGroupUid = PersonGroupMember.groupMemberGroupUid " +
            " JOIN Role ON EntityRole.erRoleUid = Role.roleUid " +
            " WHERE " +
            " PersonGroupMember.groupMemberPersonUid = :accountPersonUid " +
            " AND EntityRole.erTableId = " + Clazz.TABLE_ID +
            " AND Role.rolePermissions & ";

    protected static final String TABLE_LEVEL_PERMISSION_CONDITION2 = " > 0)";

    public static final String CLAZZ_WHERE = " SELECT Clazz.*, (SELECT COUNT(*) " +
            " FROM ClazzMember WHERE " +
            " ClazzMember.clazzMemberClazzUid = Clazz.clazzUid " +
            " AND ClazzMember.role = " + ClazzMember.ROLE_STUDENT +
            " AND ClazzMember.clazzMemberActive = 1) AS numStudents, " +
            " (SELECT COUNT(*) FROM ClazzMember " +
            " WHERE ClazzMember.clazzMemberClazzUid = Clazz.clazzUid " +
            " AND ClazzMember.role = " + ClazzMember.ROLE_TEACHER +
            " AND ClazzMember.clazzMemberActive = 1 ) AS numTeachers, " +
            " (SELECT GROUP_CONCAT(Person.firstNames ||  Person.lastName ) as teacherName " +
            " FROM Person where Person.personUid in (SELECT ClazzMember.clazzMemberPersonUid " +
            " FROM ClazzMember WHERE ClazzMember.role = " + ClazzMember.ROLE_TEACHER  +
            " AND ClazzMember.clazzMemberClazzUid = Clazz.clazzUid" +
            " AND ClazzMember.clazzMemberActive = 1) " +
            " ) AS teacherNames " ;


    @Override
    @UmInsert
    public abstract long insert(Clazz entity);

    @Override
    @UmInsert
    public abstract void insertAsync(Clazz entity, UmCallback<Long> resultObject);

    @UmQuery("SELECT * FROM Clazz WHERE clazzUid = :uid")
    public abstract Clazz findByUid(long uid);

    @UmQuery("SELECT * From Clazz WHERE clazzUid = :uid")
    public abstract UmLiveData<Clazz> findByUidLive(long uid);

    @UmQuery("SELECT * FROM Clazz WHERE clazzUid = :uid")
    public abstract void findByUidAsync(long uid, UmCallback<Clazz> resultObject);

    @UmUpdate
    public abstract void updateAsync(Clazz entity, UmCallback<Integer> resultObject);

    @UmQuery(CLAZZ_WHERE +
            " FROM Clazz WHERE :personUid in " +
            " (SELECT ClazzMember.clazzMemberPersonUid FROM ClazzMember " +
                "  WHERE ClazzMember.clazzMemberClazzUid = Clazz.clazzUid)")
    public abstract UmProvider<ClazzWithNumStudents> findAllClazzesByPersonUid(long personUid);

    @UmQuery(CLAZZ_WHERE +
            " FROM Clazz ")
    public abstract UmProvider<ClazzWithNumStudents> findAllClazzes();

    @UmQuery(CLAZZ_WHERE +
            " FROM Clazz WHERE clazzLocationUid in (:locations)")
    public abstract UmProvider<ClazzWithNumStudents> findAllClazzesInLocationList(List<Long> locations);

    @UmQuery(CLAZZ_WHERE +
            " FROM Clazz WHERE clazzLocationUid in (:locations)")
    public abstract void findAllClazzesInLocationListAsync(List<Long> locations,
                                                   UmCallback<List<ClazzWithNumStudents>> resultList);


    @UmQuery("SELECT * FROM Clazz WHERE clazzUid in (:clazzUidList) AND clazzActive = 1")
    public abstract void findClazzesByUidListAsync(List<Long> clazzUidList, UmCallback<List<Clazz>> resultList);

    @UmQuery("SELECT * FROM Clazz WHERE clazzActive = 1")
    public abstract void findAllActiveClazzesAsync(UmCallback<List<Clazz>> resultList);

    @UmQuery("SELECT * FROM Clazz WHERE clazzLocationUid IN (:allLocations) " +
            " OR clazzUid in (:allClasses) AND clazzActive = 1")
    public abstract void findAllClazzesInUidAndLocationAsync(List<Long> allLocations,
                                                         List<Long> allClasses,
                                                         UmCallback<List<Clazz>> resultList);

    @UmQuery("SELECT * FROM Clazz WHERE  clazzUid in (:allClasses) AND clazzActive = 1")
    public abstract void findAllClazzesInUidAsync(List<Long> allClasses,
                                                             UmCallback<List<Clazz>> resultList);

    @UmQuery("SELECT * FROM Clazz WHERE clazzLocationUid IN (:allLocations) " +
            " AND clazzActive = 1")
    public abstract void findAllClazzesInLocationAsync(List<Long> allLocations,
                                                             UmCallback<List<Clazz>> resultList);

    public void findAllClazzesByLocationAndUidList(List<Long> allLocations,
                                                   List<Long> allClazzes,
                                                   UmCallback<List<Clazz>> resultList){
        if(allLocations.isEmpty() && allClazzes.isEmpty()){
            findAllActiveClazzesAsync(resultList);
        }else{
            if(allLocations.isEmpty() && !allClazzes.isEmpty()){
                findAllClazzesInUidAsync(allClazzes, resultList);
            }else if(!allLocations.isEmpty() && allClazzes.isEmpty()){
                findAllClazzesInLocationAsync(allLocations, resultList);
            }else{
                findAllClazzesInUidAndLocationAsync(allLocations, allClazzes, resultList);
            }
        }
    }

    @UmQuery(CLAZZ_WHERE +
            " FROM Clazz WHERE Clazz.clazzActive = 1 ")
    public abstract UmProvider<ClazzWithNumStudents> findAllActiveClazzes();

    @UmQuery(CLAZZ_WHERE +
            " FROM Clazz WHERE Clazz.clazzActive = 1 " +
            " ORDER BY Clazz.clazzName ASC")
    public abstract UmProvider<ClazzWithNumStudents> findAllActiveClazzesSortByNameAsc();

    @UmQuery(CLAZZ_WHERE +
            " FROM Clazz WHERE Clazz.clazzActive = 1 " +
            " ORDER BY Clazz.clazzName DESC")
    public abstract UmProvider<ClazzWithNumStudents> findAllActiveClazzesSortByNameDesc();
    @UmQuery(CLAZZ_WHERE +
            " FROM Clazz WHERE Clazz.clazzActive = 1 " +
            " ORDER BY Clazz.attendanceAverage ASC ")
    public abstract UmProvider<ClazzWithNumStudents> findAllActiveClazzesSortByAttendanceAsc();
    @UmQuery(CLAZZ_WHERE +
            " FROM Clazz WHERE Clazz.clazzActive = 1 " +
            " ORDER BY Clazz.attendanceAverage DESC ")
    public abstract UmProvider<ClazzWithNumStudents> findAllActiveClazzesSortByAttendanceDesc();

    @UmQuery(CLAZZ_WHERE +
            " FROM Clazz WHERE Clazz.clazzActive = 1 " +
            " ORDER BY teacherNames ASC ")
    public abstract UmProvider<ClazzWithNumStudents> findAllActiveClazzesSortByTeacherAsc();

    @UmQuery("SELECT * FROM Clazz WHERE clazzName = :name")
    public abstract void findByClazzNameAsync(String name, UmCallback<List<Clazz>> resultList);


    @UmQuery("SELECT " +
            " (SELECT COUNT(*) FROM Clazz Where Clazz.clazzActive = 1) as numClazzes, " +
            " (SELECT COUNT(*) FROM ClazzMember WHERE ClazzMember.clazzMemberActive = 1 " +
            " AND ClazzMember.role = " + ClazzMember.ROLE_STUDENT + ") as numStudents, " +
            " (SELECT COUNT(*) FROM ClazzMember WHERE ClazzMember.clazzMemberActive = 1 " +
            " AND ClazzMember.role = " + ClazzMember.ROLE_TEACHER + ") as numTeachers, " +
            " ((SELECT SUM(Clazz.attendanceAverage) FROM Clazz WHERE Clazz.clazzActive = 1 ) / " +
            " (SELECT COUNT(*) FROM Clazz Where Clazz.clazzActive = 1)) as attendanceAverage ")
    public abstract void getClazzSummaryAsync(UmCallback<ClazzAverage> resultObject);

    @UmQuery(
        "SELECT Clazz.*, (:personUid) AS personUid, " +
            "(SELECT COUNT(*) FROM ClazzMember " +
            "WHERE ClazzMember.clazzMemberClazzUid = Clazz.clazzUid AND ClazzMember.role = 1) " +
                " AS numStudents, " +
            "(SELECT (EXISTS (SELECT * FROM ClazzMember WHERE clazzMemberPersonUid = :personUid " +
                " AND clazzMemberClazzUid = Clazz.clazzUid " +
                " ))) AS enrolled " +
            "FROM Clazz WHERE Clazz.clazzActive = 1")
    public abstract UmProvider<ClazzWithEnrollment>
                                            findAllClazzesWithEnrollmentByPersonUid(long personUid);

    @UmQuery(CLAZZ_WHERE +
            " FROM Clazz WHERE :personUid in " +
            " (SELECT ClazzMember.clazzMemberPersonUid FROM ClazzMember " +
            " WHERE ClazzMember.clazzMemberClazzUid = Clazz.clazzUid)")
    public abstract List<ClazzWithNumStudents> findAllClazzesByPersonUidAsList(long personUid);

    @UmQuery("Update Clazz SET attendanceAverage " +
            " = (SELECT COUNT(*) FROM ClazzLogAttendanceRecord  " +
            " LEFT JOIN ClazzLog " +
            " ON ClazzLogAttendanceRecord.clazzLogAttendanceRecordClazzLogUid = ClazzLog.clazzLogUid " +
            " WHERE ClazzLog.done = 1 " +
            " AND ClazzLog.clazzLogClazzUid = :clazzUid " +
            " AND ClazzLogAttendanceRecord.attendanceStatus = 1) * 1.0 " +
            " /  " +
            "MAX(1, (SELECT COUNT(*) FROM ClazzLogAttendanceRecord  " +
            "LEFT JOIN ClazzLog " +
            " ON ClazzLogAttendanceRecord.clazzLogAttendanceRecordClazzLogUid = ClazzLog.clazzLogUid " +
            "WHERE ClazzLog.done = 1 " +
            " AND ClazzLog.clazzLogClazzUid = :clazzUid " +
            ")) * 1.0 " +
            "Where Clazz.clazzUid = :clazzUid")
    public abstract void updateAttendancePercentage(long clazzUid);

    /** Check if a permission is present on a specific entity e.g. update/modify etc*/
    @UmQuery("SELECT 1 FROM Clazz WHERE Clazz.clazzUid = :clazzUid AND (" + ENTITY_LEVEL_PERMISSION_CONDITION1 +
            " :permission" + ENTITY_LEVEL_PERMISSION_CONDITION2 + ")")
    public abstract void personHasPermission(long accountPersonUid, long clazzUid, long permission,
                                             UmCallback<Boolean> callback);

    @UmQuery("SELECT " + TABLE_LEVEL_PERMISSION_CONDITION1 + " :permission "
            + TABLE_LEVEL_PERMISSION_CONDITION2 + " AS hasPermission")
    public abstract void personHasPermission(long accountPersonUid, long permission,
                                             UmCallback<Boolean> callback);

    @UmQuery("SELECT Clazz.clazzUid as primaryKey, " +
            "(" + ENTITY_LEVEL_PERMISSION_CONDITION1 + Role.PERMISSION_CLAZZ_UPDATE + ENTITY_LEVEL_PERMISSION_CONDITION2 + ") " +
                " AS userCanUpdate " +
            " FROM Clazz WHERE Clazz.clazzUid in (:primaryKeys)")
    @UmSyncCheckIncomingCanUpdate
    public abstract List<UmSyncExistingEntity> syncFindExistingEntities(List<Long> primaryKeys,
                                                                        long accountPersonUid);

    @UmQuery("SELECT COUNT(*) FROM Clazz " +
            "WHERE " +
            "clazzLocalChangeSeqNum > (SELECT syncedToLocalChangeSeqNum FROM SyncStatus WHERE tableId = 6) " +
            "AND clazzLastChangedBy = (SELECT deviceBits FROM SyncDeviceBits LIMIT 1) " +
            "AND ((" + ENTITY_LEVEL_PERMISSION_CONDITION1 + Role.PERMISSION_CLAZZ_UPDATE + //can update it
                ENTITY_LEVEL_PERMISSION_CONDITION2 + ") " +
            " OR (" + TABLE_LEVEL_PERMISSION_CONDITION1 + Role.PERMISSION_CLAZZ_INSERT + //can insert on table
                TABLE_LEVEL_PERMISSION_CONDITION2 + "))")
    public abstract int countPendingLocalChanges(long accountPersonUid);

}
