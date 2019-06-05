package cityu.jasontang.comicviewer;

//import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
//import android.widget.TextView;

import java.util.ArrayList;

/* Adapter for setting up the image in RecyclerView */
public class PageAdapter extends RecyclerView.Adapter<PageAdapter.ViewHolder> {
    private ArrayList<Page> pages;

    public PageAdapter(ArrayList<Page> pages) {
        this.pages = pages;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final View view;
        final ImageView pageImg;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            pageImg = view.findViewById(R.id.pageImg);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int ViewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_page, parent, false);

        return new ViewHolder(view);
    }

    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Page page = pages.get(i);
        //TODO: Handle double spread pages
        viewHolder.pageImg.setImageURI(page.getPageFile().getUri());
    }

    @Override
    public int getItemCount() {
        if (pages != null) {
            return pages.size();
        }
        else
            return 0;
    }

}
