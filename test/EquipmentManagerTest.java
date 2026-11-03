/* =============================================================================================
EQUIPMENT MANAGER TEST:
    - JUnit Tests for:
        - file can be opened
        - records can be added
        - a record can be removed
        - any record field can be updated
        - maintenance priority calculated
    - Each test starts with 5 basic records
    - JUnit requires void methods
============================================================================================= */

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class EquipmentManagerTest {

    // Private
    // =========================================================================================
    private EquipmentManager manager;

    // Fixed "today" value for simplicity (17th)
    private final LocalDate testDate = LocalDate.of(2026, 10, 17);

    // JUnit temp dir records
    @TempDir
    Path tempDir;

    // Shortcut for building a test record (dates written as month, day, year)
    private EquipmentRecord makeRecord(
            int id, String name, String type,
            int lastM, int lastD, int lastY,
            int nextM, int nextD, int nextY,
            String status, int criticality) {
        return new EquipmentRecord(
                id, name, type, LocalDate.of(lastY, lastM, lastD),
                LocalDate.of(nextY, nextM, nextD), status, criticality);
    }

    // Setup for every test
    @BeforeEach
    public void setUp() {
        manager = new EquipmentManager();
        manager.addRecord(makeRecord(1, "Portable Generator", "Power", 10, 1, 2026,
                10, 31, 2026, "Operational", 2));
        manager.addRecord(makeRecord(2, "Digital Multimeter", "Electric", 10, 10, 2026,
                10, 24, 2026, "Due Soon", 3));
        manager.addRecord(makeRecord(3, "Air Compressor", "Facility", 9, 17, 2026,
                12, 16, 2026, "Operational", 1));
        manager.addRecord(makeRecord(4, "Hydraulic Lift Table", "Material", 9, 22, 2026,
                10, 22, 2026, "Service Required", 3));
        manager.addRecord(makeRecord(5, "Emergency Power Supply", "Power", 9, 3, 2026,
                10, 10, 2026, "Out of Service", 3));
    }

    // Open file
    // =========================================================================================

    // valid opens w/ valid lines
    @Test
    public void testFileCanBeOpened() throws IOException {
        Path file = tempDir.resolve("test_equipment.txt");
        Files.writeString(file,
                "1,Portable Generator,Power,10/01/2026,10/31/2026,Operational,2\n"
                        + "2,Digital Multimeter,Electric,10/10/2026,10/24/2026,Due Soon,3\n");

        EquipmentManager fileManager = new EquipmentManager();
        int loaded = fileManager.loadFromFile(file.toString());  // + "x"

        assertEquals(2, loaded);
        assertEquals(2, fileManager.getRecordCount());
        assertEquals("Digital Multimeter", fileManager.findById(2).getEquipmentName());
    }

    // skip bad lines (w/ reason) but keep good lines
    @Test
    public void testFileWithBadLinesSkipsThem() throws IOException {
        Path file = tempDir.resolve("bad_equipment.txt");
        Files.writeString(file,
                "1,Portable Generator,Power,10/01/2026,10/31/2026,Operational,2\n"
                        + "abc,Bad ID,Power,10/01/2026,10/31/2026,Operational,2\n"
                        + "3,Fake Date,Power,02/30/2026,10/31/2026,Operational,2\n"); // 2/28/2026

        EquipmentManager fileManager = new EquipmentManager();
        int loaded = fileManager.loadFromFile(file.toString());

        assertEquals(1, loaded);
        assertEquals(2, fileManager.getLoadErrors().size());
    }

    // file doesn't exist throws exception (menu catches it)
    @Test
    public void testMissingFileThrowsException() {
        assertThrows(FileNotFoundException.class, () -> manager.loadFromFile("does_not_exist.txt"));
        // {real path}
    }

    // bank file throws exception
    @Test
    public void testBlankFileNameThrowsException() {
        assertThrows(FileNotFoundException.class, () -> manager.loadFromFile("   "));
        // {real path}
    }

    // Add
    // =========================================================================================

    // valid new record added and found w/ values
    @Test
    public void testAddRecord() {
        // id: 1
        EquipmentRecord newRecord = makeRecord(6, "Fuel Pump Cart", "Power", 10, 1,
                2026, 10, 31, 2026, "Operational", 2);

        assertTrue(manager.addRecord(newRecord));
        assertEquals(6, manager.getRecordCount());

        EquipmentRecord found = manager.findById(6);
        assertNotNull(found);
        assertEquals("Fuel Pump Cart", found.getEquipmentName());
        assertEquals(2, found.getCriticalityLevel());
    }

    // Existing ID (rejected)
    @Test
    public void testAddDuplicateIdFails() {
        // id: 7
        EquipmentRecord duplicate = makeRecord(1, "Another Generator", "Power", 10, 1,
                2026, 10, 31, 2026, "Operational", 2);

        assertFalse(manager.addRecord(duplicate));
        assertEquals(5, manager.getRecordCount());
        assertEquals("Portable Generator", manager.findById(1).getEquipmentName());
    }

    // record with bad data (rejected)
    @Test
    public void testAddInvalidRecordFails() {
        EquipmentRecord badCriticality = makeRecord(7, "Tool Cart", "Material", 10, 1,
                2026, 10, 31, 2026, "Operational", 5); // crit: 3
        EquipmentRecord blankName = makeRecord(8, " ", "Material", 10, 1,
                2026, 10, 31, 2026, "Operational", 2);

        assertFalse(manager.addRecord(badCriticality));
        assertFalse(manager.addRecord(blankName));
        assertEquals(5, manager.getRecordCount());
    }

    // Remove
    // =========================================================================================

    // Delete existing record
    @Test
    public void testDeleteRecord() {
        assertTrue(manager.deleteRecord(3)); // id: 99
        assertNull(manager.findById(3));
        assertEquals(4, manager.getRecordCount());
    }

    // Deleting nonexisting ID
    @Test
    public void testDeleteMissingRecordFails() {
        assertFalse(manager.deleteRecord(99)); // id: 3
        assertEquals(5, manager.getRecordCount());
    }

    // Update
    // =========================================================================================

    // Changing field (ie status) = only change that field
    @Test
    public void testUpdateStatus() {
        EquipmentRecord current = manager.findById(2);
        EquipmentRecord updated = new EquipmentRecord(2, current.getEquipmentName(),
                current.getEquipmentType(),
                current.getLastInspection(), current.getNextInspection(), "Service Required", // "Broken"
                current.getCriticalityLevel());

        assertTrue(manager.updateRecord(2, updated));
        EquipmentRecord found = manager.findById(2);

        assertEquals("Service Required", found.getStatus());
        assertEquals("Digital Multimeter", found.getEquipmentName());
        assertEquals(3, found.getCriticalityLevel());
        assertEquals(5, manager.getRecordCount());
    }

    // Every attribute can change
    @Test
    public void testUpdateEveryField() {
        EquipmentRecord updated = makeRecord(20, "Digital Multimeter Pro", "Electronics",
                10, 12, 2026, 11, 12, 2026, "Operational", 1); // nextM: 9

        assertTrue(manager.updateRecord(2, updated));
        assertNull(manager.findById(2));
        EquipmentRecord found = manager.findById(20);
        assertNotNull(found);

        assertEquals("Digital Multimeter Pro", found.getEquipmentName());
        assertEquals("Electronics", found.getEquipmentType());
        assertEquals(LocalDate.of(2026, 10, 12), found.getLastInspection());
        assertEquals(LocalDate.of(2026, 11, 12), found.getNextInspection());
        assertEquals("Operational", found.getStatus());
        assertEquals(1, found.getCriticalityLevel());
    }

    // Reject changing from existing ID
    @Test
    public void testUpdateToTakenIdFails() {
        EquipmentRecord current = manager.findById(2);
        EquipmentRecord updated = new EquipmentRecord(3, current.getEquipmentName(), // id: 30
                current.getEquipmentType(), current.getLastInspection(), current.getNextInspection(),
                current.getStatus(), current.getCriticalityLevel());

        assertFalse(manager.updateRecord(2, updated));
        assertEquals("Digital Multimeter", manager.findById(2).getEquipmentName());
        assertEquals("Air Compressor", manager.findById(3).getEquipmentName());
    }

    // Update an ID that does not exist fails
    @Test
    public void testUpdateMissingRecordFails() {
        EquipmentRecord updated = makeRecord(99, "Ghost", "None", 10, // id: 1
                1, 2026, 10, 31, 2026, "Operational", 1);
        assertFalse(manager.updateRecord(99, updated));
    }

    // Custom (maintenance priority)
    // Example: ID 4: 5 of 30 days remaining, crit: 3 (16.7) / 3 = 5.6 == High priority
    @Test
    public void testPriorityHigh() {
        double score = manager.calculatePriority(4, testDate);
        assertEquals(5.6, score, 0.05); // 5.0
        assertEquals("High", manager.getPriorityLevel(score));
    }

    // ID 1: 14 of 30 days remaining, crit: 2 (46.7) / 2 = 23.3 == Medium priority
    @Test
    public void testPriorityMedium() {
        double score = manager.calculatePriority(1, testDate); // id: 2
        assertEquals(23.3, score, 0.05);
        assertEquals("Medium", manager.getPriorityLevel(score));
    }

    // ID 3: 60 of 90 days remaining, crit: 1 (66.7) / 1 = 66.7 == Low priority
    @Test
    public void testPriorityLow() {
        double score = manager.calculatePriority(3, testDate);
        assertEquals(66.7, score, 0.05);
        assertEquals("Low", manager.getPriorityLevel(score)); // "Medium"
    }

    // ID: 5, Negative days remaining (already passed) == Overdue
    @Test
    public void testPriorityOverdue() {
        double score = manager.calculatePriority(5, testDate); // id: 3
        assertTrue(score < 0);
        assertEquals("Overdue", manager.getPriorityLevel(score));
    }

    // ID does not exist (returns NaN) to prevent crash
    @Test
    public void testPriorityMissingRecord() {
        double score = manager.calculatePriority(99, testDate); // id: 4
        assertTrue(Double.isNaN(score));
        assertEquals("Unknown", manager.getPriorityLevel(score));
    }
}


