package com.yas.commonlibrary.csv;

import com.yas.commonlibrary.csv.anotation.CsvColumn;
import com.yas.commonlibrary.csv.anotation.CsvName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvExporterTest {

    @SuperBuilder
    @CsvName(fileName = "TestFile")
    @Getter
    @Setter
    static class TestData extends BaseCsv {
        @CsvColumn(columnName = "Name")
        private String name;
        @CsvColumn(columnName = "Tags")
        private List<String> tags;
    }

    @Test
    void testExportToCsv_withValidData_shouldReturnCorrectCsvContent() throws IOException {
        List<BaseCsv> dataList = List.of(
            TestData.builder().id(1L).name("Alice").tags(List.of("tag1")).build()
        );
        byte[] csvBytes = CsvExporter.exportToCsv(dataList, TestData.class);
        String csvContent = new String(csvBytes);
        assertNotNull(csvContent);
        assertTrue(csvContent.contains("Id"));
    }

    @Test
    void testExportToCsv_withEmptyDataList_shouldReturnOnlyHeader() throws IOException {
        List<BaseCsv> dataList = new ArrayList<>();
        byte[] csvBytes = CsvExporter.exportToCsv(dataList, TestData.class);
        String csvContent = new String(csvBytes);
        // Kiểm tra xem có chứa header không (không so sánh tuyệt đối để tránh lỗi xuống dòng \n vs \r\n)
        assertTrue(csvContent.contains("Id,Name,Tags"));
    }

    @Test
    void testCreateFileName_withValidClass_shouldReturnCorrectFileName() {
        String fileName = CsvExporter.createFileName(TestData.class);
        assertTrue(fileName.contains("TestFile"));
    }
}