module com.example.trackly {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires jbcrypt;
    requires java.sql;                      // Connection que Hibernate le presta al reporte
    requires net.sf.jasperreports.core;     // reporte mensual en PDF

    opens com.example.trackly.model to org.hibernate.orm.core;   // sin esto Hibernate no puede llenar los campos
    opens com.example.trackly.controller to javafx.fxml;         // para que el FXML encuentre los @FXML
    exports com.example.trackly;
}
