package com.zwk.tool.exceller.service;

import com.zwk.tool.exceller.annotation.ExcelColumnName;
import com.zwk.tool.exceller.dto.FieldMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UnmarshallerServiceImpl implements UnmarshallerService {

    private final DataFormatter dataFormatter = new DataFormatter();
    private final SimpleDateFormat defaultDateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public <T> List<T> fromExcel (InputStream inputStream, Class<T> type) throws Exception {
        List<T> objects = new ArrayList<>();
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        XSSFSheet xssfSheet = workbook.getSheetAt(0);

        List<FieldMapper> fieldMappers = createFieldMapper(type.getDeclaredFields());

        findColumnNumberOfFields(fieldMappers, xssfSheet.getRow(0));

        for (int i = 1; i < xssfSheet.getPhysicalNumberOfRows(); i++) {
            try {
                T newInstance = type.getDeclaredConstructor().newInstance();
                System.out.println("++++++++++++++++");
                for (FieldMapper fieldMapper : fieldMappers) {
                    Object object;
                    Cell cell = xssfSheet.getRow(i).getCell(fieldMapper.getIndex());
                    System.out.println(cell.getCellTypeEnum());
                    String cellValue = dataFormatter.formatCellValue(cell);


                    Field field = fieldMapper.getField();
                    field.setAccessible(true);
                    System.out.println(field.getType().getName());
                    //TODO: more types
                    if("java.lang.String".equalsIgnoreCase(field.getType().getName()))
                        object = cellValue;
                    else if("java.util.Date".equalsIgnoreCase(field.getType().getName())) {
                        if (cell.getCellTypeEnum() == CellType.NUMERIC)
                            object = cell.getDateCellValue();
                        else {
                            try {
                                object = defaultDateFormat.parse(cellValue);
                            } catch (ParseException e) {
                                //TODO: new exception
                                throw new Exception("Incorrect date format");

                            }
                        }

                    } else if(field.getType() == Character.TYPE)
                        object = cellValue.charAt(0);
                    else if (field.getType() == Short.TYPE)
                        object = Short.parseShort(cellValue);
                    else if (field.getType() == Integer.TYPE)
                        object = Integer.parseInt(cellValue);
                    else
                        throw new Exception("Unsupported type: " + field.getType().getName());

                    field.set(newInstance, object);

                }
                objects.add(newInstance);
            } catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException e) {
                System.out.println(e);
            }


        }
        return objects;
    }

    /**
     *
     * @param fields
     * @return
     */
    private List<FieldMapper> createFieldMapper (Field [] fields) {
        List<FieldMapper> fieldMappers = new ArrayList<>();
        for (Field field : fields) {
            String fieldName = field.getAnnotation(ExcelColumnName.class) != null
                    ? field.getAnnotation(ExcelColumnName.class).value() : field.getName();
            FieldMapper fieldMapper = new FieldMapper(fieldName, field);
            fieldMappers.add(fieldMapper);
        }

        return fieldMappers;
    }

    /**
     *
     * @param fieldMappers
     * @param xssfRow
     * @throws Exception
     */
    private void findColumnNumberOfFields (List<FieldMapper> fieldMappers, XSSFRow xssfRow) throws Exception{
        int counter = fieldMappers.size();
        System.out.println("count: " + counter);
        if(xssfRow.getPhysicalNumberOfCells() < counter)
            throw new Exception("Expected fields number is greater than what's in the table");

        Iterator<Cell> cellIterator = xssfRow.cellIterator();
        while (cellIterator.hasNext() && counter > 0) {
            Cell cell = cellIterator.next();
            for (FieldMapper fieldMapper : fieldMappers) {
                if (fieldMapper.getFieldName().equalsIgnoreCase(cell.getStringCellValue())) {
                    fieldMapper.setIndex(cell.getColumnIndex());
                    counter--;
                }

            }
        }
        //TODO: check if counter is greater than 0
        if(counter > 0) {
            throw new Exception("value not found");
        }
    }
}
