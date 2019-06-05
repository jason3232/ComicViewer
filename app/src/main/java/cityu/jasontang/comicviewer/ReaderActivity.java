package cityu.jasontang.comicviewer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class ReaderActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private DocumentFile pathToImage;
    private ArrayList<Page> pagesList;
    private int mode; //0 for foursplit, 1 for findPanel
    private boolean otsu_threshold;
    private int threshold_value;
    private boolean split_panel;
    private boolean manual_split_panel;
    private int split_panel_area;
    private int split_panel_rows;
    private int split_panel_cols;
    private boolean keep_panel;
    private boolean four_sort;
    private boolean four_sort_merge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        setDefault(sharedPreferences);

        /* Set up action bar */
        Toolbar thisToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(thisToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        /* Get folder from Uri */
        Intent intent = getIntent();
        Uri selectedPath = intent.getParcelableExtra("selectedPath");
        Log.d("selectedPath", selectedPath.getPath());
        pathToImage = DocumentFile.fromTreeUri(this,selectedPath);
        if(pathToImage!=null) {
            String foldername = pathToImage.getName();
            setTitle(foldername);
        }
        else{
            Toast.makeText(ReaderActivity.this, "Error getting folder path.", Toast.LENGTH_LONG).show();
            finish();
        }

        /* Initialize the pagesList */
        pagesList = initPages();

        /* Final checking for content of pagesList */   //Redundant?
        if( pagesList.size() == 0 ) {
            Toast.makeText(ReaderActivity.this, "The selected folder has no images.", Toast.LENGTH_LONG).show();
            finish();
        }
        else {
            //Init RecyclerView
            initView();
        }

        setImmersiveMode();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    /* Setup toolbar button */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_reader, menu);

        return true;
    }

    /* Toolbar button action */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_mode:
                if(mode == 0) {
                    mode = 1;
                    item.setIcon(R.drawable.ic_mode_panel);
                }
                else {
                    mode = 0;
                    item.setIcon(R.drawable.ic_mode_four);
                }
                return true;

            case R.id.action_settings:
                Intent intent = new Intent(ReaderActivity.this, ReaderSettingsActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* Set default value for parameters */      //Should use SharedPreference editor to save preferences maybe?
    public void setDefault(SharedPreferences sharedPreferences) {
        mode = 1;
        otsu_threshold = sharedPreferences.getBoolean("guide_image_threshold_otsu",true);
        threshold_value = Integer.parseInt(sharedPreferences.getString("image_threshold_value","127"));     //getString because value modified by onPreferenceChange
        split_panel = sharedPreferences.getBoolean("guide_panel_split",true);
        manual_split_panel = sharedPreferences.getBoolean("guide_panel_split_manual",false);
        split_panel_area = Integer.parseInt(sharedPreferences.getString("panel_split_area","40"));
        split_panel_rows = Integer.parseInt(sharedPreferences.getString("panel_split_rows","2"));
        split_panel_cols = Integer.parseInt(sharedPreferences.getString("panel_split_cols","2"));
        keep_panel = sharedPreferences.getBoolean("guide_panel_split_keep",false);
        four_sort = sharedPreferences.getBoolean("guide_sort_four",false);
        four_sort_merge = sharedPreferences.getBoolean("guide_sort_four_merge",true);
    }

    /* Monitor SharedPreference changes */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("guide_image_threshold_otsu")) {
            otsu_threshold = sharedPreferences.getBoolean("guide_image_threshold_otsu",true);
        }
        if (key.equals("image_threshold_value")) {
            threshold_value = Integer.parseInt(sharedPreferences.getString("image_threshold_value","127"));
        }
        if (key.equals("guide_panel_split")) {
            split_panel = sharedPreferences.getBoolean("guide_panel_split",true);
        }
        if (key.equals("guide_panel_split_manual")) {
            manual_split_panel = sharedPreferences.getBoolean("guide_panel_split_manual",false);
        }
        if (key.equals("guide_panel_split_area")) {
            split_panel_area = Integer.parseInt(sharedPreferences.getString("panel_split_rows","40"));
        }
        if (key.equals("panel_split_rows")) {
            split_panel_rows = Integer.parseInt(sharedPreferences.getString("panel_split_rows","2"));
        }
        if (key.equals("panel_split_cols")) {
            split_panel_cols = Integer.parseInt(sharedPreferences.getString("panel_split_cols","2"));
        }
        if (key.equals("guide_panel_split_keep")) {
            keep_panel = sharedPreferences.getBoolean("guide_panel_split_keep",true);
        }
        if (key.equals("guide_sort_four")) {
            four_sort = sharedPreferences.getBoolean("guide_sort_four",true);
        }
        if (key.equals("guide_sort_four_merge")) {
            four_sort_merge = sharedPreferences.getBoolean("guide_sort_four_merge", true);
        }
    }

    /* Set immersive mode */
    private void setImmersiveMode () {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        /* Return to immersive mode on visibility change */
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            getWindow().getDecorView().setSystemUiVisibility(
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
                        }}, 2000);
                }
            }
        });
    }

    /* Setup RecyclerView */
    private void initView() {
//        final ArrayList<Page> pagesList = initPages();
        /* Setup RecyclerView */
        RecyclerView pagesView = findViewById(R.id.pagesView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        pagesView.setLayoutManager(layoutManager);

        /* Setup ViewAdapter for RecyclerView */
        RecyclerView.Adapter pagesAdapter = new PageAdapter(pagesList);
        pagesView.setAdapter(pagesAdapter);

        /* Set ItemTouchListener for images */
        pagesView.addOnItemTouchListener(new RecyclerViewItemTouchListener(pagesView) {
            @Override
            public void onItemClick(RecyclerView.ViewHolder viewHolder) {
                //Item tapped, Nothing to do yet
            }

            @Override
            public void onItemLongClick(RecyclerView.ViewHolder viewHolder) {
                //Item long tapped, start guided reader
                int pos = viewHolder.getAdapterPosition();
                Page page = pagesList.get(pos);
//                Toast.makeText(ReaderActivity.this, "Long Clicked Picture #"+pos, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent (ReaderActivity.this, GuidedReadActivity.class);
                //Pass the page to the guided reader
                intent.putExtra("clickedImg", page.getPageFile().getUri());
                intent.putExtra("Mode", mode);
                //Save sharedPreference for these maybe?
                intent.putExtra("Otsu", otsu_threshold);
                if(!otsu_threshold)
                    intent.putExtra("thresholdValue", threshold_value);
                intent.putExtra("Split", split_panel);
                if(split_panel) {
                    intent.putExtra("Manual", manual_split_panel);
                    if(manual_split_panel) {
                        intent.putExtra("Area", split_panel_area);
                        intent.putExtra("Rows", split_panel_cols);
                        intent.putExtra("Cols", split_panel_rows);
                    }
                    intent.putExtra("keepPanel", keep_panel);
                }
                intent.putExtra("Four", four_sort);
                if(four_sort)
                    intent.putExtra("FourMerge", four_sort_merge);
                startActivity(intent);
            }
        });
    }

    /* Load the images to List */
    private ArrayList<Page> initPages() {
        /* ArrayList for page image files */
        ArrayList<Page> pages = new ArrayList<>();

        /* Get all file from folder to array*/
        DocumentFile[] folderFiles = pathToImage.listFiles();

        Log.d("Steps", "Sort");

        /* Sort the files by filename */
        if(folderFiles.length > 0) {
            //Regex pattern for filenames with number
            //Implement numeric sort instead of this maybe? Depends on user input
            Arrays.sort(folderFiles, new Comparator<DocumentFile>() {
                @Override
                public int compare(DocumentFile img1, DocumentFile img2) {
                    //Maybe implement numeric sort for this? Really depends on user input
                    if (img1.getName() != null && img2.getName() != null) {     //Both shouldn't been null, handling just in case
                        return img1.getName().compareTo(img2.getName());
                    }
                    else
                        return -1;  //null before other
                }

            });
        }
        else {
            Toast.makeText(ReaderActivity.this, "The selected folder is empty.", Toast.LENGTH_LONG).show();
            finish();
        }

        /* Get image files to List */
        Log.d("Steps","Add Pages");
        for (int i = 0; i < folderFiles.length; i++) {
            String filename = folderFiles[i].getName();
            if(filename != null) {
                //Filter by file extension
                if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")) {
                    //TODO: Handle double spread page here maybe
                    Page newPage = new Page(i, folderFiles[i]);
                    Log.d("Adding", newPage.toString());
                    pages.add(newPage);
                }
            }
        }


        return pages;
    }
}
