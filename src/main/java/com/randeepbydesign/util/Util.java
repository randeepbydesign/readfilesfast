package com.randeepbydesign.util;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

@Slf4j
public class Util {


    public static Long getLong(Cell cell) {
        if(!cell.getCellType().equals(CellType.NUMERIC)) {
            log.warn(cell.getStringCellValue() + " is no numeric");
        }
        return (long)cell.getNumericCellValue();
    }

    public static Object[] convertRowToList(Row row) {
        Object[] ret = new Object[row.getLastCellNum() + 1];
        if(ret.length ==0) {
            log.trace("Row {} is empty", row.getRowNum());
            return null;
        }
        for(int i=0; i<ret.length-1; i++) {
            ret[i] = convertCellValue(row.getCell(i));
        }
        ret[ret.length-1] = row.getRowNum();
        return ret;
    }


    public static Object convertCellValue(Cell cell) {
        if(cell==null) return null;
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }
        switch(cellType) {
            case _NONE:
            case BLANK:
            case ERROR:
                return null;
            case NUMERIC:
            case FORMULA:
                if(DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                return cell.getNumericCellValue();
            case STRING:
                return cell.getStringCellValue().trim();
            case BOOLEAN:
                return cell.getBooleanCellValue();
        }
        return null;
    }

    public static String asString(Object object) {
        if(object instanceof Double) {
            return ((Double)object).toString();
        }
        return object.toString();
    }

    public static boolean hasData(Object apply) {
        if(Objects.isNull(apply)) return false;
        if(apply instanceof String && apply.toString().isEmpty()) return false;
        return true;
    }

}
