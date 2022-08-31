package com.randeepbydesign.data;

import com.randeepbydesign.data.excel.RowGroup;
import java.util.List;
import java.util.Optional;

/**
 * Takes a group of rows that are collected based on a key value. A row is defined as an Object array. In the event the
 * row group input is not valid an emptyOptional can be returned
 */
public interface RowGroupMapperFunction<T> {

    /**
     *
     * @param rows one or more Rows of a grid sheet. If the rows are not grouped the input will always be a singleton
     * list
     * @return Optional of the transformed row group or empty if the group could not be processed
     */
    Optional<T> apply(RowGroup rows);

}