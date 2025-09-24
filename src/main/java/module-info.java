module com.nuapps.ivping {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.poi.poi;
    requires java.desktop;
    requires java.logging;
    requires org.apache.poi.ooxml;

    opens com.nuapps.ivping to javafx.fxml;
    exports com.nuapps.ivping;
    exports com.nuapps.ivping.model;
}
