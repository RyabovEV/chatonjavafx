module com.example.chatonjavafx {
    requires javafx.controls;
    requires javafx.fxml;


    exports com.example.chatonjavafx.client;
    opens com.example.chatonjavafx.client to javafx.fxml;
}