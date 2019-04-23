package com.ustadmobile.core.db.dao;

import com.ustadmobile.core.db.UmLiveData;
import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.lib.database.annotation.UmDao;
import com.ustadmobile.lib.database.annotation.UmInsert;
import com.ustadmobile.lib.database.annotation.UmQuery;
import com.ustadmobile.lib.database.annotation.UmRepository;
import com.ustadmobile.lib.database.annotation.UmUpdate;
import com.ustadmobile.lib.db.entities.SalePayment;
import com.ustadmobile.lib.db.sync.dao.SyncableDao;

import java.util.List;

@UmDao(updatePermissionCondition = RoleDao.SELECT_ACCOUNT_IS_ADMIN, insertPermissionCondition =
        RoleDao.SELECT_ACCOUNT_IS_ADMIN)
@UmRepository
public abstract class SalePaymentDao implements SyncableDao<SalePayment, SalePaymentDao> {

    //INSERT

    @UmInsert
    public abstract void insertAsync(SalePayment entity, UmCallback<Long> insertCallback);


    //FIND ALL ACTIVE

    public static final String ALL_ACTIVE_QUERY =
            "SELECT * FROM SalePayment WHERE salePaymentActive = 1";
    public static final String FIND_ALL_BY_SALE_UID_QUERY =
            "SELECT * FROM SalePayment WHERE salePaymentSaleUid = :saleUid AND " +
                    "salePaymentActive = 1";

    @UmQuery(ALL_ACTIVE_QUERY)
    public abstract UmLiveData<List<SalePayment>> findAllActiveLive();

    @UmQuery(ALL_ACTIVE_QUERY)
    public abstract List<SalePayment> findAllActiveList();

    @UmQuery(ALL_ACTIVE_QUERY)
    public abstract void findAllActiveAsync(UmCallback<List<SalePayment>> allActiveCallback);

    @UmQuery(ALL_ACTIVE_QUERY)
    public abstract UmProvider<SalePayment> findAllActiveProvider();


    @UmQuery(FIND_ALL_BY_SALE_UID_QUERY)
    public abstract UmLiveData<List<SalePayment>> findBySaleUidLive(long saleUid);

    @UmQuery(FIND_ALL_BY_SALE_UID_QUERY)
    public abstract void findBySaleUidAsync(long saleUid,
                           UmCallback<List<SalePayment>> allPaymentsBySaleCallback);

    @UmQuery(FIND_ALL_BY_SALE_UID_QUERY)
    public abstract List<SalePayment> findBySaleAsList(long saleUid);


    @UmQuery(FIND_ALL_BY_SALE_UID_QUERY)
    public abstract UmProvider<SalePayment> findBySaleProvider(long saleUid);

    public static final String TOTAL_PAID_BY_SALE_UID =
            "SELECT SUM(salePaymentPaidAmount) FROM SalePayment " +
            "WHERE salePaymentSaleUid = :saleUid AND salePaymentActive = 1 " +
            "AND salePaymentDone = 1";
    @UmQuery(TOTAL_PAID_BY_SALE_UID)
    public abstract long findTotalPaidPaymentsInASale(long saleUid);

    @UmQuery(TOTAL_PAID_BY_SALE_UID)
    public abstract void findTotalPaidBySaleAsync(long saleUid, UmCallback<Long> resultCallback);


    //LOOK UP

    public static final String FIND_BY_UID_QUERY =
            "SELECT * FROM SalePayment WHERE salePaymentUid = :uid";


    @UmQuery(FIND_BY_UID_QUERY)
    public abstract SalePayment findByUid(long uid);

    @UmQuery(FIND_BY_UID_QUERY)
    public abstract void findByUidAsync(long uid, UmCallback<SalePayment> findByUidCallback);

    @UmQuery(FIND_BY_UID_QUERY)
    public abstract UmLiveData<SalePayment> findByUidLive(long uid);



    //INACTIVATE:

    public static final String INACTIVATE_QUERY =
            "UPDATE SalePayment SET salePaymentActive = 0 WHERE salePaymentUid = :uid";
    @UmQuery(INACTIVATE_QUERY)
    public abstract void inactivateEntity(long uid);

    @UmQuery(INACTIVATE_QUERY)
    public abstract void inactivateEntityAsync(long uid, UmCallback<Integer> inactivateCallback);


    //UPDATE:

    @UmUpdate
    public abstract void updateAsync(SalePayment entity, UmCallback<Integer> updateCallback);
}