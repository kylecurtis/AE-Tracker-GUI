/* =============================================================================================
INPUT VALIDATOR:
    - Checks that each field is valid before any record is added / updated
    - Used for manual entry (menu) and loading from file
    - Methods return true/false or formatted values for printing
============================================================================================= */
package com.example.aetrackergui;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public class InputValidator {

    // Private
    // =========================================================================================
    private final String[] validStatuses = {"Operational", "Due Soon", "Service Required", "Out of Service"};
    private final int maxTextLength = 50;

    // STRICT requires "uuuu" (no fake dates / dates that don't exist)
    // Ref: https://howtodoinjava.com/java/date-time/resolverstyle-strict-date-parsing/
    // Ref: https://www.baeldung.com/java-datetimeformatter#dateFormatter
    private final DateTimeFormatter dateFormat =
            DateTimeFormatter.ofPattern("MM/dd/uuuu").withResolverStyle(ResolverStyle.STRICT);

    // Public
    // =========================================================================================
    public InputValidator() {}

    // ID must be a positive whole number
    public boolean isValidId(int id) { return id > 0; }

    // Name and type can't be blank or too long (>50)
    public boolean isValidText(String text) {
        return text != null && !text.trim().isEmpty() && text.trim().length() <= maxTextLength;
    }

    // Convert MM/dd/yyyy to LocalDate.
    // Returns null if input isn't real date (error handle)
    public LocalDate parseDate(String text) {
        if (text == null) { return null; }
        try {
            return LocalDate.parse(text.trim(), dateFormat);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    public boolean isValidDate(String text) { return parseDate(text) != null; }

    // Next inspection must come after last one
    // (Catches divide by zero calculation)
    public boolean isNextAfterLast(LocalDate last, LocalDate next) {
        return last != null && next != null && next.isAfter(last);
    }

    // Return status with correct capitalization (or null if invalid)
    public String formatStatus(String status) {
        if (status == null) { return null; }

        for (String valid : validStatuses) {
            if (valid.equalsIgnoreCase(status.trim())) {
                return valid;
            }
        }
        return null;
    }

    // Status must be one of the 4 options set
    public boolean isValidStatus(String status) {
        return formatStatus(status) != null;
    }

    // Return the list of statuses for ref
    public String getStatusOptions() { return String.join(", ", validStatuses); }

    // Return status for menu
    public String[] getValidStatuses() { return validStatuses.clone(); }

    // Verify crit limits
    public boolean isValidCriticality(int level) { return level >= 1 && level <= 3; }
}
