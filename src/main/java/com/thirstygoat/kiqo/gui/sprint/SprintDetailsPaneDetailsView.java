package com.thirstygoat.kiqo.gui.sprint;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.fxml.*;
import javafx.scene.control.*;

import com.thirstygoat.kiqo.model.Story;
import com.thirstygoat.kiqo.util.*;

import de.saxsys.mvvmfx.*;

/**
* Created by Carina Blair on 3/08/2015.
*/
public class SprintDetailsPaneDetailsView implements FxmlView<SprintDetailsPaneDetailsViewModel>, Initializable {
    
    private Label placeHolder = new Label();
    
    @InjectViewModel
    private SprintDetailsPaneDetailsViewModel viewModel;

    @FXML
    private Label longNameLabel;
    @FXML
    private Label goalLabel;
    @FXML
    private Label teamLabel;
    @FXML
    private Label backlogLabel;
    @FXML
    private Label startDateLabel;
    @FXML
    private Label endDateLabel;
    @FXML
    private Label releaseLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private TableView<Story> storyTableView;
    @FXML
    private TableColumn<Story, String> shortNameTableColumn;

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        longNameLabel.textProperty().bind(viewModel.longNameProperty());
        goalLabel.textProperty().bind(viewModel.goalProperty());
        startDateLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            if (viewModel.startDateProperty().get() != null) {
                return viewModel.startDateProperty().get().format(Utilities.DATE_TIME_FORMATTER);
            }
            return "";
        }, viewModel.startDateProperty()));
        endDateLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            if (viewModel.endDateProperty().get() != null) {
                return viewModel.endDateProperty().get().format(Utilities.DATE_TIME_FORMATTER);
            }
            return "";
        }, viewModel.endDateProperty()));
        releaseLabel.textProperty().bindBidirectional(viewModel.releaseProperty(),
                StringConverters.releaseStringConverter(viewModel.organisationProperty()));
        descriptionLabel.textProperty().bind(viewModel.descriptionProperty());
        teamLabel.textProperty().bindBidirectional(viewModel.teamProperty(),
                StringConverters.teamStringConverter(viewModel.organisationProperty()));
        backlogLabel.textProperty().bindBidirectional(viewModel.backlogProperty(),
                StringConverters.backlogStringConverter(viewModel.organisationProperty()));

        storyTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        storyTableView.itemsProperty().bind(viewModel.stories());

        placeHolder.textProperty().set(SprintDetailsPaneDetailsViewModel.PLACEHOLDER);
    }

    public SprintDetailsPaneDetailsViewModel getViewModel() {
        return viewModel;
    }
}
