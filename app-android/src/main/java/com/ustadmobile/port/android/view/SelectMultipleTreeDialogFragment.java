package com.ustadmobile.port.android.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.controller.SelectMultipleTreeDialogPresenter;
import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.db.dao.LocationDao;
import com.ustadmobile.core.impl.UmAccountManager;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.core.view.DismissableDialog;
import com.ustadmobile.core.view.SelectMultipleTreeDialogView;
import com.ustadmobile.lib.db.entities.Location;
import com.ustadmobile.port.android.util.UMAndroidUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import io.reactivex.annotations.NonNull;
import tellh.com.recyclertreeview_lib.TreeNode;
import tellh.com.recyclertreeview_lib.TreeViewAdapter;
import tellh.com.recyclertreeview_lib.TreeViewBinder;

/**
 * The activity that is a full screen dialog that selects items from a tree structure.
 * Designed to be common for both Location and Classes (although classes wont be in a tree structure).
 * The data should be provided to it to load.
 */
public class SelectMultipleTreeDialogFragment extends UstadDialogFragment implements
        SelectMultipleTreeDialogView, AdapterView.OnItemSelectedListener,
        DialogInterface.OnClickListener, DialogInterface.OnShowListener,
        View.OnClickListener, DismissableDialog  {


    AlertDialog dialog;
    View rootView;

    //Recycler view for the tree
    private RecyclerView recyclerView;
    //Adapter for tree
    private TreeViewAdapter adapter;

    //Context (activity calling this)
    private Context mAttachedContext;

    Toolbar toolbar;
    LocationDao locationDao;

    HashMap<String, Long> selectedOptions;

    SelectMultipleTreeDialogPresenter mPresenter;

    List<Long> selectedLocationList;

    public interface MultiSelectTreeDialogListener {

        void onLocationResult(HashMap<String, Long> selected);

    }

    private class PopulateTreeNodeCallback implements UmCallback<List<Location>> {

        private TreeNode node;

        private PopulateTreeNodeCallback(TreeNode node) {
            this.node = node;
        }

        @Override
        public void onSuccess(List<Location> result) {
            runOnUiThread(() -> {
                for(Location childLocations : result) {
                    long childLocationUid = childLocations.getLocationUid();
                    boolean selected = false;
                    if(selectedLocationList.contains(childLocationUid)){
                        selected = true;
                    }

                    node.addChild(new TreeNode<>(
                            new LocationLayoutType(childLocations.getTitle(),
                                    childLocationUid, selected, false)));
                }
                if(!result.isEmpty()){
                    ((LocationLayoutType)node.getContent()).leaf = false;
                }else{
                    ((LocationLayoutType)node.getContent()).leaf = true;
                }

            });

            if(!result.isEmpty()){
                ((LocationLayoutType)node.getContent()).leaf = false;
            }else{
                ((LocationLayoutType)node.getContent()).leaf = true;
            }
        }

        @Override
        public void onFailure(Throwable exception) {

        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem item =  menu.findItem(R.id.menu_catalog_entry_presenter_share);

        //Get the icon itself.
        Drawable itemIcon = getResources().getDrawable(R.drawable.ic_check_white_24dp);
        itemIcon.setTint(getResources().getColor(R.color.icons));
        itemIcon.setColorFilter(getResources().getColor(R.color.icons), PorterDuff.Mode.SRC_IN);
        item.setIcon(itemIcon);

    }

    @android.support.annotation.NonNull
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater =
                (LayoutInflater) Objects.requireNonNull(getContext()).getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;

        rootView = inflater.inflate(R.layout.fragment_select_multiple_tree_dialog , null);

        //Set up Recycler view
        initView();

        //Get initial data
        //List<TreeNode> nodes = initTestData();

        toolbar = rootView.findViewById(R.id.fragment_select_multiple_tree_dialog_toolbar);
        toolbar.setTitle(R.string.select_locations);



        Drawable upIcon = AppCompatResources.getDrawable(getContext(),
                R.drawable.ic_arrow_back_white_24dp);
        upIcon = getTintedDrawable(upIcon, R.color.icons);

        toolbar.setNavigationIcon(upIcon);
        toolbar.setNavigationOnClickListener(v -> dialog.dismiss());

        toolbar.inflateMenu(R.menu.menu_done);
        toolbar.setOnMenuItemClickListener(item -> {
            int i = item.getItemId();
            if (i == R.id.menu_catalog_entry_presenter_share) {
                mPresenter.handleClickPrimaryActionButton();
            }
            return false;
        });

        UmAppDatabase repository = UmAccountManager.getRepositoryForActiveAccount(getContext());
        locationDao = repository.getLocationDao();

        mPresenter = new SelectMultipleTreeDialogPresenter(getContext(),
                UMAndroidUtil.bundleToHashtable(getArguments()), this);
        mPresenter.onCreate(UMAndroidUtil.bundleToHashtable(savedInstanceState));

        selectedLocationList = mPresenter.getSelectedLocationsList();

        dialog = new AlertDialog.Builder(getContext(), R.style.FullScreenDialogStyle)
                .setView(rootView)
                .setTitle("")
                .create();
        return dialog;

    }

    public Drawable getTintedDrawable(Drawable drawable, int color) {
        drawable = DrawableCompat.wrap(drawable);
        int tintColor = ContextCompat.getColor(getContext(), color);
        DrawableCompat.setTint(drawable, tintColor);
        return drawable;
    }

    /**
     * Custom TreeView Adapter written so that we can work with onBindView and manipulate the
     * view on every tree node.
     *
     */
    public class TreeViewAdapterWithBind extends TreeViewAdapter{

        public TreeViewAdapterWithBind(List<TreeNode> nodes, List<? extends TreeViewBinder> viewBinders) {
            super(nodes, viewBinders);
        }


        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            CheckBox locationCB = holder.itemView.findViewById(
                    R.id.item_select_multiple_tree_dialog_checkbox);
            ImageView arrowIV = holder.itemView.findViewById(
                    R.id.item_select_multiple_tree_dialog_arrow);

            Iterator<TreeNode> displayNodesIterator = getDisplayNodesIterator();
            TreeNode displayNode = null;
            int i=0;
             while(displayNodesIterator.hasNext()){
                  displayNode = displayNodesIterator.next();
                  if(i>=position){
                      break;
                  }else{
                      i++;
                  }
             }

            LocationLayoutType displayNodeContent = (LocationLayoutType) displayNode.getContent();
            long locationUid = displayNodeContent.uid;

            if (selectedLocationList != null && selectedLocationList.contains(locationUid)){
                locationCB.setChecked(true);
            }else{
                locationCB.setChecked(false);
            }

            arrowIV.setVisibility(displayNodeContent.leaf?View.INVISIBLE:View.VISIBLE);

        }
    }

    private void initView(){
        //Set recycler view
        recyclerView = rootView.findViewById(R.id.fragment_select_multiple_tree_dialog_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void populateTopLocation(List<Location> locations) {
        List<TreeNode> nodes = new ArrayList<>();

        for(Location every_location : locations){
            long childLocationUid = every_location.getLocationUid();
            boolean selected = false;
            if(selectedLocationList.contains(childLocationUid)){
                selected = true;
            }
            TreeNode<LocationLayoutType> app = new TreeNode<>(
                    new LocationLayoutType(
                            every_location.getTitle(), every_location.getLocationUid(),
                            selected, false
                    )
            );
            nodes.add(app);
        }

        for(TreeNode childNode : nodes) {
            long childLocationUid = ((LocationLayoutType) childNode.getContent()).getUid();
            locationDao.findAllChildLocationsForUidAsync(childLocationUid,
                    new PopulateTreeNodeCallback(childNode));
        }

        //Init adapter with the location node binder as types of data to accept
        adapter = new TreeViewAdapterWithBind(nodes, Arrays.asList(new LocationNodeBinder(mPresenter)));

        //Set adapter to Recycler view.
        runOnUiThread(() -> recyclerView.setAdapter(adapter));


        //Set adapter listener
        adapter.setOnTreeNodeListener(new TreeViewAdapter.OnTreeNodeListener() {

            @Override
            public boolean onClick(TreeNode treeNode, RecyclerView.ViewHolder viewHolder) {
                if(!treeNode.isLeaf()){
                    List<TreeNode> nodeList = treeNode.getChildList();
                    for(TreeNode childNode : nodeList) {
                        if(childNode.isLeaf()) {
                            //Find all child's children and add then to the node
                            // (via PopulateTreeNodeCallback class)
                            long childLocationUid = ((LocationLayoutType) childNode.getContent()).getUid();
                            locationDao.findAllChildLocationsForUidAsync(childLocationUid,
                                    new PopulateTreeNodeCallback(childNode));
                        }
                    }
                    onToggle(treeNode.isExpand(), viewHolder);
                }

                return false;
            }

            @Override
            public void onToggle(boolean b, RecyclerView.ViewHolder viewHolder) {

                //Change icon of the item.
                LocationNodeBinder.ViewHolder locationViewHolder =
                        (LocationNodeBinder.ViewHolder) viewHolder;
                ImageView arrowImage = locationViewHolder.getIvArrow();
                int rotateDegree = b ? 90 : -90;
                arrowImage.animate().rotationBy(rotateDegree).start();

            }
        });
    }

    @Override
    public void finish() {
        selectedOptions = mPresenter.getSelectedOptions();
        if(mAttachedContext instanceof MultiSelectTreeDialogListener) {
            ((MultiSelectTreeDialogListener)mAttachedContext).onLocationResult(selectedOptions);
        }
        dialog.dismiss();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mAttachedContext = context;
        this.selectedOptions = new HashMap<>();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mAttachedContext = null;
        this.selectedOptions = null;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

    }

    @Override
    public void onShow(DialogInterface dialog) {

    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}