package com.thirstygoat.kiqo.viewModel.detailControllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import com.thirstygoat.kiqo.model.Team;
import com.thirstygoat.kiqo.util.Utilities;
import com.thirstygoat.kiqo.viewModel.AllocationsTableViewController;
import com.thirstygoat.kiqo.viewModel.MainController;

public class TeamDetailsPaneController implements Initializable, IDetailsPaneController<Team> {

    @FXML
    private Label shortNameLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label teamMembersLabel;
    @FXML
    private Label poLabel;
    @FXML
    private Label smLabel;
    @FXML
    private Label devTeamLabel;
    @FXML
    private AllocationsTableViewController allocationsTableViewController;

    @Override
    public void showDetails(final Team team) {
        if (team != null) {
            shortNameLabel.textProperty().bind(team.shortNameProperty());
            descriptionLabel.textProperty().bind(team.descriptionProperty());
            teamMembersLabel.textProperty().bind(Utilities.commaSeparatedValuesProperty(team.observableTeamMembers()));

            if (team.getProductOwner() != null) {
                poLabel.textProperty().bind(team.getProductOwner().shortNameProperty());
            } else {
                poLabel.textProperty().unbind();
                poLabel.setText(null);
            }
            team.productOwnerProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    // Then the product owner is not null, proceed
                    poLabel.textProperty().unbind();
                    poLabel.textProperty().bind(newValue.shortNameProperty());
                } else {
                    poLabel.textProperty().unbind();
                    poLabel.setText(null);
                }
            });

            if (team.getScrumMaster() != null) {
                smLabel.textProperty().bind(team.getScrumMaster().shortNameProperty());
            } else {
                smLabel.textProperty().unbind();
                smLabel.setText(null);
            }
            team.scrumMasterProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    // Then the scrum master is not null, proceed
                    smLabel.textProperty().unbind();
                    smLabel.textProperty().bind(newValue.shortNameProperty());
                } else {
                    smLabel.textProperty().unbind();
                    smLabel.setText(null);
                }
            });

            devTeamLabel.textProperty().bind(Utilities.commaSeparatedValuesProperty(team.observableDevTeam()));

            if (team.getAllocations() != null) {
                allocationsTableViewController.setItems(team.observableAllocations());
            }
        } else {
            shortNameLabel.setText(null);
            descriptionLabel.setText(null);
            teamMembersLabel.setText(null);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        allocationsTableViewController.init(AllocationsTableViewController.FirstColumnType.PROJECT);
    }

    @Override
    public void setMainController(MainController mainController) {
        allocationsTableViewController.setMainController(mainController);
    }
}
