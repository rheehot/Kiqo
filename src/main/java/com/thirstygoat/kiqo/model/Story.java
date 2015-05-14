package com.thirstygoat.kiqo.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Created by leroy on 15/05/15.
 */
public class Story extends Item {
    private SimpleStringProperty shortName;
    private SimpleStringProperty longName;
    private SimpleStringProperty description;
    private ObjectProperty<Person> creator;
    private SimpleIntegerProperty priority;

    public String getShortName() {
        return shortName.get();
    }

    public SimpleStringProperty shortNameProperty() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName.set(shortName);
    }

    public String getLongName() {
        return longName.get();
    }

    public SimpleStringProperty longNameProperty() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName.set(longName);
    }

    public String getDescription() {
        return description.get();
    }

    public SimpleStringProperty descriptionProperty() {
        return description;
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public Person getCreator() {
        return creator.get();
    }

    public ObjectProperty<Person> creatorProperty() {
        return creator;
    }

    public void setCreator(Person creator) {
        this.creator.set(creator);
    }

    public int getPriority() {
        return priority.get();
    }

    public SimpleIntegerProperty priorityProperty() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority.set(priority);
    }
}
