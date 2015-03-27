package seng302.group4.viewModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import org.controlsfx.control.StatusBar;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;

import seng302.group4.PersistenceManager;
import seng302.group4.Person;
import seng302.group4.Project;
import seng302.group4.exceptions.InvalidPersonException;
import seng302.group4.exceptions.InvalidProjectException;
import seng302.group4.undo.Command;
import seng302.group4.undo.CompoundCommand;
import seng302.group4.undo.CreatePersonCommand;
import seng302.group4.undo.CreateProjectCommand;
import seng302.group4.undo.UndoManager;

import com.google.gson.JsonSyntaxException;

/**
 * Main controller for the primary view
 */
public class MainController implements Initializable {
    private final UndoManager undoManager = new UndoManager();
    private final ObservableList<Project> projects = FXCollections.observableArrayList();
    private final ObservableList<Person> people = FXCollections.observableArrayList();
    private final StatusBar statusBar = new StatusBar();
    final private String ALL_CHANGES_SAVED_TEXT = "All changes saved.";
    final private String UNSAVED_CHANGES_TEXT = "You have unsaved changes.";
    private final SimpleBooleanProperty changesSaved = new SimpleBooleanProperty(true);
    private Stage primaryStage;
    private AnchorPane listAnchorPane;
    private double dividerPosition;
    // FXML Injections
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private ListView<Project> projectListView;
    @FXML
    private ListView<Person> peopleListView;
    @FXML
    private Tab projectTab;
    @FXML
    private Tab peopleTab;
    @FXML
    private TabPane tabViewPane;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private Label listLabel;
    @FXML
    private Pane detailsPane;
    @FXML
    private DetailsPaneController detailsPaneController;
    @FXML
    private MenuBarController menuBarController;
    private Project selectedProject;
    private Person selectedPerson;

    public void editPerson() {
        if (selectedPerson != null) {
            editPersonDialog(selectedPerson);
        }
    }

    public void editProject() {
        if (selectedProject != null) {
            editProjectDialog(selectedProject);
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
        setLayoutProperties();

        initialiseProjectListView();
        initialisePeopleListView();
        initialiseTabs();
        addStatusBar();
        menuBarController.setListenersOnUndoManager(undoManager);
    }

    private void initialiseTabs() {
        tabViewPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == projectTab) {
                projectListView.getSelectionModel().select(null);
                if (selectedProject != null) {
                    projectListView.getSelectionModel().select(selectedProject);
                } else {
                    projectListView.getSelectionModel().selectFirst();
                }

                menuBarController.updateAfterProjectListSelected(true);
            } else if (newValue == peopleTab) {
                if (selectedProject != null && !(selectedProject.getPeople().isEmpty())) {
                    people.setAll(selectedProject.getPeople());
                } else {
                    people.clear();
                    detailsPaneController.showDetailsPane(null);
                }
                peopleListView.getSelectionModel().select(null);
                peopleListView.getSelectionModel().selectFirst();

                menuBarController.updateAfterPersonListSelected(true);
            }
        });
    }

    public void newPerson() {
        if (selectedProject != null) {
            newPersonDialog();
        }
    }

    public void newProject() {
        if (selectedProject != null) {
            Dialogs.create().owner(primaryStage).title("Error")
            .message("Currently, only one project at a time is supported in this version.").showWarning();
            return;
        } else {
            newProjectDialog();
        }
    }

    public void openProject() {
        if (selectedProject != null) {
            Dialogs.create().owner(primaryStage).title("Error")
            .message("Currently, only one project at a time is supported in this version.").showWarning();
            return;
        }

        final FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files(.JSON)", "*.json"));
        final File filePath = fileChooser.showOpenDialog(primaryStage);

        if (filePath == null) {
            return;
        }
        // TODO Actually do something with the selected file
        Project project = null;
        try {
            project = PersistenceManager.loadProject(filePath);
        } catch (JsonSyntaxException | InvalidProjectException e) {
            System.out.println("JSON file invalid");
            e.printStackTrace();
        } catch (final InvalidPersonException e) {
            System.out.println("Person invalid");
            e.printStackTrace();
        } catch (final FileNotFoundException e) {
            System.out.println("file not found");
            e.printStackTrace();
        }
        if (project != null) {
            project.setSaveLocation(filePath);
            addProject(project);
            System.out.println(project.getShortName() + " has been loaded successfully");
        }
        tabViewPane.getSelectionModel().select(projectTab);
    }

    /**
     * Saves the project to disk and marks project as saved.
     */
    public void saveProject() {
        final Project project = selectedProject;
        try {
            PersistenceManager.saveProject(project.getSaveLocation(), project);
        } catch (final IOException e) {
            e.printStackTrace();
            return;
        }
        changesSaved.set(true);
    }

    public void setListVisible(boolean visible) {
        if (visible) {
            // shows the list view
            mainSplitPane.getItems().add(0, listAnchorPane);
            mainSplitPane.setDividerPosition(0, dividerPosition);
        } else {
            // hides the list view
            dividerPosition = mainSplitPane.getDividerPositions()[0];
            mainSplitPane.getItems().remove(listAnchorPane);
        }
    }

    public void setPrimaryStage(final Stage primaryStage) {
        this.primaryStage = primaryStage;
        addClosePrompt();
        menuBarController.setMainController(this);
        detailsPaneController.setMainController(this);
    }

    public void switchToPersonList() {
        tabViewPane.getSelectionModel().select(peopleTab);
    }

    public void switchToProjectList() {
        tabViewPane.getSelectionModel().select(projectTab);
    }

    public void undo() {
        undoManager.undoCommand();
        // If the changes are already saved, and we undo something, then the
        // changes are now not saved
        if (changesSaved.get()) {
            changesSaved.set(false);
        }
    }


    public void redo() {
        undoManager.redoCommand();
        // If the changes are already saved, and we redo something, then the
        // changes are now not saved
        if (changesSaved.get()) {
            changesSaved.set(false);
        }
    }

    /**
     * Set save prompt when to handle request to close application
     */
    private void addClosePrompt() {
        primaryStage.setOnCloseRequest(event -> {
            if (!changesSaved.get()) {
                final Action response = Dialogs.create().owner(primaryStage).title("Save Project")
                        .masthead("You have unsaved changes.").message("Would you like to save the changes you have made to the project?")
                        .showConfirm();
                if (response == Dialog.ACTION_YES) {
                    saveProject();
                } else if (response == Dialog.ACTION_CANCEL) {
                    event.consume();
                }
            }
        });
    }



    private void addPersonToList(Person person) {
        if (person != null) {
            // Update view accordingly
            people.add(person);

            // Select added person in the listView
            peopleListView.getSelectionModel().select(null);
            peopleListView.getSelectionModel().select(person);
            menuBarController.updateAfterPersonListSelected(true);
            switchToPersonList();

            // Save the project
            saveProject();
        }
    }

    /**
     * Adds the new project to the observable list so that it is visible in the list view
     * @param project New Project to be added
     */
    private void addProject(final Project project) {
        if (project != null) {
            // Update View Accordingly
            projects.add(project);
            // Select added project in the ListView
            projectListView.getSelectionModel().select(null);
            projectListView.getSelectionModel().select(project);
            // enable menuitem
            menuBarController.enableNewPerson();
            saveProject();
        }
    }

    /**
     * Set up the status bar for the application and monitor for changes in the
     * save state
     */
    private void addStatusBar() {
        // Add the status bar to the bottom of the window
        mainBorderPane.setBottom(statusBar);

        // Set up listener for save status
        changesSaved.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // If changes are saved, then update message to reflect that
                statusBar.setText(ALL_CHANGES_SAVED_TEXT);
            } else {
                // Then there are unsaved changes, update status message
                statusBar.setText(UNSAVED_CHANGES_TEXT);
            }
        });
    }

    private void editPersonDialog(Person person) {
        // Needed to wrap the dialog box in runLater due to the dialog box occasionally opening twice (known FX issue)
        Platform.runLater(() -> {
            final Stage stage = new Stage();
            stage.setTitle("Edit Person");
            stage.initOwner(primaryStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setResizable(false);
            final FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainController.class.getClassLoader().getResource("dialogs/editPerson.fxml"));
            BorderPane root;
            try {
                root = loader.load();
            } catch (final IOException e) {
                e.printStackTrace();
                return;
            }
            final Scene scene = new Scene(root);
            stage.setScene(scene);
            final EditPersonController editPersonController = loader.getController();
            editPersonController.setStage(stage);
            editPersonController.setProject(selectedProject);
            editPersonController.loadPerson(person);


            stage.showAndWait();

            if (editPersonController.isValid()) {
                final Command c = new Command() {
                    CompoundCommand cc = editPersonController.getCommand();

                    @Override
                    public Object execute() {
                        // Add to projectListView
                        cc.execute();
                        saveProject();
                        refreshList();
                        return null;
                    }

                    @Override
                    public String getType() {
                        return "Edit Person";
                    }

                    @Override
                    public void undo() {
                        // Remove from projectListView
                        cc.undo();
                        refreshList();
                    }
                };
                undoManager.doCommand(c);
            }
        });
    }

    private void editProjectDialog(Project project) {
        // Needed to wrap the dialog box in runLater due to the dialog box occasionally opening twice (known FX issue)
        Platform.runLater(() -> {
            final Stage stage = new Stage();
            stage.setTitle("Edit Project");
            stage.initOwner(primaryStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setResizable(false);
            final FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainController.class.getClassLoader().getResource("dialogs/editProject.fxml"));
            BorderPane root;
            try {
                root = loader.load();
            } catch (final IOException e) {
                e.printStackTrace();
                return;
            }
            final Scene scene = new Scene(root);
            stage.setScene(scene);
            final EditProjectController editProjectController = loader.getController();
            editProjectController.setStage(stage);
            editProjectController.loadProject(project);

            stage.showAndWait();
            if (editProjectController.isValid()) {
                final Command c = new Command() {
                    CompoundCommand cc = editProjectController.getCommand();

                    @Override
                    public Void execute() {
                        // Add to projectListView
                        cc.execute();
                        saveProject();
                        refreshList();
                        return null;
                    }

                    @Override
                    public String getType() {
                        return "Edit Project";
                    }

                    @Override
                    public void undo() {
                        // Remove from projectListView
                        cc.undo();
                        refreshList();
                    }
                };
                undoManager.doCommand(c);
            }
        });
    }

    /**
     * Sets the content for the main list view
     */
    private void initialiseProjectListView() {
        // derived from example at
        // http://docs.oracle.com/javafx/2/api/javafx/scene/control/Cell.html
        projectListView.setCellFactory(new Callback<ListView<Project>, ListCell<Project>>() {
            @Override
            public ListCell<Project> call(final ListView<Project> arg0) {
                return new ListCell<Project>() {
                    @Override
                    protected void updateItem(final Project project, final boolean empty) {
                        // calling super here is very important
                        super.updateItem(project, empty);
                        setText(empty ? "" : project.getShortName());
                    }
                };
            }
        });
        projectListView.setItems(projects);
        final ContextMenu contextMenu = new ContextMenu();
        final MenuItem editContextMenu = new MenuItem("Edit Project");
        contextMenu.getItems().add(editContextMenu);

        projectListView.setContextMenu(contextMenu);

        editContextMenu.setOnAction(event -> {
            editProject();
        });

        // Set change listener for projectListView
        projectListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectedProject = newValue;

                // Update status bar to show current save status of selected
                // project
                // Probably not the best way to do this, but it's the simplest
                changesSaved.set(!changesSaved.get());
                changesSaved.set(!changesSaved.get());

                menuBarController.updateAfterProjectSelected(selectedProject != null);
                listLabel.setText((selectedProject != null) ? selectedProject.getShortName() : null);

                detailsPaneController.showDetailsPane(selectedProject);
            }
        });
    }

    /**
     * Sets the content for the main list view
     */
    private void initialisePeopleListView() {
        peopleListView.setCellFactory(new Callback<ListView<Person>, ListCell<Person>>() {
            @Override
            public ListCell<Person> call(final ListView<Person> arg0) {
                return new ListCell<Person>() {
                    @Override
                    protected void updateItem(final Person person, final boolean empty) {
                        // calling super here is very important
                        super.updateItem(person, empty);
                        setText(empty ? "" : person.getShortName());
                    }
                };
            }
        });
        peopleListView.setItems(people);

        final ContextMenu contextMenu = new ContextMenu();
        final MenuItem editContextMenu = new MenuItem("Edit Person");
        contextMenu.getItems().add(editContextMenu);

        peopleListView.setContextMenu(contextMenu);

        editContextMenu.setOnAction(event -> {
            editPerson();
        });

        // Set change listener for projectListView
        peopleListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectedPerson = newValue;

                // Update status bar to show current save status of selected
                // project
                // Probably not the best way to do this, but it's the simplest
                changesSaved.set(!changesSaved.get());
                changesSaved.set(!changesSaved.get());

                    menuBarController.updateAfterPersonSelected(newValue != null);

                    detailsPaneController.showDetailsPane(selectedPerson);
            }
        });
    }

    private void newPersonDialog() {
        // Needed to wrap the dialog box in runLater due to the dialog box occasionally opening twice (known FX issue)
        Platform.runLater(() -> {
            final Stage stage = new Stage();
            stage.setTitle("New Person");
            stage.initOwner(primaryStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setResizable(false);
            final FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainController.class.getClassLoader().getResource("dialogs/newPerson.fxml"));
            BorderPane root;
            try {
                root = loader.load();
            } catch (final IOException e) {
                e.printStackTrace();
                return;
            }
            final Scene scene = new Scene(root);
            stage.setScene(scene);
            final NewPersonController newPersonController = loader.getController();
            newPersonController.setStage(stage);
            newPersonController.setProject(selectedProject);

            stage.showAndWait();
            if (newPersonController.isValid()) {
                final Command c = new Command() {
                    CreatePersonCommand cpc = newPersonController.getCommand();

                    @Override
                    public Object execute() {
                        // Add to projectListView
                        final Person person = cpc.execute();
                        selectedProject.addPerson(person);
                        addPersonToList(person);
                        return person;
                    }

                    @Override
                    public String getType() {
                        return cpc.getType();
                    }

                    @Override
                    public void undo() {
                        // Remove from projectListView
                        people.remove(cpc.getPerson());
                        selectedProject.removePerson(cpc.getPerson());
                        cpc.undo();
                    }
                };
                undoManager.doCommand(c);
            }
        });
    }

    private void newProjectDialog() {
        // Needed to wrap the dialog box in runLater due to the dialog box
        // occasionally opening twice (known FX issue)
        Platform.runLater(() -> {
            final Stage stage = new Stage();
            stage.initOwner(primaryStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setResizable(false);
            final FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainController.class.getClassLoader().getResource("dialogs/newProject.fxml"));
            BorderPane root;
            try {
                root = loader.load();
            } catch (final IOException e) {
                e.printStackTrace();
                return;
            }
            final Scene scene = new Scene(root);
            stage.setScene(scene);
            final NewProjectController newProjectController = loader.getController();
            newProjectController.setStage(stage);

            stage.showAndWait();
            if (newProjectController.isValid()) {
                final Command<Project> c = new Command<Project>() {
                    private final CreateProjectCommand cpc = newProjectController.getCommand();

                    @Override
                    public Project execute() {
                        // Add to projectListView
                        final Project project = cpc.execute();
                        addProject(project);
                        return project;
                    }

                    @Override
                    public String getType() {
                        return cpc.getType();
                    }

                    @Override
                    public void undo() {
                        // Remove from projectListView
                        projects.remove(cpc.getProject());
                        cpc.undo();
                    }
                };

                // this.undoManager.doCommand(c);
                // We don't do the command, since it is not meant to be undoable
                // at this stage
                c.execute();
            }
        });
    }

    /**
     * Forces a redraw of the list view (and the detailsPane)
     */
    private void refreshList() {
        if (projectTab.isSelected()) {
            final Project tmpProject = selectedProject;
            projectListView.setItems(null);
            projectListView.setItems(projects);
            projectListView.getSelectionModel().select(null);
            projectListView.getSelectionModel().select(selectedProject);
        } else if (peopleTab.isSelected()) {
            final Person tempPerson = selectedPerson;
            peopleListView.setItems(null);
            peopleListView.setItems(people);
            peopleListView.getSelectionModel().select(null);
            peopleListView.getSelectionModel().select(selectedPerson);
        }
    }

    /**
     * Sets layout specific properties
     */
    private void setLayoutProperties() {
        listAnchorPane = (AnchorPane) mainSplitPane.getItems().get(0);
    }
}
