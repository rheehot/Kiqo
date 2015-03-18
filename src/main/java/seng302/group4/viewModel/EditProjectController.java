package seng302.group4.viewModel;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.controlsfx.control.PopOver;
import seng302.group4.Project;

import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Created by Bradley on 13/03/15.
 */
public class EditProjectController implements Initializable {
    private Stage stage;
    private File projectLocation;

    // FXML Injections
    @FXML
    private Button cancelButton;
    @FXML
    private Button saveChangesButton;
    @FXML
    private TextField nameTextField;
    @FXML
    private TextField shortNameTextField;
    @FXML
    private TextField projectLocationTextField;
    @FXML
    private Button openButton;
    @FXML
    private TextField descriptionTextField;

    private final int SHORT_NAME_SUGGESTED_LENGTH = 20;
    private boolean shortNameModified = false;

    private Project project;

    private PopOver errorPopOver = new PopOver();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setCancelButton();
        setSaveButton();
        setOpenButton();

        setErrorPopOvers();
    }

    /**
     * Populates the fields with project data to enable editing
     * @param project
     */
    public void loadProject(Project project) {
        this.project = project;
        nameTextField.setText(project.getLongName());
        shortNameTextField.setText(project.getShortName());
        projectLocationTextField.setText(project.getSaveLocation().getAbsolutePath());
        descriptionTextField.setText(project.getDescription());

        projectLocation = project.getSaveLocation();
    }

    /**
     * Sets focus listeners on text fields so PopOvers are hidden upon focus
     */
    private void setErrorPopOvers() {
        // Set PopOvers as not detachable so we don't have floating PopOvers
        errorPopOver.setDetachable(false);

        // Set handlers so that popovers are hidden on field focus
        nameTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                errorPopOver.hide();
            }
        });
        shortNameTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                errorPopOver.hide();
            }
        });
        projectLocationTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                errorPopOver.hide();
            }
        });
    }

    /**
     * Sets the event handler for the New Project Button, performs validation checks and instantiates the new project
     * if applicable
     */
    private void setSaveButton() {
        saveChangesButton.setOnAction(event -> {

            // Hide existing error message if there is one
            errorPopOver.hide();

            // Perform validity checks and create project
            if (checkName() && checkShortName() && checkSaveLocation()) {

                project.setLongName(nameTextField.getText());
                project.setShortName(shortNameTextField.getText());
                project.setSaveLocation(projectLocation);
                project.setDescription(descriptionTextField.getText());

                // Close the new project dialog (this window)
                stage.close();
            }
        });
    }

    /**
     * Checks to make sure that the save location has been set, and it is writable by the user
     * @return Whether or not the save location is valid/readable/writable
     */
    private boolean checkSaveLocation() {
        if (projectLocation == null) {
            // Then the user hasn't selected a project directory, alert them!
            errorPopOver.setContentNode(new Label("Please select a Project Location"));
            errorPopOver.show(projectLocationTextField);
            return false;
        }
        // Check read/write access
        if (!projectLocation.canRead()) {
            // Then we can't read from the directory, what's the point!
            errorPopOver.setContentNode(new Label("Can't read from the specified directory"));
            errorPopOver.show(projectLocationTextField);
            return false;
        }

        if (!projectLocation.canWrite()) {
            // Then we can't write to the directory
            errorPopOver.setContentNode(new Label("Can't write to the specified directory"));
            errorPopOver.show(projectLocationTextField);
            return false;
        }

        return true;
    }

    /**
     * Checks to make sure the short name is valid
     * @return Whether or not the short name is valid
     */
    private boolean checkShortName() {
        if (shortNameTextField.getText().length() == 0) {
            errorPopOver.setContentNode(new Label("Short name must not be empty"));
            errorPopOver.show(shortNameTextField);
            return false;
        }
//        TODO Check for uniqueness
//        if (!UNIQUE CHECKER) {
//            shortNamePopOver.setContentNode(new Label("Short name must be unique"));
//            shortNamePopOver.show(shortNameTextField);
//            shortNameTextField.requestFocus();
//        }
        return true;
    }

    /**
     * Checks to make sure the long name is valid
     * @return Whether or not the long name is valid
     */
    private boolean checkName() {
        if (nameTextField.getText().length() == 0) {
            errorPopOver.setContentNode(new Label("Name must not be empty"));
            errorPopOver.show(nameTextField);
            return false;
        }
        return true;
    }


    /**
     * Sets the open dialog functionality including getting the path chosen by the user.
     */
    private void setOpenButton() {
        openButton.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(stage);
            projectLocation = selectedDirectory;
            if (selectedDirectory != null) {
                projectLocationTextField.setText(selectedDirectory.getAbsolutePath());
            }
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Sets the cancel button functionality
     */
    private void setCancelButton() {
        cancelButton.setOnAction(event -> stage.close());
    }

    public Project getProject() {
        return project;
    }
}