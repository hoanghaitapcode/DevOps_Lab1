package com.yas.commonlibrary.csv;

import com.yas.commonlibrary.csv.anotation.CsvColumn;
import com.yas.commonlibrary.csv.anotation.CsvName;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvExporterEdgeCaseTest {

    @SuperBuilder
    @CsvName(fileName = "EdgeFile")
    static class EdgeData extends BaseCsv {
        @CsvColumn(columnName = "NoGetter")
        private String noGetter;

        @CsvColumn(columnName = "Tags")
        @Getter
        private List<String> tags;
    }

    @Test
    void exportToCsv_missingGetter_shouldProduceEmptyColumn_and_formatList() throws IOException {
        EdgeData d = EdgeData.builder().id(1L).tags(List.of("a", "b")).build();

        byte[] bytes = CsvExporter.exportToCsv(List.of(d), EdgeData.class);
        String csv = new String(bytes);

        // header should include Id, NoGetter, Tags in that order
        assertTrue(csv.contains("Id,NoGetter,Tags"));

        // data row should contain id, empty value for missing getter, and list formatted as [a|b]
        assertTrue(csv.contains("1,,[a|b]"));
    }
}
