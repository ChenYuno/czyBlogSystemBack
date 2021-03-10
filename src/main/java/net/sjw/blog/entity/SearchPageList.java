package net.sjw.blog.entity;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class SearchPageList<T> implements Serializable {
    //做分页的数据封装类
    // 当前页码
    private int currentPage;

    //总数量
    private int totalCount;

    //每一页有多少数量
    private int pageSize;

    //总页数
    private int totalPage;

    //是否有前一页
    private boolean hasPrevious;

    //是否有后一页
    private boolean hasNext;

    //数据
    private List<T> records = new ArrayList<>();


    public SearchPageList(int page, int size, int totalCount) {
        this.totalCount = totalCount;
        this.currentPage = page;
        this.pageSize = size;
        this.totalPage = totalCount % size == 0 ?
                totalCount / size : totalCount / size + 1;
        this.hasNext = page < totalPage;
        this.hasPrevious = page > 1;
    }
    public SearchPageList(Page<T> all) {
        this.totalCount = (int)all.getTotal();
        this.currentPage = (int)all.getCurrent();
        this.pageSize = (int)all.getSize();
        this.totalPage = totalCount % this.pageSize == 0 ?
                totalCount / this.pageSize : totalCount / this.pageSize + 1;
        this.hasNext = all.hasNext();
        this.hasPrevious = all.hasPrevious();
        this.records = all.getRecords();
    }
}
