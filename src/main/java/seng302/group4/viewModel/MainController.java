package seng302.group4.viewModel;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.*;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.StatusBar;
import seng302.group4.PersistenceManager;
import seng302.group4.Project;
import seng302.group4.undo.Command;
import seng302.group4.undo.CompoundCommand;
import seng302.group4.undo.CreateProjectCommand;
import seng302.group4.undo.UndoManager;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import java.io.File;

/**
 * Main controller for the primary view
 */
public class MainController implements Initializable {
    private Stage primaryStage;
    private AnchorPane listAnchorPane;
    private double dividerPosition;


    // FXML Injections
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private CheckMenuItem listToggleCheckMenuItem;
    @FXML
    private MenuItem quitMenuItem;
    @FXML
    private ListView<Project> mainListView;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private MenuItem newProjectMenuItem;
    @FXML
    private MenuItem openMenuItem;
    @FXML
    private MenuItem saveMenuItem;
    @FXML
    private MenuItem projectDetailsMenuItem;
    @FXML
    private MenuItem undoMenuItem;
    @FXML
    private MenuItem redoMenuItem;

    private ObservableList<Project> projects = FXCollections.observableArrayList();
    private Project selectedProject;

    private UndoManager undoManager = new UndoManager();

    private StatusBar statusBar = new StatusBar();
    final private String ALL_CHANGES_SAVED_TEXT = "All changes saved.";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setQuitMenuItem();
        setListToggleCheckMenuItem();
        setLayoutProperties();
        setNewProjectMenuItem();
        setProjectDetailsMenuItem();
        setUndoHandlers();

        setMainListView();
        setOpenMenu();
        setSaveMenu();
        setShortcuts();

        setStatusBar();
    }

    private void setStatusBar() {
        mainBorderPane.setBottom(statusBar);
        statusBar.setText(ALL_CHANGES_SAVED_TEXT);
    }

    private void setUndoHandlers() {
        undoMenuItem.setOnAction(event -> {
            undoManager.undoCommand();
            // Update status bar to show that there are unsaved changes.
            statusBar.setText("You have unsaved changes.");
        });

        redoMenuItem.setOnAction(event -> undoManager.redoCommand());

        undoManager.canUndoProperty.addListener((observable, oldValue, newValue) -> {
            undoMenuItem.setDisable(!newValue);
            if (newValue) {
                // Update text to say (eg. Undo 'Create Project')
                undoMenuItem.setText("Undo " + undoManager.getUndoType());
            } else {
                undoMenuItem.setText("Undo");
            }
        });

        undoManager.canRedoProperty.addListener((observable, oldValue, newValue) -> {
            redoMenuItem.setDisable(!newValue);
            if (newValue) {
                // Update text to say (eg. Redo 'Create Project');
                redoMenuItem.setText("Redo " + undoManager.getRedoType());
            } else {
                redoMenuItem.setText("Redo");
                if (undoManager.canUndoProperty.get()) {
                    undoMenuItem.setText("Undo " + undoManager.getUndoType());
                }
                statusBar.setText(ALL_CHANGES_SAVED_TEXT);
            }
        });

        undoManager.shouldUpdateMenuProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                if (undoManager.canUndoProperty.get()) {
                    undoMenuItem.setText("Undo " + undoManager.getUndoType());
                }
                if (undoManager.canRedoProperty.get()) {
                    redoMenuItem.setText("Redo " + undoManager.getRedoType());
                }
                undoManager.shouldUpdateMenuProperty.set(false);
            }
        });
    }

    private void setProjectDetailsMenuItem() {
        projectDetailsMenuItem.setOnAction(event -> {
            if (selectedProject != null) {
                editProjectDialog(selectedProject);
            } else {
                // Something went wrong and the button wasn't disabled, alert the user
                // TODO
            }
        });
    }

    private void refreshList() {
        Project tmp = selectedProject;
        mainListView.setItems(null);
        mainListView.setItems(projects);
        mainListView.getSelectionModel().select(null);
        mainListView.getSelectionModel().select(tmp);
    }

    private void setSaveMenu() {
        saveMenuItem.setOnAction(event -> {
            try {
                PersistenceManager.saveProject(selectedProject.getSaveLocation().getAbsoluteFile(), selectedProject);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            statusBar.setText(ALL_CHANGES_SAVED_TEXT);
        });
    }

    /**
     * Sets the shortcuts for the main window
     */
    private void setShortcuts() {
        newProjectMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        saveMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        openMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        listToggleCheckMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN));

        // Undo/redo
        undoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN));
        redoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN,
                KeyCombination.SHIFT_DOWN));
    }

    /**
     * Sets the handler so an open dialog is presented when the user clicks File->Open
     */
    private void setOpenMenu() {
        openMenuItem.setOnAction(event -> {
//            DirectoryChooser directoryChooser = new DirectoryChooser();
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files(.JSON)", "*.json"));
//            File filePath = fileChooser.showDialog(primaryStage);
            File filePath = fileChooser.showOpenDialog(primaryStage);

            if (filePath == null) { return; }
            // TODO Actually do something with the selected file
            try {
                Project project = PersistenceManager.loadProject(filePath);
                projects.add(project);
                System.out.println(project.toString() + " has been loaded successfully");
            } catch (Exception e) {
                System.out.println("Couldnt load project");
                e.printStackTrace();
            }
        });
    }

    /**
     * Sets the content for the main list view
     */
    private void setMainListView() {
        mainListView.setItems(projects);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem editContextMenu = new MenuItem("Edit Project");
        contextMenu.getItems().add(editContextMenu);

        mainListView.setContextMenu(contextMenu);

        editContextMenu.setOnAction(event -> {
            if (selectedProject != null) {
                editProjectDialog(selectedProject);
            }
        });

        // Set change listener for mainListView
        mainListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedProject = newValue;
            if (newValue != null) {
                // Then a project is selected, enable the Project Details MenuItem
                projectDetailsMenuItem.setDisable(false);
            } else {
                // No project selected, disable Project Details MenuItem
                projectDetailsMenuItem.setDisable(true);
            }
        });
    }

    /**
     * Sets layout specific properties
     */
    private void setLayoutProperties() {
        listAnchorPane = (AnchorPane) mainSplitPane.getItems().get(0);

    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Sets the functionality for the quit menu item
     */
    private void setQuitMenuItem() {
        quitMenuItem.setOnAction(event -> {
            primaryStage.close();
        });
    }

    /**
     * Sets the functionality for the toggle list view menu item
     */
    private void setListToggleCheckMenuItem() {
        listToggleCheckMenuItem.setSelected(true);
        listToggleCheckMenuItem.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // shows the list view
                mainSplitPane.getItems().add(0, listAnchorPane);
                mainSplitPane.setDividerPosition(0, dividerPosition);
            } else {
                // hides the list view
                dividerPosition = mainSplitPane.getDividerPositions()[0];
                mainSplitPane.getItems().remove(listAnchorPane);
            }

        });
    }

    private void setNewProjectMenuItem() {
        newProjectMenuItem.setOnAction(event -> {
            newProjectDialog();
        });
    }

    private void editProjectDialog(Project project) {
        // Needed to wrap the dialog box in runLater due to the dialog box occasionally opening twice (known FX issue)
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.setTitle("Edit Project");
            stage.initOwner(primaryStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setResizable(false);
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainController.class.getClassLoader().getResource("dialogs/editProject.fxml"));
            BorderPane root;
            try {
                root = loader.load();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Scene scene = new Scene(root);
            stage.setScene(scene);
            EditProjectController editProjectController = loader.getController();
            editProjectController.setStage(stage);
            editProjectController.loadProject(project);

            stage.showAndWait();
            if (editProjectController.valid) {
                Command c = new Command() {
                    CompoundCommand cc = editProjectController.command;

                    @Override
                    public Object execute() {
                        // Add to mainListView
                        return cc.execute();
                    }

                    @Override
                    public void undo() {
                        // Remove from mainListView
                        cc.undo();
                        refreshList();
                    }

                    @Override
                    public String getType() {
                        return "Edit Project";
                    }
                };
                undoManager.doCommand(c);
                refreshList();
            }
        });
    }

    private void newProjectDialog() {
        // Needed to wrap the dialog box in runLater due to the dialog box occasionally opening twice (known FX issue)
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.initOwner(primaryStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setResizable(false);
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainController.class.getClassLoader().getResource("dialogs/newProject.fxml"));
            BorderPane root;
            try {
                root = loader.load();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Scene scene = new Scene(root);
            stage.setScene(scene);
            NewProjectController newProjectController = loader.getController();
            newProjectController.setStage(stage);

            stage.showAndWait();
            if (newProjectController.valid) {
                Command c = new Command() {
                    CreateProjectCommand cpc =  new CreateProjectCommand(
                            newProjectController.shortName, newProjectController.longName,
                            newProjectController.projectLocation, newProjectController.description);
                    @Override
                    public Object execute() {
                        // Add to mainListView
                        Project project = cpc.execute();
                        addProject(project);
                        return project;
                    }

                    @Override
                    public void undo() {
                        // Remove from mainListView
                        projects.remove(cpc.getProject());
                        cpc.undo();
                    }

                    @Override
                    public String getType() {
                        return cpc.getType();
                    }
                };

                undoManager.doCommand(c);
            }
        });
    }



    /**
     * Adds the new project to the observable list so that it is visible in the list view
     * @param project New Project to be added
     */
    private void addProject(Project project) {
        if (project != null) {
            // Update View Accordingly
            projects.add(project);
            // Attempt to write file to disk
            try {
                PersistenceManager.saveProject(project.getSaveLocation().getAbsoluteFile(), project);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
