module ai {
    requires javafx.controls;
    requires javafx.fxml;

    opens ai to javafx.fxml;
    exports ai;

    exports quoridor.model;
    exports quoridor.logic;
    exports quoridor.ai;
}