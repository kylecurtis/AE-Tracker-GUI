module com.example.aetrackergui {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.aetrackergui to javafx.fxml;
    exports com.example.aetrackergui;
}