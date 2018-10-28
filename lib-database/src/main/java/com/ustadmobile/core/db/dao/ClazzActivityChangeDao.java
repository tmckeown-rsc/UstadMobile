package com.ustadmobile.core.db.dao;

import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.lib.database.annotation.UmDao;
import com.ustadmobile.lib.database.annotation.UmInsert;
import com.ustadmobile.lib.database.annotation.UmQuery;
import com.ustadmobile.lib.database.annotation.UmUpdate;
import com.ustadmobile.lib.db.entities.ClazzActivityChange;

@UmDao
public abstract  class ClazzActivityChangeDao implements BaseDao<ClazzActivityChange> {

    @UmInsert
    public abstract long insert(ClazzActivityChange entity);

    @UmUpdate
    public abstract void update(ClazzActivityChange entity);

    @UmInsert
    public abstract void insertAsync(ClazzActivityChange entity, UmCallback<Long> result);

    @UmQuery("SELECT * FROM ClazzActivityChange")
    public abstract UmProvider<ClazzActivityChange> findAllClazzActivityChanges();

    @UmUpdate
    public abstract void updateAsync(ClazzActivityChange entity, UmCallback<Integer> result);

    @UmQuery("SELECT * FROM ClazzActivityChange WHERE clazzActivityChangeUid = :uid")
    public abstract ClazzActivityChange findByUid(long uid);

    @UmQuery("SELECT * FROM ClazzActivityChange WHERE clazzActivityChangeTitle = :title")
    public abstract ClazzActivityChange findByTitle(String title);
}
