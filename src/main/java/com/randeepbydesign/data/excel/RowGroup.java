package com.randeepbydesign.data.excel;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public class RowGroup {
    private final Optional<Object[]> headerRow;

    private final List<Object[]> groupedRows;

    public Optional<Object[]> getHeaderRow() {
        return headerRow;
    }

    public List<Object[]> getGroupedRows() {
        return groupedRows;
    }

    public RowGroup(List<Object[]> groupedRows) {
        this(null, groupedRows);
    }

    public RowGroup(Object[] headerRow, List<Object[]> groupedRows) {
        this.headerRow = ofNullable(headerRow);
        this.groupedRows = groupedRows;
    }
}
