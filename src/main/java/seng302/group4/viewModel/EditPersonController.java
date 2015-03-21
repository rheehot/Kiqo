package seng302.group4.viewModel;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import seng302.group4.Person;
import seng302.group4.Project;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by James on 18/03/15.
 */
public class EditPersonController implements Initializable {
    private Stage stage;
    private Project project;
    private Person person;
    private boolean valid = false;
    // FXML Injections
    @FXML
    private Button cancelButton;
    @FXML
    private Button editPersonButton;
    @FXML
    private PersonFormController formController;


    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.setCancelButton();
        this.setSaveButton();

        Platform.runLater(() -> setProjectForFormController());
    }


    /**
     * Populates the fields with project data to enable editing
     *
     * @param person
     */
    public void loadPerson(final Person person) {
        this.person = person;
        this.formController.loadPerson(person);
    }

    /**
     * Sets the event handler for the Save button, performs validation checks
     * and instantiates the new project if applicable
     */
    private void setSaveButton() {
        editPersonButton.setOnAction(event -> {
            // check to see that shortname and longname fields are populated and shortname is unique within the project
            formController.validate();
            if (formController.isValid()) {
                if (!formController.getShortName().equals(person.getShortName())) {
                    person.setShortName(formController.getShortName());
                }
                if (!formController.getLongName().equals(person.getLongName())) {
                    person.setLongName(formController.getLongName());
                }
                if (!formController.getDescription().equals(person.getDescription())) {
                    person.setDescription(formController.getDescription());
                }
                if (!formController.getUserID().equals(person.getUserID())) {
                    person.setUserID(formController.getUserID());
                }
                if (!formController.getEmailAddress().equals(person.getEmailAddress())) {
                    person.setEmailAddress(formController.getEmailAddress());
                }
                if (!formController.getPhoneNumber().equals(person.getPhoneNumber())) {
                    person.setPhoneNumber(formController.getPhoneNumber());
                }
                if (!formController.getDepartment().equals(person.getDepartment())) {
                    person.setDepartment(formController.getDepartment());
                }
                // Close the new project dialog (this window)
                this.stage.close();
            }
        });
    }

    public Project getProject() {
        return project;
    }

    /**
     * @return the valid
     */
    public boolean isValid() {
        return this.valid;
    }

    public void setStage(final Stage stage) {
        this.stage = stage;
    }

    private void setCancelButton() {
        this.cancelButton.setOnAction(event -> this.stage.close());
    }

    public void setProject(Project project) {
        this.project = project;
    }

    private void setProjectForFormController() {
        formController.setProject(project);
    }
}
