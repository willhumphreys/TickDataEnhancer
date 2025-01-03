package uk.co.threebugs;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class MissingHourAdderTest {

    private MissingHourAdder missingHourAdder;

    @BeforeEach
    void setUp() {
        missingHourAdder = new MissingHourAdder(); // Initialize the class
    }

    @Test
    void testAddMissingHours() throws IOException {
        // Create temporary files for input and output
        Path inputFile = Files.createTempFile("input_", ".csv");
        Path outputFile = Files.createTempFile("output_", ".csv");

        // Write sample input data (2 rows with 1 missing hour in between)
        Files.write(inputFile, List.of(
                "dateTime,name,open,high,low,close,volume,atr,weighting,weightingAtr,newHour,fixedLow,fixedHigh,mapTime",
                "2023-10-01T09:00,instrument1,100,120,90,110,1000,10,1.00,1.00,false,90,120,2023-10-01T09:00",
                "2023-10-01T11:00,instrument1,110,130,100,120,1200,12,1.10,1.20,true,100,130,2023-10-01T11:00"
        ));

        // Call the method to add missing hourly rows
        missingHourAdder.addMissingHours(inputFile, outputFile);

        // Read the output file
        List<String> outputData = Files.lines(outputFile).toList();

        // Assert that the output contains the expected rows
        assertEquals(4, outputData.size(), "Output should have a header and rows for 3 hours (including missing hour)");

        // Verify the header with appended `holiday` column
        assertEquals("dateTime,name,open,high,low,close,volume,atr,weighting,weightingAtr,newHour,fixedLow,fixedHigh,mapTime,holiday",
                outputData.get(0));

        // Verify the rows
        assertEquals("2023-10-01T09:00,instrument1,100,120,90,110,1000,10,1.00,1.00,false,90,120,2023-10-01T09:00,0",
                outputData.get(1), "Row for 09:00 should have holiday=0");
        assertEquals("2023-10-01T10:00,instrument1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,2023-10-01T10:00,1",
                outputData.get(2), "Generated row for 10:00 should have holiday=1 and fields set to -1");
        assertEquals("2023-10-01T11:00,instrument1,110,130,100,120,1200,12,1.10,1.20,true,100,130,2023-10-01T11:00,0",
                outputData.get(3), "Row for 11:00 should have holiday=0");

        // Clean up temporary files
        Files.deleteIfExists(inputFile);
        Files.deleteIfExists(outputFile);
    }

    @Test
    void testInputFileIsEmpty() {
        // Create temporary files for input and output
        Path inputFile = null;
        Path outputFile = null;

        try {
            inputFile = Files.createTempFile("empty_input_", ".csv");
            outputFile = Files.createTempFile("output_", ".csv");

            // Call the method to add missing hourly rows
            final Path finalInputFile = inputFile;
            final Path finalOutputFile = outputFile;
            Exception exception = assertThrows(IOException.class,
                    () -> missingHourAdder.addMissingHours(finalInputFile, finalOutputFile),
                    "Expected IOException when input file is empty");

            assertEquals("Input file is empty.", exception.getMessage(), "Exception message should match");

        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        } finally {
            // Clean up temporary files
            try {
                if (inputFile != null) Files.deleteIfExists(inputFile);
                if (outputFile != null) Files.deleteIfExists(outputFile);
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    void testMissingRequiredColumns() {
        // Create temporary files for input and output
        Path inputFile = null;
        Path outputFile = null;

        try {
            inputFile = Files.createTempFile("missing_columns_", ".csv");
            outputFile = Files.createTempFile("output_", ".csv");

            // Write input with missing required columns
            Files.write(inputFile, List.of(
                    "open,high,low,close", // Missing dateTime and name columns
                    "100,120,90,110"
            ));

            // Call the method to add missing hourly rows
            final Path finalInputFile = inputFile;
            final Path finalOutputFile = outputFile;
            Exception exception = assertThrows(IllegalStateException.class,
                    () -> missingHourAdder.addMissingHours(finalInputFile, finalOutputFile),
                    "Expected IllegalStateException when required columns are missing");

            assertEquals("Missing required columns: 'dateTime' or 'name'",
                    exception.getMessage(), "Exception message should match");

        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        } finally {
            // Clean up temporary files
            try {
                if (inputFile != null) Files.deleteIfExists(inputFile);
                if (outputFile != null) Files.deleteIfExists(outputFile);
            } catch (IOException ignored) {
            }
        }


    }


    @Test
    void testAddMultipleMissingHours() throws IOException {
        // Create temporary files for input and output
        Path inputFile = Files.createTempFile("input_", ".csv");
        Path outputFile = Files.createTempFile("output_", ".csv");

        // Write sample input data with multiple missing hours in between
        Files.write(inputFile, List.of(
                "dateTime,name,open,high,low,close,volume,atr,weighting,weightingAtr,newHour,fixedLow,fixedHigh,mapTime",
                "2023-10-01T08:00,instrument1,100,120,90,110,1000,10,1.00,1.00,false,90,120,2023-10-01T08:00",
                "2023-10-01T12:00,instrument1,110,130,100,120,1200,12,1.10,1.20,true,100,130,2023-10-01T12:00"
        ));

        // Call the method to add missing hourly rows
        missingHourAdder.addMissingHours(inputFile, outputFile);

        // Read the output file
        List<String> outputData = Files.lines(outputFile).toList();

        // Assert that the output contains the expected rows
        assertEquals(6, outputData.size(), "Output should have a header and rows for 5 hours (including 4 missing hours)");

        // Verify the header with appended `holiday` column
        assertEquals("dateTime,name,open,high,low,close,volume,atr,weighting,weightingAtr,newHour,fixedLow,fixedHigh,mapTime,holiday",
                outputData.get(0), "Header row should include the appended 'holiday' column");

        // Verify the rows
        assertEquals("2023-10-01T08:00,instrument1,100,120,90,110,1000,10,1.00,1.00,false,90,120,2023-10-01T08:00,0",
                outputData.get(1), "Row for 08:00 should have holiday=0");

        assertEquals("2023-10-01T09:00,instrument1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,2023-10-01T09:00,1",
                outputData.get(2), "Generated row for 09:00 should have holiday=1 and fields set to -1");
        assertEquals("2023-10-01T10:00,instrument1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,2023-10-01T10:00,1",
                outputData.get(3), "Generated row for 10:00 should have holiday=1 and fields set to -1");
        assertEquals("2023-10-01T11:00,instrument1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,2023-10-01T11:00,1",
                outputData.get(4), "Generated row for 11:00 should have holiday=1 and fields set to -1");

        assertEquals("2023-10-01T12:00,instrument1,110,130,100,120,1200,12,1.10,1.20,true,100,130,2023-10-01T12:00,0",
                outputData.get(5), "Row for 12:00 should have holiday=0");

        // Clean up temporary files
        Files.deleteIfExists(inputFile);
        Files.deleteIfExists(outputFile);
    }

    @Test
    void testAddTwoGaps() throws IOException {
        // Create temporary files for input and output
        Path inputFile = Files.createTempFile("input_", ".csv");
        Path outputFile = Files.createTempFile("output_", ".csv");

        // Write sample input data with two gaps
        Files.write(inputFile, List.of(
                "dateTime,name,open,high,low,close,volume,atr,weighting,weightingAtr,newHour,fixedLow,fixedHigh,mapTime",
                "2023-10-01T08:00,instrument1,100,120,90,110,1000,10,1.00,1.00,false,90,120,2023-10-01T08:00",
                "2023-10-01T10:00,instrument1,110,130,100,120,1200,12,1.10,1.20,true,100,130,2023-10-01T10:00",
                "2023-10-01T13:00,instrument1,120,140,110,130,1300,13,1.20,1.30,false,110,140,2023-10-01T13:00"
        ));

        // Call the method to add missing hourly rows
        missingHourAdder.addMissingHours(inputFile, outputFile);

        // Read the output file
        List<String> outputData = Files.lines(outputFile).toList();

        // Assert that the output contains the expected rows
        assertEquals(7, outputData.size(),
                "Output should have a header and rows for all hours between 08:00 and 13:00 (including gaps)");

        // Verify the header with appended `holiday` column
        assertEquals(
                "dateTime,name,open,high,low,close,volume,atr,weighting,weightingAtr,newHour,fixedLow,fixedHigh,mapTime,holiday",
                outputData.get(0), "Header row should include the appended 'holiday' column"
        );

        // Verify the output rows
        assertEquals(
                "2023-10-01T08:00,instrument1,100,120,90,110,1000,10,1.00,1.00,false,90,120,2023-10-01T08:00,0",
                outputData.get(1), "Row for 08:00 should have holiday=0"
        );
        assertEquals(
                "2023-10-01T09:00,instrument1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,2023-10-01T09:00,1",
                outputData.get(2), "Generated row for 09:00 should have holiday=1 and fields set to -1"
        );
        assertEquals(
                "2023-10-01T10:00,instrument1,110,130,100,120,1200,12,1.10,1.20,true,100,130,2023-10-01T10:00,0",
                outputData.get(3), "Row for 10:00 should have holiday=0"
        );
        assertEquals(
                "2023-10-01T11:00,instrument1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,2023-10-01T11:00,1",
                outputData.get(4), "Generated row for 11:00 should have holiday=1 and fields set to -1"
        );
        assertEquals(
                "2023-10-01T12:00,instrument1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,2023-10-01T12:00,1",
                outputData.get(5), "Generated row for 12:00 should have holiday=1 and fields set to -1"
        );
        assertEquals(
                "2023-10-01T13:00,instrument1,120,140,110,130,1300,13,1.20,1.30,false,110,140,2023-10-01T13:00,0",
                outputData.get(6), "Row for 13:00 should have holiday=0"
        );

        // Clean up temporary files
        Files.deleteIfExists(inputFile);
        Files.deleteIfExists(outputFile);
    }

}