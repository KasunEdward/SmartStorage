package com.smartstorage.app.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.smartstorage.app.R;
import com.smartstorage.app.recycler.Adapter;
import com.smartstorage.app.ui.InputDialog;
import com.smartstorage.app.utils.FileUtils;
import com.smartstorage.app.utils.PreferenceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.smartstorage.app.utils.FileUtils.*;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,ServiceCallbacks{
    final Context context=this;
    private MyService myService;
    private boolean bound = false;
    DriveId driveId;

    private boolean fileOperation=false;

    private static GoogleApiClient mGoogleApiClient;
    private static final String GOOGLE_DRIVE_TAG="Google Drive";
    private static final String DROP_BOX_TAG="DropBox";
    private static final int REQUEST_CODE_RESOLUTION = 1;
    private static final  int REQUEST_CODE_OPENER = 2;

    private static final String SAVED_DIRECTORY = "com.calintat.explorer.SAVED_DIRECTORY";

    private static final String SAVED_SELECTION = "com.calintat.explorer.SAVED_SELECTION";

    private static final String EXTRA_NAME = "com.calintat.explorer.EXTRA_NAME";

    private static final String EXTRA_TYPE = "com.calintat.explorer.EXTRA_TYPE";

    private CollapsingToolbarLayout toolbarLayout;

    private CoordinatorLayout coordinatorLayout;

    private DrawerLayout drawerLayout;

    private NavigationView navigationView;

    private Toolbar toolbar;

    private File currentDirectory;

    private Adapter adapter;

    private String name;

    private String type;

    SharedPreferences prefs=null;

    //----------------------------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {

        initActivityFromIntent();

        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        prefs=getSharedPreferences("com.smartstorage..app",MODE_PRIVATE);
//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addApi(Drive.API)
//                .addScope(Drive.SCOPE_FILE)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .build();
//        mGoogleApiClient.connect();

        initAppBarLayout();

        initCoordinatorLayout();

        initDrawerLayout();

        initFloatingActionButton();

        initNavigationView();

        initRecyclerView();

        loadIntoRecyclerView();

        invalidateToolbar();

        invalidateTitle();
    }

    @Override
    public void onBackPressed() {

        if (drawerLayout.isDrawerOpen(navigationView)) {

            drawerLayout.closeDrawers();

            return;
        }

        if (adapter.anySelected()) {

            adapter.clearSelection();

            return;
        }

        if (!FileUtils.isStorage(currentDirectory)) {

            setPath(currentDirectory.getParentFile());

            return;
        }

        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 0) {

            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                Snackbar.make(coordinatorLayout, "Permission required", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Settings", v -> gotoApplicationSettings())
                        .show();
            }
            else {

                loadIntoRecyclerView();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if(prefs.getBoolean("firstrun",true)){
//            setDriveAccount();
//            prefs.edit().putBoolean("firstrun",false).commit();
//
//        }
        setDriveAccount();
        Log.i(GOOGLE_DRIVE_TAG,"OnResume........");
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        Log.i(GOOGLE_DRIVE_TAG,"Thread running finished........");
        Intent intent = new Intent(this, MyService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        if(bound){
            myService.doStuff();
        }
        if (adapter != null) adapter.refresh();


//        if (mGoogleApiClient == null) {
//
//            /**
//             * Create the API client and bind it to an instance variable.
//             * We use this instance as the callback for connection and connection failures.
//             * Since no account name is passed, the user is prompted to choose.
//             */
//            mGoogleApiClient = new GoogleApiClient.Builder(this)
//                    .addApi(Drive.API)
//                    .addScope(Drive.SCOPE_FILE)
//                    .addConnectionCallbacks(this)
//                    .addOnConnectionFailedListener(this)
//                    .build();
//        }
//
//        mGoogleApiClient.connect();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {

        adapter.select(savedInstanceState.getIntegerArrayList(SAVED_SELECTION));

        String path = savedInstanceState.getString(SAVED_DIRECTORY, getInternalStorage().getPath());

        if (currentDirectory != null) setPath(new File(path));

        super.onRestoreInstanceState(savedInstanceState);
    }
    @Override
    public void onStart(){
        super.onStart();
        // bind to Service
        Log.i(GOOGLE_DRIVE_TAG,"OnStart.............");
        Intent intent = new Intent(this, MyService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        if(bound){
            myService.doStuff();

        }

    }
    @Override
    protected void onStop(){
        super.onStop();
        // Unbind from service
        if (bound) {
            myService.setCallbacks(null); // unregister
            unbindService(serviceConnection);
            bound = false;
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putIntegerArrayList(SAVED_SELECTION, adapter.getSelectedPositions());

        outState.putString(SAVED_DIRECTORY, getPath(currentDirectory));

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.action, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_delete:
                actionDelete();
                return true;

            case R.id.action_rename:
                actionRename();
                return true;

            case R.id.action_search:
                actionSearch();
                return true;

            case R.id.action_copy:
                actionCopy();
                return true;

            case R.id.action_move:
                actionMove();
                return true;

            case R.id.action_send:
                actionSend();
                return true;

            case R.id.action_sort:
                actionSort();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (adapter != null) {

            int count = adapter.getSelectedItemCount();

            menu.findItem(R.id.action_delete).setVisible(count >= 1);

            menu.findItem(R.id.action_rename).setVisible(count >= 1);

            menu.findItem(R.id.action_search).setVisible(count == 0);

            menu.findItem(R.id.action_copy).setVisible(count >= 1 && name == null && type == null);

            menu.findItem(R.id.action_move).setVisible(count >= 1 && name == null && type == null);

            menu.findItem(R.id.action_send).setVisible(count >= 1);

            menu.findItem(R.id.action_sort).setVisible(count == 0);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    //----------------------------------------------------------------------------------------------

    private void initActivityFromIntent() {

        name = getIntent().getStringExtra(EXTRA_NAME);

        type = getIntent().getStringExtra(EXTRA_TYPE);

        if (type != null) {

            switch (type) {

                case "audio":
                    setTheme(R.style.app_theme_Audio);
                    break;

                case "image":
                    setTheme(R.style.app_theme_Image);
                    break;

                case "video":
                    setTheme(R.style.app_theme_Video);
                    break;
            }
        }
    }

    private void loadIntoRecyclerView() {

        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;

        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, permission)) {

            ActivityCompat.requestPermissions(this, new String[]{permission}, 0);

            return;
        }

        final Context context = this;

        if (name != null) {

            adapter.addAll(FileUtils.searchFilesName(context, name));

            return;
        }

        if (type != null) {

            switch (type) {

                case "audio":
                    adapter.addAll(FileUtils.getAudioLibrary(context));
                    break;

                case "image":
                    adapter.addAll(FileUtils.getImageLibrary(context));
                    break;

                case "video":
                    adapter.addAll(FileUtils.getVideoLibrary(context));
                    break;
            }

            return;
        }

        setPath(getInternalStorage());
    }

    //----------------------------------------------------------------------------------------------

    private void initAppBarLayout() {

        toolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar_layout);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.ic_more));

        setSupportActionBar(toolbar);
    }

    private void initCoordinatorLayout() {

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
    }

    private void initDrawerLayout() {

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (drawerLayout == null) return;

        if (name != null || type != null) {

            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    private void initFloatingActionButton() {

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floating_action_button);

        if (fab == null) return;

        fab.setOnClickListener(v -> actionCreate());

        if (name != null || type != null) {

            ViewGroup.LayoutParams layoutParams = fab.getLayoutParams();

            ((CoordinatorLayout.LayoutParams) layoutParams).setAnchorId(View.NO_ID);

            fab.setLayoutParams(layoutParams);

            fab.hide();
        }
    }

    private void initNavigationView() {

        navigationView = (NavigationView) findViewById(R.id.navigation_view);

        if (navigationView == null) return;

        MenuItem menuItem = navigationView.getMenu().findItem(R.id.navigation_external);

        menuItem.setVisible(getExternalStorage() != null);

        navigationView.setNavigationItemSelectedListener(item ->
        {
            switch (item.getItemId()) {
                case R.id.navigation_audio:
                    setType("audio");
                    return true;

                case R.id.navigation_image:
                    setType("image");
                    return true;

                case R.id.navigation_video:
                    setType("video");
                    return true;

                case R.id.navigation_feedback:
                    gotoFeedback();
                    return true;

                case R.id.navigation_settings:
                    gotoSettings();
                    return true;
            }

            drawerLayout.closeDrawers();

            switch (item.getItemId()) {

                case R.id.navigation_directory_0:
                    setPath(getPublicDirectory("DCIM"));
                    return true;

                case R.id.navigation_directory_1:
                    setPath(getPublicDirectory("Download"));
                    return true;

                case R.id.navigation_directory_2:
                    setPath(getPublicDirectory("Movies"));
                    return true;

                case R.id.navigation_directory_3:
                    setPath(getPublicDirectory("Music"));
                    return true;

                case R.id.navigation_directory_4:
                    setPath(getPublicDirectory("Pictures"));
                    return true;

                default:
                    return true;
            }
        });

        TextView textView = (TextView) navigationView.getHeaderView(0).findViewById(R.id.header);

        textView.setText(getStorageUsage(this));

        textView.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)));
    }

    private void initRecyclerView() {

        adapter = new Adapter(this);

        adapter.setOnItemClickListener(new OnItemClickListener(this));

        adapter.setOnItemSelectedListener(() -> {

            invalidateOptionsMenu();

            invalidateTitle();

            invalidateToolbar();
        });

        if (type != null) {

            switch (type) {

                case "audio":
                    adapter.setItemLayout(R.layout.list_item_1);
                    adapter.setSpanCount(getResources().getInteger(R.integer.span_count1));
                    break;

                case "image":
                    adapter.setItemLayout(R.layout.list_item_2);
                    adapter.setSpanCount(getResources().getInteger(R.integer.span_count2));
                    break;

                case "video":
                    adapter.setItemLayout(R.layout.list_item_3);
                    adapter.setSpanCount(getResources().getInteger(R.integer.span_count3));
                    break;
            }
        }
        else {

            adapter.setItemLayout(R.layout.list_item_0);

            adapter.setSpanCount(getResources().getInteger(R.integer.span_count0));
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        if (recyclerView != null) recyclerView.setAdapter(adapter);
    }

    //----------------------------------------------------------------------------------------------

    private void invalidateTitle() {

        if (adapter.anySelected()) {

            int selectedItemCount = adapter.getSelectedItemCount();

            toolbarLayout.setTitle(String.format("%s selected", selectedItemCount));
        }
        else if (name != null) {

            toolbarLayout.setTitle(String.format("Search for %s", name));
        }
        else if (type != null) {

            switch (type) {

                case "image":
                    toolbarLayout.setTitle("Images");
                    break;

                case "audio":
                    toolbarLayout.setTitle("Music");
                    break;

                case "video":
                    toolbarLayout.setTitle("Videos");
                    break;
            }
        }
        else if (currentDirectory != null && !currentDirectory.equals(getInternalStorage())) {

            toolbarLayout.setTitle(getName(currentDirectory));
        }
        else {

            toolbarLayout.setTitle(getResources().getString(R.string.app_name));
        }
    }

    private void invalidateToolbar() {

        if (adapter.anySelected()) {

            toolbar.setNavigationIcon(R.drawable.ic_clear);

            toolbar.setNavigationOnClickListener(v -> adapter.clearSelection());
        }
        else if (name == null && type == null) {

            toolbar.setNavigationIcon(R.drawable.ic_menu);

            toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(navigationView));
        }
        else {

            toolbar.setNavigationIcon(R.drawable.ic_back);

            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    //----------------------------------------------------------------------------------------------

    private void actionCreate() {

        InputDialog inputDialog = new InputDialog(this, "Create", "Create directory") {

            @Override
            public void onActionClick(String text) {

                try {
                    File directory = FileUtils.createDirectory(currentDirectory, text);

                    adapter.clearSelection();

                    adapter.add(directory);
                }
                catch (Exception e) {

                    showMessage(e);
                }
            }
        };

        inputDialog.show();
    }

    private void actionDelete() {

        actionDelete(adapter.getSelectedItems());

        adapter.clearSelection();
    }

    private void actionDelete(final List<File> files) {

        final File sourceDirectory = currentDirectory;

        adapter.removeAll(files);

        String message = String.format("%s files deleted", files.size());

        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG)
                .setAction("Undo", v -> {

                    if (currentDirectory == null || currentDirectory.equals(sourceDirectory)) {

                        adapter.addAll(files);
                    }
                })
                .addCallback(new Snackbar.Callback() {

                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {

                        if (event != DISMISS_EVENT_ACTION) {

                            try {

                                for (File file : files) FileUtils.deleteFile(file);
                            }
                            catch (Exception e) {

                                showMessage(e);
                            }
                        }

                        super.onDismissed(snackbar, event);
                    }
                })
                .show();
    }

    private void actionRename() {

        final List<File> selectedItems = adapter.getSelectedItems();

        InputDialog inputDialog = new InputDialog(this, "Rename", "Rename") {

            @Override
            public void onActionClick(String text) {

                adapter.clearSelection();

                try {

                    if (selectedItems.size() == 1) {

                        File file = selectedItems.get(0);

                        int index = adapter.indexOf(file);

                        adapter.updateItemAt(index, FileUtils.renameFile(file, text));
                    }
                    else {

                        int size = String.valueOf(selectedItems.size()).length();

                        String format = " (%0" + size + "d)";

                        for (int i = 0; i < selectedItems.size(); i++) {

                            File file = selectedItems.get(i);

                            int index = adapter.indexOf(file);

                            File newFile = FileUtils.renameFile(file, text + String.format(format, i + 1));

                            adapter.updateItemAt(index, newFile);
                        }
                    }
                }
                catch (Exception e) {

                    showMessage(e);
                }
            }
        };

        if (selectedItems.size() == 1) {

            inputDialog.setDefault(removeExtension(selectedItems.get(0).getName()));
        }

        inputDialog.show();
    }

    private void actionSearch() {

        InputDialog inputDialog = new InputDialog(this, "Search", "Search") {

            @Override
            public void onActionClick(String text) {

                setName(text);
            }
        };

        inputDialog.show();
    }

    private void actionCopy() {

        List<File> selectedItems = adapter.getSelectedItems();

        adapter.clearSelection();

        transferFiles(selectedItems, false);
    }

    private void actionMove() {

        List<File> selectedItems = adapter.getSelectedItems();

        adapter.clearSelection();

        transferFiles(selectedItems, true);
    }

    private void actionSend() {

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        intent.setType("*/*");

        ArrayList<Uri> uris = new ArrayList<>();

        for (File file : adapter.getSelectedItems()) {

            if (file.isFile()) uris.add(Uri.fromFile(file));
        }

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        startActivity(intent);
    }

    private void actionSort() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        int checkedItem = PreferenceUtils.getInteger(this, "pref_sort", 0);

        String sorting[] = {"Name", "Last modified", "Size (high to low)"};

        final Context context = this;

        builder.setSingleChoiceItems(sorting, checkedItem, (dialog, which) -> {

            adapter.update(which);

            PreferenceUtils.putInt(context, "pref_sort", which);

            dialog.dismiss();
        });

        builder.setTitle("Sort by");

        builder.show();
    }

    //----------------------------------------------------------------------------------------------

    private void transferFiles(final List<File> files, final Boolean delete) {

        String paste = delete ? "moved" : "copied";

        String message = String.format(Locale.getDefault(), "%d items waiting to be %s", files.size(), paste);

        View.OnClickListener onClickListener = v -> {

            try {

                for (File file : files) {

                    adapter.addAll(FileUtils.copyFile(file, currentDirectory));

                    if (delete) FileUtils.deleteFile(file);
                }
            }
            catch (Exception e) {

                showMessage(e);
            }
        };

        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_INDEFINITE)
                .setAction("Paste", onClickListener)
                .show();
    }

    private void showMessage(Exception e) {

        showMessage(e.getMessage());
    }

    private void showMessage(String message) {

        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_SHORT).show();
    }

    //----------------------------------------------------------------------------------------------

    private void gotoFeedback() {

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();

        builder.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary0));

        builder.build().launchUrl(this, Uri.parse("https://github.com/calintat/Explorer/issues"));
    }

    private void gotoSettings() {

        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void gotoApplicationSettings() {

        Intent intent = new Intent();

        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);

        intent.setData(Uri.fromParts("package", "com.calintat.explorer", null));

        startActivity(intent);
    }

    private void setPath(File directory) {

        if (!directory.exists()) {

            Toast.makeText(this, "Directory doesn't exist", Toast.LENGTH_SHORT).show();

            return;
        }

        currentDirectory = directory;

        adapter.clear();

        adapter.clearSelection();

        adapter.addAll(FileUtils.getChildren(directory));

        invalidateTitle();
    }

    private void setName(String name) {

        Intent intent = new Intent(this, MainActivity.class);

        intent.putExtra(EXTRA_NAME, name);

        startActivity(intent);
    }

    private void setType(String type) {

        Intent intent = new Intent(this, MainActivity.class);

        intent.putExtra(EXTRA_TYPE, type);

        if (Build.VERSION.SDK_INT >= 21) {

            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }

        startActivity(intent);
    }
    //----------------------------------------------------------------------------------------------

    private void setDriveAccount(){
        Log.e("Smart storge","first run");
        CharSequence drivers[]=new CharSequence[]{"Google Drive","DropBox"};
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Pleas a choose a drive....")
                .setItems(drivers, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which==0){
                            Log.i(GOOGLE_DRIVE_TAG,"Connecting to Google Drive............");
                            googleDriveConnect();
                        }
                        else if(which==1){
                            Log.i(DROP_BOX_TAG,"Connecting to DropBox............");
                            dropBoxConnect();
                        }
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void dropBoxConnect() {

    }
   // Context context=getApplicationContext();
    private void googleDriveConnect(){
//        GoogleDriveActivity googleDriveActivity=GoogleDriveActivity.getInstance();
//        googleDriveActivity.connect(context);
                    mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        mGoogleApiClient.connect();
        }



    @Override
    public void onConnected(Bundle bundle) {
        Query query =
                new Query.Builder().addFilter(Filters.and(Filters.eq(SearchableField.TITLE, "SmartStorage"), Filters.eq(SearchableField.TRASHED, false)))
                        .build();
        Drive.DriveApi.query(mGoogleApiClient, query).setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
            @Override public void onResult(DriveApi.MetadataBufferResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.e(GOOGLE_DRIVE_TAG, "Cannot create folder in the root.");
                } else {
                    boolean isFound = false;
                    for (Metadata m : result.getMetadataBuffer()) {
                        if (m.getTitle().equals("SmartStorage")) {
                            Log.e(GOOGLE_DRIVE_TAG, "Folder exists");
                            isFound = true;
                            driveId = m.getDriveId();
                            //create_file_in_folder(driveId);
                            break;
                        }
                    }
                    if (!isFound) {
                        Log.i(GOOGLE_DRIVE_TAG, "Folder not found; creating it.");
                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle("SmartStorage").build();
                        Drive.DriveApi.getRootFolder(mGoogleApiClient)
                                .createFolder(mGoogleApiClient, changeSet)
                                .setResultCallback(new ResultCallback<DriveFolder.DriveFolderResult>() {
                                    @Override public void onResult(DriveFolder.DriveFolderResult result) {
                                        if (!result.getStatus().isSuccess()) {
                                            Log.e(GOOGLE_DRIVE_TAG, "U AR A MORON! Error while trying to create the folder");
                                        } else {
                                            Log.i(GOOGLE_DRIVE_TAG, "Created a folder");
                                            driveId = result.getDriveFolder().getDriveId();
//                                            create_file_in_folder(driveId);
                                        }
                                    }
                                });
                    }
                }
            }
        });
        Log.d(GOOGLE_DRIVE_TAG,"Connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(GOOGLE_DRIVE_TAG,"Suspended");
    }

    @Override
    public void onConnectionFailed(@Nullable ConnectionResult connectionResult) {
        Log.d(GOOGLE_DRIVE_TAG,"Connection failed");
        // Called whenever the API client fails to connect.
        Log.i(GOOGLE_DRIVE_TAG, "GoogleApiClient connection failed:" + connectionResult.toString());

        if (!connectionResult.hasResolution()) {

            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0).show();
            return;
        }

        /**
         *  The failure has a resolution. Resolve it.
         *  Called typically when the app is not yet authorized, and an  authorization
         *  dialog is displayed to the user.
         */

        try {

           connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);

        } catch (IntentSender.SendIntentException e) {

            Log.e(GOOGLE_DRIVE_TAG, "Exception while starting resolution activity&", e);
        }
    }

    @Override
    public void doStuff() {
        for(int i=0;i<10;i++){
            Log.i(GOOGLE_DRIVE_TAG,String.valueOf(i));
        }
    }


    //----------------------------------------------------------------------------------------------

    private final class OnItemClickListener implements com.smartstorage.app.recycler.OnItemClickListener {

        private final Context context;

        private OnItemClickListener(Context context) {

            this.context = context;
        }

        @Override
        public void onItemClick(int position) {

            final File file = adapter.get(position);

            if (adapter.anySelected()) {

                adapter.toggle(position);

                return;
            }

            if (file.isDirectory()) {

                if (file.canRead()) {

                    setPath(file);
                }
                else {

                    showMessage("Cannot open directory");
                }
            }
            else {

                if (Intent.ACTION_GET_CONTENT.equals(getIntent().getAction())) {

                    Intent intent = new Intent();

                    intent.setDataAndType(Uri.fromFile(file), getMimeType(file));

                    setResult(Activity.RESULT_OK, intent);

                    finish();
                }
                else if (FileType.getFileType(file) == FileType.ZIP) {

                    final ProgressDialog dialog = ProgressDialog.show(context, "", "Unzipping", true);

                    Thread thread = new Thread(() -> {

                        try {

                            setPath(unzip(file));

                            runOnUiThread(dialog::dismiss);
                        }
                        catch (Exception e) {

                            showMessage(e);
                        }
                    });

                    thread.run();
                }
                else {

                    try {

                        Intent intent = new Intent(Intent.ACTION_VIEW);

                        intent.setDataAndType(Uri.fromFile(file), getMimeType(file));

                        startActivity(intent);
                    }
                    catch (Exception e) {

                        showMessage(String.format("Cannot open %s", getName(file)));
                    }
                }
            }
        }

        @Override
        public boolean onItemLongClick(int position) {

            adapter.toggle(position);

            return true;
        }
    }

    /** Callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // cast the IBinder and get MyService instance
            MyService.LocalBinder binder = (MyService.LocalBinder) service;
            Log.i(GOOGLE_DRIVE_TAG,"Service connected..................");
            myService = binder.getService();
            bound = true;
            myService.setCallbacks(MainActivity.this); // register

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    //TODO: dummy method to create a list of files
    public ArrayList<String> getFiles(){
        ArrayList<String> fileList=new ArrayList<>();
        fileList.add("/storage/emulated/0/Samsung/Music/Over the Horizon.mp3");
        fileList.add("/storage/emulated/0/DCIM/Camera/20170531_130417.jpg");
        fileList.add("/storage/emulated/0/DCIM/Screenshots/Screenshot_2017-08-08-18-29-01.png");
        fileList.add("/storage/emulated/0/Download/UoM-Virtual-Server-request-form-Final-Year-Projects.doc");
        fileList.add("/storage/emulated/0/Download/SL-Netacad-Hackathon-2017.pdf");

        return fileList;
    }
    ArrayList<String> fileList=getFiles();

    public void onBtnClick(View v){
//        for(int i=0;i<50000;i++){
//            Log.e(GOOGLE_DRIVE_TAG,String.valueOf(i));
//        }
        for(int i=0;i<fileList.size();i++){
            copyFileToGoogleDrive(fileList.get(i));
        }

//        fileOperation=true;
//        Drive.DriveApi.newDriveContents(mGoogleApiClient)
//                .setResultCallback(driveContentsCallback);

    }



//    final ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback =
//            new ResultCallback<DriveApi.DriveContentsResult>() {
//                @Override
//                public void onResult(DriveApi.DriveContentsResult result) {
//
//                    if (result.getStatus().isSuccess()) {
//
//                        if (fileOperation == true) {
//
//
//                            for(int i=0;i<fileList.size();i++){
//                                String fileUrl=fileList.get(i);
//                                copyFileToGoogleDrive(result,fileUrl);
//                            }
//
//
//                        } else {
////                            DownloadFile();
//
//                        }
//                    }
//
//                }
//    };
//





    public void copyFileToGoogleDrive(String fileUrl){
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.DriveContentsResult result) {
                        DriveContents driveContents = result.getDriveContents();

                        // Perform I/O off the UI thread.
                        new Thread() {
                            @Override
                            public void run() {
                                // write content to DriveContents
                                OutputStream outputStream = driveContents.getOutputStream();
                                // String fileUrl=fileList.get(i);
                                File file=new File(fileUrl);
                                try {
                                    FileInputStream fileInputStream=new FileInputStream(file);
                                    if (fileInputStream != null) {
                                        byte[] data = new byte[1024];
                                        while (fileInputStream.read(data) != -1) {
                                            outputStream.write(data);
                                        }
                                        fileInputStream.close();
                                    }
                                    outputStream.flush();
                                    //  outputStream.close();
                                } catch (IOException e) {
                                    Log.e(GOOGLE_DRIVE_TAG, e.getMessage());
                                }

                                String extension= fileUrl.substring(fileUrl.indexOf(".")+1);
                                String fileType=MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                                Log.e(GOOGLE_DRIVE_TAG,fileType);
                                String[] arr= fileUrl.split("/");
                                String fileName=arr[arr.length-1].substring(0,arr[arr.length-1].indexOf("."));

                                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                        .setTitle(fileName)
                                        .setMimeType(fileType)
                                        .setStarred(true).build();

                                // create a file in root folder
                                DriveFolder folder = driveId.asDriveFolder();
                                folder.createFile(mGoogleApiClient, changeSet, driveContents).setResultCallback(fileCallback);


                            }
                        }.start();

                    }
                });


    }

    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (result.getStatus().isSuccess()) {
                        Log.e("Android exxx:",result.getDriveFile().getDriveId().toString());

                        Toast.makeText(getApplicationContext(), "file created:"+";"+
                                result.getDriveFile().getDriveId(), Toast.LENGTH_LONG).show();

                    }

                    return;

                }
            };


}