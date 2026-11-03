/* =============================================================================================
EQUIPMENT RECORD:
    - Represents singular aerospace ground support equipment.
    - Each record stores 7 pieces of data:
        - ID
        - Name
        - Type
        - Last inspection date
        - Next inspection date
        - Status
        - Criticality level (1-3)
============================================================================================= */
package com.example.aetrackergui;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class EquipmentRecord {
    private int equipmentId;
    private String equipmentName;
    private String equipmentType;
    private LocalDate lastInspection;
    private LocalDate nextInspection;
    private String status;
    private int criticalityLevel;

    // Print dates as MM/dd/yyyy for toString()
    private final DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    // Create record w/ all 7 fields (assuming valid input)
    // =========================================================================================
    public EquipmentRecord(int equipmentId, String equipmentName, String equipmentType, LocalDate lastInspection, LocalDate nextInspection, String status, int criticalityLevel) {
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.equipmentType = equipmentType;
        this.lastInspection = lastInspection;
        this.nextInspection = nextInspection;
        this.status = status;
        this.criticalityLevel = criticalityLevel;
    }

    // *etters
    // =========================================================================================
    public int getEquipmentId() { return equipmentId; }
    public void setEquipmentId(int equipmentId) { this.equipmentId = equipmentId; }

    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }

    public String getEquipmentType() { return equipmentType; }
    public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }

    public LocalDate getLastInspection() { return lastInspection; }
    public void setLastInspection(LocalDate lastInspection) { this.lastInspection = lastInspection; }

    public LocalDate getNextInspection() { return nextInspection; }
    public void setNextInspection(LocalDate nextInspection) { this.nextInspection = nextInspection; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCriticalityLevel() { return criticalityLevel; }
    public void setCriticalityLevel(int criticalityLevel) { this.criticalityLevel = criticalityLevel; }


    // Return equipment record data in one formatted line
    // =========================================================================================
    @Override
    public String toString() {
        return "ID: " + equipmentId +
                " | " + equipmentName +
                " | " + equipmentType +
                " | Last: " + lastInspection.format(displayFormat) +
                " | Next: " + nextInspection.format(displayFormat) +
                " | " + status +
                " | Criticality: " + criticalityLevel;
    }
}
