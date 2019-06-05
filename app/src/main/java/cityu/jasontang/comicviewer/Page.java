package cityu.jasontang.comicviewer;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;

/* Page object */   //TODO: Create sub class for double spread pages
public class Page {
    private int pageNo;
    private DocumentFile pageFile;

    public Page(int pageNo, DocumentFile pageFile) {
        this.pageNo = pageNo;
        this.pageFile = pageFile;
    }

    DocumentFile getPageFile() {
        return pageFile;
    }

    @Override
    @NonNull
    public String toString() {
        return pageNo+","+pageFile.getUri();
    }


}
