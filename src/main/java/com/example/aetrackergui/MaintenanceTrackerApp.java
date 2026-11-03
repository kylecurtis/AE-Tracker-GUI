/* =============================================================================================
MAINTENANCE TRACKER APP:
    - Main application class for the Aerospace Equipment Maintenance Tracker (Phase 3 GUI).
    - Creates the EquipmentManager and the main window, then shows the window.
============================================================================================= */
package com.example.aetrackergui;

import javafx.application.Application;
import javafx.stage.Stage;

public class MaintenanceTrackerApp extends Application {

    // Private
    // =========================================================================================
    private final EquipmentManager manager = new EquipmentManager();

    // Public
    // =========================================================================================

    // JavaFX start (requires to be void)
    @Override
    public void start(Stage stage) {
        MainWindow window = new MainWindow(manager, stage);

        stage.setTitle("Aerospace Equipment Maintenance Tracker");
        stage.setScene(window.buildScene());
        stage.setMinWidth(900);
        stage.setMinHeight(500);
        stage.show();
    }
}
