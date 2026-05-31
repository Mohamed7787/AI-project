package ai;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Button;

/**
 * Controller for the main menu screen.
 * Launches the game with the chosen mode.
 */
public class MainMenuController {

    @FXML private Button btnHvH;
    @FXML private Button btnEasy;
    @FXML private Button btnMedium;
    @FXML private Button btnHard;

    @FXML
    private void onHumanVsHuman(ActionEvent event) {
        openGame(GameMode.HUMAN_VS_HUMAN);
    }

    @FXML
    private void onHumanVsAIEasy(ActionEvent event) {
        openGame(GameMode.HUMAN_VS_AI_EASY);
    }

    @FXML
    private void onHumanVsAIMedium(ActionEvent event) {
        openGame(GameMode.HUMAN_VS_AI_MEDIUM);
    }

    @FXML
    private void onHumanVsAIHard(ActionEvent event) {
        openGame(GameMode.HUMAN_VS_AI_HARD);
    }

    private void openGame(GameMode mode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameView.fxml"));
            Parent root = loader.load();

            GameController controller = loader.getController();
            controller.initGame(mode);

            Stage stage = (Stage) btnHvH.getScene().getWindow();
            stage.setScene(new Scene(root));

            String title;
            switch (mode) {
                case HUMAN_VS_AI_EASY:   title = "Quoridor  –  Human vs Computer (Easy)";   break;
                case HUMAN_VS_AI_MEDIUM: title = "Quoridor  –  Human vs Computer (Medium)"; break;
                case HUMAN_VS_AI_HARD:   title = "Quoridor  –  Human vs Computer (Hard)";   break;
                default:                 title = "Quoridor  –  Human vs Human";              break;
            }
            stage.setTitle(title);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
