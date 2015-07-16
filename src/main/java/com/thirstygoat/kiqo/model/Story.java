package com.thirstygoat.kiqo.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Created by leroy on 15/05/15.
 */
public class Story extends Item {
    public static final int DEFAULT_PRIORITY = 0;
    public static final int MAX_PRIORITY = 1000;
    public static final int MIN_PRIORITY = -1000;

    private final StringProperty shortName;
    private final StringProperty longName;
    private final StringProperty description;
    private final ObjectProperty<Person> creator;
    private final ObjectProperty<Project> project;
    private final ObjectProperty<Backlog> backlog;
    private final IntegerProperty priority;
    private final ObservableList<AcceptanceCriteria> acceptanceCriteria;

    /**
     * no-arg constructor for JavaBeans compliance
     */
    public Story() {
        this.shortName = new SimpleStringProperty("");
        this.longName = new SimpleStringProperty("");
        this.description = new SimpleStringProperty("");
        this.creator = new SimpleObjectProperty<>(null);
        this.project = new SimpleObjectProperty<>(null);
        this.backlog = new SimpleObjectProperty<>(null);
        this.priority = new SimpleIntegerProperty(DEFAULT_PRIORITY);
        this.acceptanceCriteria = FXCollections.observableArrayList();
    }

    public Story(String shortName, String longName, String description, Person creator, Project project,
                 Backlog backlog, Integer priority) {
        this.shortName = new SimpleStringProperty(shortName);
        this.longName = new SimpleStringProperty(longName);
        this.description = new SimpleStringProperty(description);
        this.creator = new SimpleObjectProperty<>(creator);
        this.project = new SimpleObjectProperty<>(project);
        this.backlog = new SimpleObjectProperty<>(backlog);
        this.priority = new SimpleIntegerProperty(priority);
        this.acceptanceCriteria = FXCollections.observableArrayList();
    }

    @Override
    public String getShortName() {
        return shortName.get();
    }

    public void setShortName(String shortName) {
        this.shortName.set(shortName);
    }

    @Override
    public StringProperty shortNameProperty() {
        return shortName;
    }

    public String getLongName() {
        return longName.get();
    }

    public void setLongName(String longName) {
        this.longName.set(longName);
    }

    public StringProperty longNameProperty() {
        return longName;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public Person getCreator() {
        return creator.get();
    }

    public void setCreator(Person creator) {
        this.creator.set(creator);
    }

    public ObjectProperty<Person> creatorProperty() {
        return creator;
    }

    public ObjectProperty<Project> projectProperty() { return project; }

    public Project getProject() { return project.get(); }

    public void setProject(Project project) {
        this.project.set(project);
    }

    ObjectProperty<Backlog> backlogProperty() {
        return backlog;
    }

    public Backlog getBacklog() {
        return backlog.get();
    }

    public void setBacklog(Backlog backlog) {
        this.backlog.set(backlog);
    }

    public int getPriority() { return priority.get();}

    public void setPriority(int priority) {
        this.priority.set(priority);
    }

    public IntegerProperty priorityProperty() {
        return priority;
    }

    public ObservableList<AcceptanceCriteria> getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

}
