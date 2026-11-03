/* =============================================================================================
EQUIPMENT MANAGER:
    - Store all equipment records in ArrayList (no db)
    - Handles all CRUD operations w/ file importing
    - Handles calculate priority (custom action)
============================================================================================= */
package com.example.aetrackergui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.io.FileNotFoundException;

public class EquipmentManager {

    // Private
    // =========================================================================================
    private final ArrayList<EquipmentRecord> records;
    private final InputValidator validator;

    // Messages for skipped lines (last file load)
    private final ArrayList<String> loadErrors;

    // Check fields (for add and update)
    private boolean isValidRecord(EquipmentRecord record) {
        return record != null &&
                validator.isValidId(record.getEquipmentId()) &&
                validator.isValidText(record.getEquipmentName()) &&
                validator.isValidText(record.getEquipmentType()) &&
                validator.isNextAfterLast(record.getLastInspection(), record.getNextInspection()) &&
                validator.isValidStatus(record.getStatus()) &&
                validator.isValidCriticality(record.getCriticalityLevel());
    }

    // Convert line into record
    // Return null (w/ message) if line is wrong
    private EquipmentRecord parseLine(String line, int lineNumber) {

        // FORMAT: id,name,type,lastInspection,nextInspection,status,criticality
        String[] parts = line.split(",", -1);
        if (parts.length != 7) {
            loadErrors.add("ERROR: Line " + lineNumber + ": expected 7 values, but found " + parts.length);
            return null;
        }

        int id;
        int criticality;
        try {
            id = Integer.parseInt(parts[0].trim());
            criticality = Integer.parseInt(parts[6].trim());
        } catch (NumberFormatException e) {
            loadErrors.add("ERROR: Line " + lineNumber + ": ID and criticality must be a whole number!");
            return null;
        }

        LocalDate last = validator.parseDate(parts[3]);
        LocalDate next = validator.parseDate(parts[4]);
        String status = validator.formatStatus(parts[5]);

        if (last == null || next == null) {
            loadErrors.add("ERROR: Line " + lineNumber + ": dates must be real dates in MM/dd/yyyy format!");
            return null;
        }

        if (status == null) {
            loadErrors.add("ERROR: Line " + lineNumber + ": status must be one of: " + validator.getStatusOptions());
            return null;
        }

        EquipmentRecord record = new EquipmentRecord(id, parts[1].trim(), parts[2].trim(), last, next, status,
                criticality);

        if (!isValidRecord(record)) {
            loadErrors.add("ERROR: Line " + lineNumber + ": invalid ID, blank name/type, next inspection (missing), or criticality not 1-3");
            return null;
        }

        return record;
    }

    // Public
    // =========================================================================================
    public EquipmentManager() {
        records = new ArrayList<>();
        validator = new InputValidator();
        loadErrors = new ArrayList<>();
    }

    // Find ID
    public EquipmentRecord findById(int id) {
        for (EquipmentRecord record : records) {
            if (record.getEquipmentId() == id) {
                return record;
            }
        }
        return null;
    }

    // Check ID
    public boolean idExists(int id) {
        return findById(id) != null;
    }

    // Create / add record (if valid)
    public boolean addRecord(EquipmentRecord record) {
        if (!isValidRecord(record) || idExists(record.getEquipmentId())) {
            return false;
        }
        records.add(record);
        return true;
    }

    // Create from file (comma separated lines)
    public int loadFromFile(String filePath) throws IOException {
        loadErrors.clear();
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new FileNotFoundException("File name can not be blank!");
        }

        // Check if !exists or a folder
        File file = new File(filePath.trim());
        if (!file.isFile()) {
            // where the program looked
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }

        int added = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // ignore blank lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Handled by parseLine
                EquipmentRecord record = parseLine(line, lineNumber);
                if (record == null) {
                    continue;
                }

                if (idExists(record.getEquipmentId())) {
                    loadErrors.add("ERROR: Line " + lineNumber + ": ID " + record.getEquipmentId() + " already exists!");
                    continue;
                }
                records.add(record);
                added++;
            }
        } catch (IOException e) {
            return -1;
        }
        return added;
    }

    // Skipped line message
    public ArrayList<String> getLoadErrors() {
        return new ArrayList<>(loadErrors);
    }

    // Copy for returns
    public ArrayList<EquipmentRecord> getAllRecords() {
        return new ArrayList<>(records);
    }

    // Count
    public int getRecordCount() {
        return records.size();
    }


    // Update
    // =========================================================================================
    public boolean updateRecord(int id, EquipmentRecord updatedRecord) {
        EquipmentRecord existing = findById(id);
        if (existing == null || !isValidRecord(updatedRecord)) {
            return false;
        }

        int newId = updatedRecord.getEquipmentId();
        if (newId != id && idExists(newId)) {
            return false;
        }

        records.set(records.indexOf(existing), updatedRecord);
        return true;
    }

    // Delete
    // =========================================================================================
    public boolean deleteRecord(int id) {
        return records.removeIf(record -> record.getEquipmentId() == id);
    }

    // Calc Priority
    // =========================================================================================
    public double calculatePriority(int id) {
        return calculatePriority(id, LocalDate.now());
    }

    // Get score

    public double calculatePriority(int id, LocalDate today) {
        EquipmentRecord record = findById(id);
        if (record == null) {
            return Double.NaN;
        }

        long daysBetween = ChronoUnit.DAYS.between(record.getLastInspection(), record.getNextInspection());
        long daysLeft = ChronoUnit.DAYS.between(today, record.getNextInspection());

        // Validate next after last again (backup)
        if (daysBetween <= 0) {
            return Double.NaN;
        }

        // Main calc
        double percentLeft = (double) daysLeft / daysBetween * 100;
        return percentLeft / record.getCriticalityLevel();
    }

    // Score to priority level
    public String getPriorityLevel(double score) {
        if (Double.isNaN(score)) {
            return "Unknown";
        } else if (score < 0) {
            return "Overdue";
        } else if (score < 10) {
            return "High";
        } else if (score < 25) {
            return "Medium";
        } else {
            return "Low";
        }

    }
}
