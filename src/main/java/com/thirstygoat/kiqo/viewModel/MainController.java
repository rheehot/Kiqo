package com.thirstygoat.kiqo.viewModel;


import com.google.gson.JsonSyntaxException;
import com.thirstygoat.kiqo.PersistenceManager;
import com.thirstygoat.kiqo.command.*;
import com.thirstygoat.kiqo.exceptions.InvalidPersonException;
import com.thirstygoat.kiqo.exceptions.InvalidProjectException;
import com.thirstygoat.kiqo.model.*;
import com.thirstygoat.kiqo.nodes.GoatDialog;
import com.thirstygoat.kiqo.reportGenerator.ReportGenerator;
import com.thirstygoat.kiqo.util.Utilities;
import com.thirstygoat.kiqo.viewModel.detailControllers.MainDetailsPaneController;
import com.thirstygoat.kiqo.viewModel.formControllers.AllocationFormController;
import com.thirstygoat.kiqo.viewModel.formControllers.IFormController;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import javafx.util.Callback;
import org.controlsfx.control.StatusBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Main controller for the primary view
 */
public class MainController implements Initializable {
    private static final String ALL_CHANGES_SAVED_TEXT = "All changes saved.";
    private static final String UNSAVED_CHANGES_TEXT = "You have unsaved changes.";
    private static final String PRODUCT_NAME = "Kiqo";
    private static final SimpleObjectProperty<Item> focusedItemProperty = new SimpleObjectProperty<>();
    private final UndoManager undoManager = new UndoManager();
    private final SimpleBooleanProperty changesSaved = new SimpleBooleanProperty(true);
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private ListView<Project> projectListView;
    @FXML
    private ListView<Person> peopleListView;
    @FXML
    private ListView<Skill> skillsListView;
    @FXML
    private ListView<Team> teamsListView;
    @FXML
    private ListView<Release> releasesListView;
    @FXML
    private Tab projectTab;
    @FXML
    private Tab peopleTab;
    @FXML
    private Tab skillsTab;
    @FXML
    private Tab teamsTab;
    @FXML
    private Tab releasesTab;
    @FXML
    private TabPane tabViewPane;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private Label listLabel;
    @FXML
    private Pane listPane;
    @FXML
    private Pane detailsPane;
    @FXML
    private MainDetailsPaneController detailsPaneController;
    @FXML
    private MenuBarController menuBarController;
    private Stage primaryStage;
    private double dividerPosition;

    private Organisation selectedOrganisation;
    private final ObjectProperty<Project> selectedProject = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Organisation> selectedOrganisationProperty = new SimpleObjectProperty<>();
    private Person selectedPerson;
    private Skill selectedSkill;
    private Team selectedTeam;
    private Release selectedRelease;

    private int savePosition = 0;
    private File lastSavedFile;

    private void setLastSavedFile(File file) {
        try {
            lastSavedFile = File.createTempFile("KIQO_LAST_SAVED_FILE", ".tmp");
            // Delete the tmp file upon exit of the application
            lastSavedFile.deleteOnExit();
            // Copy the opened file to the tmp file
            Files.copy(file.toPath(), lastSavedFile.toPath());
        } catch (final IOException e) {
            GoatDialog.showAlertDialog(primaryStage, "Error", "Something went wrong",
                    "Either the disk is full, or read/write access is disabled in your tmp directory.\n" +
                            "Revert functionality is disabled");
            // TODO Disable revert functionality
        }
    }

    private void revert() {
        // TODO checking stuff

        openOrganisation(lastSavedFile);
    }

    private void setStageTitleProperty() {
        // Add a listener to know when changes are saved, so that the title can be updated
        final StringProperty changesSavedAsterisk = new SimpleStringProperty(changesSaved.get() ? "" : "*");
        changesSaved.addListener((observable, oldValue, newValue) -> {
            changesSavedAsterisk.set(newValue ? "" : "*");
        });

        final StringProperty orgName = new SimpleStringProperty();
        orgName.bind(selectedOrganisationProperty.get().organisationNameProperty());
        selectedOrganisationProperty.addListener((observable, oldValue, newValue) -> {
            orgName.unbind();
            orgName.bind(newValue.organisationNameProperty());
        });

        primaryStage.titleProperty().bind(Bindings
                .concat(orgName)
                .concat(changesSavedAsterisk)
                .concat(" - ")
                .concat(MainController.PRODUCT_NAME));
    }

    /**
     * @param project organisation to be deleted
     *
     */
    private void deleteProject(Project project) {
        final DeleteProjectCommand command = new DeleteProjectCommand(project, selectedOrganisation);

        final String[] buttons = {"Delete Project", "Cancel"};
        final String result = GoatDialog.createBasicButtonDialog(primaryStage, "Delete Project", "Are you sure?",
                "Are you sure you want to delete the project " + project.getShortName() + "?", buttons);

        if (result.equals("Delete Project")) {
            doCommand(command);
        }

        if (selectedOrganisation.getProjects().size() < 1) {
            menuBarController.disableNewRelease();
        }
    }

    private void deleteSkill(Skill skill) {
        if (skill == selectedOrganisation.getPoSkill() || skill == selectedOrganisation.getSmSkill()) {
            GoatDialog.showAlertDialog(primaryStage, "Prohibited Operation", "Not allowed.",
                    "The Product Owner and Scrum Master skills cannot be deleted.");
        } else {

            String deleteMessage = "There are no people with this skill.";
            final DeleteSkillCommand command = new DeleteSkillCommand(skill, selectedOrganisation);
                if (command.getPeopleWithSkill().size() > 0) {
                deleteMessage = "Deleting the skill will also remove it from the following people:\n";
                deleteMessage += Utilities.concatenatePeopleList((command.getPeopleWithSkill()), 5);
            }
            final String[] buttons = { "Delete Skill", "Cancel" };
            final String result = GoatDialog.createBasicButtonDialog(primaryStage, "Delete Skill",
                    "Are you sure you want to delete the skill " + skill.getShortName() + "?", deleteMessage, buttons);

            if (result.equals("Delete Skill")) {
                doCommand(new DeleteSkillCommand(skill, selectedOrganisation));
            }
        }
    }

    private void deleteTeam(Team team) {
        final VBox node = new VBox();
        node.setSpacing(10);

        CheckBox checkbox;

        if (team.getTeamMembers().size() > 0) {
            checkbox = new CheckBox("Also delete the people belonging to this team");
            String deleteMessage = "Are you sure you want to delete the team: " + team.getShortName() +
                    "?\nCurrent team members:\n";
            deleteMessage += Utilities.concatenatePeopleList(team.getTeamMembers(), 5);
            node.getChildren().add(new Label(deleteMessage));
            node.getChildren().add(checkbox);
        } else {
            final String deleteMessage = "Are you sure you want to delete the team: " + team.getShortName() +
                    "?\nThis team has nobody in it.";
            node.getChildren().add(new Label(deleteMessage));
            checkbox = null;
        }

        final String[] buttons = { "Delete Team", "Cancel" };
        final String result = GoatDialog.createCustomNodeDialog(primaryStage, "Delete Team", "Are you sure?", node, buttons);

        // change this because its hasn't been init yet
        final boolean deletePeople = (checkbox != null) ? checkbox.selectedProperty().getValue() : false;

        if (result.equals("Delete Team")) {
            // Then delete the team
            // The result of whether or not to delete the team members can be
            // fetched by deletePeople boolean
            final DeleteTeamCommand command = new DeleteTeamCommand(team, selectedOrganisation);
            if (deletePeople) {
                command.setDeleteMembers();
            }
            doCommand(command);
        }
    }

    private void deletePerson(Person person) {

        final VBox node = new VBox();
        node.setSpacing(10);

        String deleteMessage = "Are you sure you want to remove the person: " + person.getShortName() + "?";
        if (person.getTeam() != null) {
            deleteMessage += "\nThis will remove " + person.getShortName() + " from team ";
            deleteMessage += person.getTeam().getShortName() + ".";
        }
        node.getChildren().add(new Label(deleteMessage));

        final String[] buttons = {"Delete Person", "Cancel"};

        final String result = GoatDialog.createCustomNodeDialog(primaryStage, "Delete Person",
                "Are you sure? ", node, buttons);

        if (result.equals("Delete Person")) {
            doCommand(new DeletePersonCommand(selectedPerson, selectedOrganisation));
        }
    }

    public void deleteRelease(Release release) {
       final VBox node = new VBox();
        node.setSpacing(10);

        final String deleteMessage = "Are you sure you want to remove the release: "
                + release.getShortName() + ", " + release.getDate() + "?";
        node.getChildren().add(new Label(deleteMessage));

        final String[] buttons = {"Delete Release", "Cancel"};
        final String result = GoatDialog.createCustomNodeDialog(primaryStage, "Delete Release",
                "Are you sure? ", node, buttons);

        if (result.equals("Delete Release")) {
            doCommand(new DeleteReleaseCommand(selectedRelease));
        }

    }

    public void deleteItem() {
        Platform.runLater(() -> {
            final Item focusedObject = MainController.focusedItemProperty.get();
            if (focusedObject == null) {
                // do nothing
            } else if (focusedObject instanceof Project) {
                deleteProject((Project) focusedObject);
            } else if (focusedObject instanceof Person) {
                deletePerson((Person) focusedObject);
            } else if (focusedObject instanceof Skill) {
                deleteSkill((Skill) focusedObject);
            } else if (focusedObject instanceof Team) {
                deleteTeam((Team) focusedObject);
            } else if (focusedObject instanceof Release) {
                deleteRelease((Release) focusedObject);
            }
        });
    }

    public void editItem() {
        final Item focusedObject = MainController.focusedItemProperty.get();
        if (focusedObject == null) {
            // do nothing
        } else if (focusedObject instanceof Project) {
            dialog((Project) focusedObject);
        } else if (focusedObject instanceof Person) {
            dialog((Person) focusedObject);
        } else if (focusedObject instanceof Skill) {
            if (focusedObject == selectedOrganisation.getPoSkill() || focusedObject == selectedOrganisation.getSmSkill()) {
                GoatDialog.showAlertDialog(primaryStage, "Prohibited Operation", "Not allowed.",
                        "The Product Owner and Scrum Master skills cannot be edited.");
            } else {
                dialog((Skill) focusedObject);
            }
        } else if (focusedObject instanceof Team) {
            dialog((Team) focusedObject);
        } else if (focusedObject instanceof Release) {
            dialog((Release) focusedObject);
        }
    }

    /**
     * Exits the application after prompting to save unsaved changes.
     *
     * We could just call primaryStage.close(), but that is a force close, and
     * then we can't prompt for saving changes
     */
    public void exit() {
        primaryStage.fireEvent(new WindowEvent(primaryStage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        selectedOrganisation = new Organisation();
        selectedOrganisationProperty.set(selectedOrganisation);

            // enable menu items
        menuBarController.enableNewTeam();
        menuBarController.enableNewPerson();
        menuBarController.enableNewSkill();

        initializeListViews();
        initialiseTabs();
        saveStateChanges();
        menuBarController.setListenersOnUndoManager(undoManager);
        MainController.focusedItemProperty.addListener((observable, oldValue, newValue) -> {
            System.out.println("Focus changed to " + newValue);
            detailsPaneController.showDetailsPane(newValue);
            menuBarController.updateAfterAnyObjectSelected(newValue != null);
        });

        selectedOrganisationProperty.addListener((observable, oldValue, newValue) -> {
            selectedOrganisation = newValue;
            setListViewData();
            // Clear undo/redo stack
            undoManager.empty();
            setNewReleaseEnabled();
        });

        Platform.runLater(() -> listLabel.setText(""));
    }

    private void initializeListViews() {
        setListViewData();

        // Get a list of them
        final ArrayList<ListView<? extends Item>> listViews = new ArrayList<>();
        listViews.add(projectListView);
        listViews.add(peopleListView);
        listViews.add(skillsListView);
        listViews.add(teamsListView);
        listViews.add(releasesListView);

        // All these ListViews share a single context menu
        final ContextMenu contextMenu = new ContextMenu();
        final MenuItem editContextMenu = new MenuItem("Edit");
        final MenuItem deleteContextMenu = new MenuItem("Delete");
        contextMenu.getItems().add(editContextMenu);
        contextMenu.getItems().add(deleteContextMenu);
        editContextMenu.setOnAction(event -> editItem());
        deleteContextMenu.setOnAction(event -> deleteItem());

        for (final ListView<? extends Item> listView : listViews) {
            initialiseListView(listView, contextMenu);
        }


        // set additional listeners so that the selection is retained despite
        // tab-switching
        projectListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // only for project: also update releases listView
            releasesListView.setItems(null);
            selectedProject.set(newValue);
            if (tabViewPane.getSelectionModel().selectedItemProperty().get() == projectTab) {
                MainController.focusedItemProperty.set(newValue);
            }

            if (newValue != null) {
                releasesListView.setItems(selectedProject.get().observableReleases());

                // Update list label
                if (projectListView.getItems().contains(newValue)) {
                    listLabel.textProperty().unbind();
                    listLabel.textProperty().bind(newValue.shortNameProperty());
                } else {
                    Platform.runLater(() -> listLabel.setText(""));
                }

            } else {
                // Update list label
                listLabel.textProperty().unbind();
                listLabel.setText(null);
            }
        });


        peopleListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedPerson = newValue;
            if (tabViewPane.getSelectionModel().selectedItemProperty().get() == peopleTab) {
                MainController.focusedItemProperty.set(newValue);
            }
        });
        skillsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedSkill = newValue;
            if (tabViewPane.getSelectionModel().selectedItemProperty().get() == skillsTab) {
                MainController.focusedItemProperty.set(newValue);
            }
        });
        teamsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedTeam = newValue;
            if (tabViewPane.getSelectionModel().selectedItemProperty().get() == teamsTab) {
                MainController.focusedItemProperty.set(newValue);
            }
        });
        releasesListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedRelease = newValue;
            if (tabViewPane.getSelectionModel().selectedItemProperty().get() == releasesTab) {
                MainController.focusedItemProperty.set(newValue);
            }
        });
    }


    private void setListViewData() {

        projectListView.setItems(selectedOrganisationProperty.getValue().getProjects());

        // ensure that you can only crate a realease if a project exists
        projectListView.getItems().addListener(new ListChangeListener<Project>() {
            @Override
            public void onChanged(Change<? extends Project> c) {
                setNewReleaseEnabled();
            }
        });

        peopleListView.setItems(selectedOrganisation.getPeople());
        teamsListView.setItems(selectedOrganisation.getTeams());
        skillsListView.setItems(selectedOrganisation.getSkills());
        // releases are looked after by projectListView selectionChangeListener


        switchToProjectList();
        projectListView.getSelectionModel().select(0);
    }

    public Organisation getSelectedOrganisation() {
        return selectedOrganisation;
    }

    public SimpleObjectProperty<Organisation> getSelectedOrganisationProperty() {
        return selectedOrganisationProperty;
    }

    /**
     * Sets if new release is enabled or not dependant on the existence of at lease 1 project
     */
    private void setNewReleaseEnabled() {
        if (projectListView.getItems().size() > 0) {
            menuBarController.enableNewRelease();
        } else {
            menuBarController.disableNewRelease();
        }
    }


    private void initialiseTabs() {
        tabViewPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == projectTab) {
                if (selectedProject == null) {
                    projectListView.getSelectionModel().selectFirst();
                }
                MainController.focusedItemProperty.set(selectedProject.get());

                menuBarController.updateAfterProjectListSelected(true);
            } else if (newValue == peopleTab) {
                if (selectedPerson == null) {
                    peopleListView.getSelectionModel().selectFirst();
                }
                MainController.focusedItemProperty.set(selectedPerson);

                menuBarController.updateAfterPersonListSelected(true);
            } else if (newValue == skillsTab) {
                if (selectedSkill == null) {
                    skillsListView.getSelectionModel().selectFirst();
                }
                MainController.focusedItemProperty.set(selectedSkill);

                menuBarController.updateAfterSkillListSelected(true);
            } else if (newValue == teamsTab) {
                if (selectedTeam == null) {
                    teamsListView.getSelectionModel().selectFirst();
                }
                MainController.focusedItemProperty.set(selectedTeam);

                menuBarController.updateAfterTeamListSelected(true);
            } else if (newValue == releasesTab) {
                if (selectedRelease == null) {
                    releasesListView.getSelectionModel().selectFirst();
                }
                MainController.focusedItemProperty.set(selectedRelease);

                menuBarController.updateAfterReleasesListSelected(true);
            }
        });
    }

    public void setSelectedTab(int tab) {
        switch (tab) {
            case 0:
                tabViewPane.getSelectionModel().select(projectTab);
                break;
            case 1:
                tabViewPane.getSelectionModel().select(teamsTab);
                break;
            case 2:
                tabViewPane.getSelectionModel().select(peopleTab);
                break;
            case 3:
                tabViewPane.getSelectionModel().select(skillsTab);
                break;
            case 4:
                tabViewPane.getSelectionModel().select(releasesTab);
                break;
        }
    }

    public void newSkill() {
        if (selectedOrganisation != null) {
            dialog(null, "Skill");
        }
    }

    public void newPerson() {
        if (selectedOrganisation != null) {
            dialog(null, "Person");
        }
    }

    public void newTeam() {
        if (selectedOrganisation != null) {
            dialog(null, "Team");
        }
    }

    public void newRelease() {
        if (selectedOrganisation != null) {
            dialog(null, "Release");
        }
    }

    public void newStory() {
        if (selectedOrganisation != null) {
            dialog(null, "Story");
        }
    }


    public void newProject() {
        if (selectedOrganisation != null) {
            dialog(null, "Project");

            if (selectedOrganisation.getProjects().size() > 0) {
                menuBarController.enableNewRelease();
            }
            if(selectedOrganisation.getProjects().size() > 0 && selectedOrganisation.getPeople().size() > 0) {
                menuBarController.enableNewStory();
            }
        }
    }

    public void allocateTeams() {
        if (selectedOrganisation != null ) {
            allocationDialog(null);
        }
    }

    public void openOrganisation(File draggedFilePath) {
        File filePath;

        if (selectedOrganisation != null) {
            if(!promptForUnsavedChanges()) {
                return;
            }
        }

        if (draggedFilePath == null) {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files(.JSON)", "*.json"));
            filePath = fileChooser.showOpenDialog(primaryStage);
        } else {
            filePath = draggedFilePath;
        }

        if (filePath == null) {
            return;
        }
        Organisation organisation;
        try {
            organisation = PersistenceManager.loadOrganisation(filePath);
            selectedOrganisationProperty.set(organisation);
            changesSaved.set(true);
            // Store the organisation as it currently stands
            setLastSavedFile(filePath);
        } catch (JsonSyntaxException | InvalidProjectException e) {
            GoatDialog.showAlertDialog(primaryStage, "Error Loading Project", "No can do.", "The JSON file you supplied is invalid.");
        } catch (final InvalidPersonException e) {
            GoatDialog.showAlertDialog(primaryStage, "Person Invalid", "No can do.", "An invalid person was found.");
        } catch (final FileNotFoundException e) {
            GoatDialog.showAlertDialog(primaryStage, "File Not Found", "No can do.", "Somehow, the file you tried to open was not found.");
        }

        if(PersistenceManager.getIsOldJSON()) {
            GoatDialog.showAlertDialog(primaryStage, "Warning", "An old JSON file has been loaded.", "You will need to allocate teams to your project [Project > Allocate Teams].");
            PersistenceManager.resetIsOldJSON();
        }
    }

    /**
     * Saves the project to disk and marks project as saved.
     */
    public void saveOrganisation() {
        final Organisation organisation = selectedOrganisation;
        // ask for save location if not yet set
        if (organisation.getSaveLocation() == null) {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files(.JSON)", "*.json"));
            final File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                organisation.setSaveLocation(file);
            }
        }
        // if successfully set
        if (organisation.getSaveLocation() != null) {
            try {
                PersistenceManager.saveOrganisation(organisation.getSaveLocation(), organisation);
            } catch (final IOException e) {
                GoatDialog.showAlertDialog(primaryStage, "Save failed", "No can do.", "Somehow, that file didn't allow saving.");
                return;
            }
            savePosition = undoManager.getUndoStackSize();
            changesSaved.set(true);
        }
    }

    /**
     * Prompts the user for a new save location via a filechooser.
     * Updates the organisation's current save location.
     * Saves the current organisation to it.
     */
    public void saveAsOrganisation() {
        final Organisation organisation = selectedOrganisation;
        final FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files(.JSON)", "*.json"));
        final File existingFile = selectedOrganisation.getSaveLocation();
        if (existingFile != null) {
            fileChooser.setInitialDirectory(existingFile.getParentFile());
            fileChooser.setInitialFileName(existingFile.getName());
        }
        final File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            organisation.setSaveLocation(file);
        }
        if (organisation.getSaveLocation() != null) {
            try {
                PersistenceManager.saveOrganisation(organisation.getSaveLocation(), organisation);
            } catch (final IOException e) {
                GoatDialog.showAlertDialog(primaryStage, "Save failed", "No can do.", "Somehow, that file didn't allow saving.");
                return;
            }
            changesSaved.set(true);
        }
    }

    public void setListVisible(boolean visible) {
        if (visible) {
            // shows the list view
            mainSplitPane.getItems().add(0, listPane);
            mainSplitPane.setDividerPosition(0, dividerPosition);
        } else {
            // hides the list view
            dividerPosition = mainSplitPane.getDividerPositions()[0];
            mainSplitPane.getItems().remove(listPane);
        }
    }

    public void switchToSkillList() {
        tabViewPane.getSelectionModel().select(skillsTab);
    }

    public void saveStatusReport() {
        final String EXTENSION = ".yaml";
        final FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("yaml Files", "*" + EXTENSION));
        final File existingFile = selectedOrganisation.getSaveLocation();
        if (existingFile != null) {
            fileChooser.setInitialDirectory(existingFile.getParentFile());
            fileChooser.setInitialFileName(selectedOrganisation.organisationNameProperty().get());
        }

        final File selectedFile = fileChooser.showSaveDialog(primaryStage);

        if (selectedFile != null) {
            try (final FileWriter fileWriter = new FileWriter(selectedFile)) {
                final ReportGenerator reportGenerator = new ReportGenerator(selectedOrganisation);
                fileWriter.write(reportGenerator.generateReport());
                fileWriter.close();
            } catch(final Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void switchToPersonList() {
        tabViewPane.getSelectionModel().select(peopleTab);
    }

    public void switchToTeamList() {
        tabViewPane.getSelectionModel().select(teamsTab);
    }

    public void switchToProjectList() {
        tabViewPane.getSelectionModel().select(projectTab);
    }

    public void switchToReleaseList() {
        tabViewPane.getSelectionModel().select(releasesTab);
    }

    public void undo() {
        undoManager.undoCommand();
        // If the changes are already saved, and we undo something, then the changes are now not saved
        changesSaved.set(undoManager.getUndoStackSize() == savePosition);
    }

    public void redo() {
        undoManager.redoCommand();
        // If the changes are already saved, and we redo something, then the changes are now not saved

        changesSaved.set(undoManager.getUndoStackSize() == savePosition);
    }

    public void doCommand(Command<?> command) {
        undoManager.doCommand(command);
        changesSaved.set(false);
    }

    /**
     * Set save prompt when to handle request to close application
     */
    private void addClosePrompt() {
        primaryStage.setOnCloseRequest(event -> {
            if (!promptForUnsavedChanges()) {
                event.consume();
            }
        });
    }

    /**
     * Prompt the user if they want to save unsaved changes
     * @return if the user clicked cancel or not
     */
    private boolean promptForUnsavedChanges() {
        if (!changesSaved.get()) {
            final String[] options = {"Save changes", "Discard changes", "Cancel"};
            final String response = GoatDialog.createBasicButtonDialog(primaryStage, "Save Project", "You have unsaved changes.",
                    "Would you like to save the changes you have made to the project?", options);
            if (response.equals("Save changes")) {
                saveOrganisation();
            } else if (response.equals("Cancel")) {
                // do nothing
                return false;
            }
        }
        return true;
    }

    /**
     * Set up the status bar for the application and monitor for changes in the
     * save state
     */
    private void saveStateChanges() {
        final StatusBar statusBar = new StatusBar();
        // Add the status bar to the bottom of the window
        mainBorderPane.setBottom(statusBar);

        // Set up listener for save status
        changesSaved.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // If changes are saved, then update message to reflect that
                statusBar.setText(MainController.ALL_CHANGES_SAVED_TEXT);
            } else {
                // Then there are unsaved changes, update status message
                statusBar.setText(MainController.UNSAVED_CHANGES_TEXT);
            }
        });
    }

    /**
     * Attaches cell factory and selection listener to the list view.
     */
    private <T extends Item> void initialiseListView(ListView<T> listView, ContextMenu contextMenu) {
        // derived from example at
        // http://docs.oracle.com/javafx/2/api/javafx/scene/control/Cell.html
        listView.setCellFactory(new Callback<ListView<T>, ListCell<T>>() {
            @Override
            public ListCell<T> call(final ListView<T> arg0) {
                return new ListCell<T>() {
                    @Override
                    protected void updateItem(final T item, final boolean empty) {
                        // calling super here is very important
                        super.updateItem(item, empty);
                        if (item != null) {
                            textProperty().bind(item.shortNameProperty());
                            setContextMenu(contextMenu);
                        } else {
                            textProperty().unbind();
                            setText("");
                            setContextMenu(null);
                        }
                    }
                };
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // Update status bar to show current save status
                // Probably not the best way to do this, but it's the simplest
                changesSaved.set(!changesSaved.get());
                changesSaved.set(!changesSaved.get());
            }
        });
    }

    /**
     * Convenience method for {dialog(t, type)}
     * @param t must not be null
     */
    private <T> void dialog(T t) {
        final String[] fullname = t.getClass().getName().split("\\.");
        final String name = fullname[fullname.length - 1];
        dialog(t, name);
    }

    /**
     *
     * @param t may be null
     * @param type type of t. This is displayed in the dialog title and also used to retrieve the fxml file, eg. "Project" => "forms/project.fxml".
     */
    private <T> void dialog(T t, String type) {
        Platform.runLater(() -> {
            final Stage stage = new Stage();
            stage.initOwner(primaryStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setResizable(false);
            stage.setTitle(t == null ? "New " : "Edit " + type);
            final FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainController.class.getClassLoader().getResource("forms/" + type.toLowerCase() + ".fxml"));
            Pane root;
            try {
                root = loader.load();
            } catch (final IOException e) {
                e.printStackTrace();
                return;
            }
            final Scene scene = new Scene(root);
            stage.setScene(scene);
            @SuppressWarnings("unchecked")
            final IFormController<T> formController = (IFormController<T>) loader.getController();
            formController.setStage(stage);
            formController.setOrganisation(selectedOrganisation);
            formController.populateFields(t);
            stage.showAndWait();
            if (formController.isValid()) {
                doCommand(formController.getCommand());
            }
        });
    }

    private void allocationDialog(Allocation allocation) {
        Platform.runLater(() -> {
            final Stage stage = new Stage();
            stage.initOwner(primaryStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setResizable(false);
            final FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainController.class.getClassLoader().getResource("forms/allocation.fxml"));
            Pane root;
            try {
                root = loader.load();
            } catch (final IOException e) {
                e.printStackTrace();
                return;
            }
            final Scene scene = new Scene(root);
            stage.setScene(scene);
            final AllocationFormController allocationFormController = loader.getController();
            allocationFormController.setStage(stage);
            allocationFormController.setOrganisation(selectedOrganisation);

            if (MainController.focusedItemProperty.getValue().getClass().equals(Team.class)) {
                allocationFormController.setProject(null);
                allocationFormController.setTeam((Team) MainController.focusedItemProperty.getValue());
            } else {
                allocationFormController.setProject(selectedProject.get());
                allocationFormController.setTeam(null);
            }

            allocationFormController.populateFields(allocation);

            stage.showAndWait();
            if (allocationFormController.isValid()) {
              doCommand(allocationFormController.getCommand());
            }
        });
    }



    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(final Stage primaryStage) {
        this.primaryStage = primaryStage;
        addClosePrompt();
        menuBarController.setMainController(this);
        detailsPaneController.setMainController(this);

        setStageTitleProperty();
    }

    public void newOrganisation() {
        if (selectedOrganisation != null) {
            if(!promptForUnsavedChanges()) {
                return;
            }
        }
        selectedOrganisationProperty.set(new Organisation());
    }
}
