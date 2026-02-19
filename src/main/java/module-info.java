module com.drishti.dataviztool {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;

    opens com.drishti.dataviztool to javafx.fxml;
    exports com.drishti.dataviztool;
}