module com.nuapps.ivping {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires org.apache.commons.collections4;
    requires java.desktop;

    opens com.nuapps.ivping to javafx.fxml;
    exports com.nuapps.ivping;

    exports com.nuapps.ivping.model; // novo
}
