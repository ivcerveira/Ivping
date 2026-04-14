module com.nuapps.ivping {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.logging;
    requires java.prefs;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    opens com.nuapps.ivping to javafx.fxml;
    exports com.nuapps.ivping;
    exports com.nuapps.ivping.model;
    exports com.nuapps.ivping.controller;
    opens com.nuapps.ivping.controller to javafx.fxml;
}
