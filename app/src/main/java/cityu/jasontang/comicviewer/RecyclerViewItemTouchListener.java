package cityu.jasontang.comicviewer;

import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/* For detecting gesture on RecyclerViewItem */
public abstract class RecyclerViewItemTouchListener implements RecyclerView.OnItemTouchListener {
    private GestureDetectorCompat gestureDetector;
    private RecyclerView recyclerView;

    public RecyclerViewItemTouchListener(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        gestureDetector = new GestureDetectorCompat(recyclerView.getContext(),
                new ItemTouchGestureListener());
    }

    public abstract void onItemClick(RecyclerView.ViewHolder viewHolder);

    public abstract void onItemLongClick(RecyclerView.ViewHolder viewHolder);

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent m) {
        gestureDetector.onTouchEvent(m);
        return false;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent m) {
        gestureDetector.onTouchEvent(m);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) { }

    private class ItemTouchGestureListener extends GestureDetector.SimpleOnGestureListener {
        // For single tap
        @Override
        public boolean onSingleTapUp(MotionEvent m) {
            View child = recyclerView.findChildViewUnder(m.getX(), m.getY());
            if (child != null) {
                RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(child);
                onItemClick(viewHolder);
            }
            return true;
        }

        //For long tap
        @Override
        public void onLongPress(MotionEvent m) {
            View child = recyclerView.findChildViewUnder(m.getX(), m.getY());
            if (child != null) {
                RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(child);
                onItemLongClick(viewHolder);
            }
        }
    }
}
