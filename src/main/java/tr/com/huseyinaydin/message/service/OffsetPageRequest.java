package tr.com.huseyinaydin.message.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serializable;

public class OffsetPageRequest implements Pageable, Serializable {

    private final int offset;
    private final int limit;
    private final Sort sort;

    public OffsetPageRequest(int offset, int limit, Sort sort) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be less than zero!");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must not be less than one!");
        }
        this.offset = offset;
        this.limit = limit;
        this.sort = sort == null ? Sort.unsorted() : sort;
    }

    public OffsetPageRequest(int offset, int limit) {
        this(offset, limit, Sort.unsorted());
    }

    @Override
    public int getPageNumber() {
        return offset / limit;
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest((int) (getOffset() + getPageSize()), getPageSize(), getSort());
    }

    public OffsetPageRequest previous() {
        return hasPrevious() ? new OffsetPageRequest((int) (getOffset() - getPageSize()), getPageSize(), getSort()) : this;
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? previous() : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, getPageSize(), getSort());
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageRequest(pageNumber * getPageSize(), getPageSize(), getSort());
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
