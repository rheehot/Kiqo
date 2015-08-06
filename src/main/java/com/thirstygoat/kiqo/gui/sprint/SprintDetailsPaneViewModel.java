package com.thirstygoat.kiqo.gui.sprint;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import com.thirstygoat.kiqo.model.Organisation;
import com.thirstygoat.kiqo.model.Sprint;
import com.thirstygoat.kiqo.util.StringConverters;

/**
 * Created by Carina Blair on 5/08/2015.
 */
public class SprintDetailsPaneViewModel extends SprintViewModel implements Loadable<Sprint> {
    public static final String PLACEHOLDER = "No stories in sprint";
    
    private final StringProperty backlogShortNameProperty;
    private final StringProperty teamShortNameProperty;
    private final StringProperty releaseShortNameProperty;
    
    public SprintDetailsPaneViewModel() {
        super();
        backlogShortNameProperty = new SimpleStringProperty("");
        teamShortNameProperty = new SimpleStringProperty("");
        releaseShortNameProperty = new SimpleStringProperty("");
    }

    /**
     * Bind to model so there is not copying data in
     */
    @Override
    public void load(Sprint sprint, Organisation organisation) {
        bindStringProperties();
        
        if (sprint != null) {
            goalProperty().bind(sprint.shortNameProperty());
            longNameProperty().bind(sprint.longNameProperty());
            descriptionProperty().bind(sprint.descriptionProperty());
            backlogProperty().bind(sprint.backlogProperty());
            startDateProperty().bind(sprint.startDateProperty());
            endDateProperty().bind(sprint.endDateProperty());
            teamProperty().bind(sprint.teamProperty());
            releaseProperty().bind(sprint.releaseProperty());
            stories().clear();
            stories().addAll(sprint.getStories());
        } else {
            goalProperty().unbind();
            longNameProperty().unbind();
            descriptionProperty().unbind();
            backlogProperty().unbind();
            startDateProperty().unbind();
            endDateProperty().unbind();
            teamProperty().unbind();
            releaseProperty().unbind();
            stories().clear();
        }
    }

    /**
     * The StringConverters must always be bound with the current organisation.
     * @param organisation
     */
    private void bindStringProperties() {
        backlogShortNameProperty.unbindBidirectional(backlogProperty());
        teamShortNameProperty.unbindBidirectional(teamProperty());
        releaseShortNameProperty.unbindBidirectional(releaseProperty());
        
        if (organisation != null) {
            backlogShortNameProperty.bindBidirectional(backlogProperty(), StringConverters.backlogStringConverter(organisation));
            teamShortNameProperty.bindBidirectional(teamProperty(), StringConverters.teamStringConverter(organisation));
            releaseShortNameProperty.bindBidirectional(releaseProperty(), StringConverters.releaseStringConverter(organisation));  
        }
    }

    protected StringProperty backlogShortNameProperty() {
        return backlogShortNameProperty;
    }
    
    protected StringProperty teamShortNameProperty() {
        return teamShortNameProperty;
    }

    protected StringProperty releaseShortNameProperty() {
        return releaseShortNameProperty;
    }
}

