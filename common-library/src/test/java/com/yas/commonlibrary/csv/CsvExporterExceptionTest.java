package com.yas.commonlibrary.csv;

import com.yas.commonlibrary.csv.anotation.CsvColumn;
import com.yas.commonlibrary.csv.anotation.CsvName;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvExporterExceptionTest {

    @SuperBuilder
    @CsvName(fileName = "ExcFile")
    @lombok.Getter
    @lombok.Setter
    static class ExcData extends BaseCsv {
        @CsvColumn(columnName = "Bad")
        private String bad;

        // getter intentionally throws to simulate InvocationTargetException
        public String getBad() {
            throw new RuntimeException("boom");
        }

        @CsvColumn(columnName = "Ok")
        private List<String> ok;
    }

    @Test
    void exportToCsv_getterThrows_shouldProduceEmptyColumn() throws IOException {
        ExcData d = ExcData.builder().id(2L).ok(List.of("x")).build();

        byte[] bytes = CsvExporter.exportToCsv(List.of(d), ExcData.class);
        String csv = new String(bytes);

        // header contains columns
        assertTrue(csv.contains("Id,Bad,Ok"));

        // When getter throws, exporter should write empty value for that column
        assertTrue(csv.contains("2,,[x]"));
    }
}
