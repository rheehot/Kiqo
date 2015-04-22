package seng302.group4.viewModel;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import seng302.group4.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Switches between detail panes depending on type of content shown.
 *
 * @author amy
 *
 */
public class DetailsPaneController implements Initializable {
    @FXML
    private BorderPane detailsPane;
    @FXML
    private StackPane stackPane;
    @FXML
    private AnchorPane projectDetailsPane;
    @FXML
    private AnchorPane personDetailsPane;
    @FXML
    private AnchorPane skillDetailsPane;
    @FXML
    private AnchorPane teamDetailsPane;
    @FXML
    private AnchorPane releaseDetailsPane;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button allocateTeamButton;
    @FXML
    private ProjectDetailsPaneController projectDetailsPaneController;
    @FXML
    private PersonDetailsPaneController personDetailsPaneController;
    @FXML
    private SkillDetailsPaneController skillDetailsPaneController;
    @FXML
    private TeamDetailsPaneController teamDetailsPaneController;
    @FXML
    private ReleaseDetailsPaneController releaseDetailsPaneController;

    private MainController mainController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clear();

        editButton.setOnAction(event -> mainController.editItem());
        deleteButton.setOnAction(event -> mainController.deleteItem());
//        allocateTeamButton.setOnAction(event -> mainController.allocateTeams());  // uncomment when amy finishes the dialog
    }


    /**
     * Display the details of the specified item.
     *
     * @param item item to be displayed
     */
    public void showDetailsPane(Item item) {
        if (item == null) {
            clear();
        } else {
            if (item instanceof Project) {
                showProjectDetailsPane((Project) item);
            } else if (item instanceof Person) {
                showPersonDetailsPane((Person) item);
            } else if (item instanceof Skill) {
                showSkillDetailsPane((Skill) item);
            } else if (item instanceof Team) {
                showTeamDetailsPane((Team) item);
            } else if (item instanceof Release) {
                showReleaseDetailPane((Release) item);
            }
        }
    }

    private void clear() {
        for (final Node node : stackPane.getChildren()) {
            node.setVisible(false);
        }
        editButton.setVisible(false);
        deleteButton.setVisible(false);
    }

    private void showSkillDetailsPane(Skill skill) {
        skillDetailsPaneController.showDetails(skill);

        skillDetailsPane.setVisible(true);
        projectDetailsPane.setVisible(false);
        personDetailsPane.setVisible(false);
        teamDetailsPane.setVisible(false);
        releaseDetailsPane.setVisible(false);

        showOptionButtons();
    }

    private void showProjectDetailsPane(Project project) {
        projectDetailsPaneController.showDetails(project);

        skillDetailsPane.setVisible(false);
        projectDetailsPane.setVisible(true);
        personDetailsPane.setVisible(false);
        teamDetailsPane.setVisible(false);
        releaseDetailsPane.setVisible(false);

        showOptionButtons();
        allocateTeamButton.setVisible(true);
    }

    private void showPersonDetailsPane(Person person) {
        personDetailsPaneController.showDetails(person);

        skillDetailsPane.setVisible(false);
        projectDetailsPane.setVisible(false);
        personDetailsPane.setVisible(true);
        teamDetailsPane.setVisible(false);
        releaseDetailsPane.setVisible(false);

        showOptionButtons();
    }

    private void showTeamDetailsPane(Team team) {
        teamDetailsPaneController.showDetails(team);

        skillDetailsPane.setVisible(false);
        projectDetailsPane.setVisible(false);
        personDetailsPane.setVisible(false);
        teamDetailsPane.setVisible(true);
        releaseDetailsPane.setVisible(false);

        showOptionButtons();
    }

    private void showReleaseDetailPane(Release release) {
        releaseDetailsPaneController.showDetails(release);

        skillDetailsPane.setVisible(false);
        projectDetailsPane.setVisible(false);
        personDetailsPane.setVisible(false);
        teamDetailsPane.setVisible(false);
        releaseDetailsPane.setVisible(true);

        showOptionButtons();
    }

    private void showOptionButtons() {
        editButton.setVisible(true);
        deleteButton.setVisible(true);
        allocateTeamButton.setVisible(false);
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}