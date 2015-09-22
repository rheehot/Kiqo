package com.thirstygoat.kiqo.gui.effort;

import com.thirstygoat.kiqo.command.Command;
import com.thirstygoat.kiqo.command.CompoundCommand;
import com.thirstygoat.kiqo.command.UndoManager;
import com.thirstygoat.kiqo.command.create.CreateEffortCommand;
import com.thirstygoat.kiqo.gui.Editable;
import com.thirstygoat.kiqo.gui.ModelViewModel;
import com.thirstygoat.kiqo.model.*;
import com.thirstygoat.kiqo.util.Utilities;
import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Created by leroy on 19/09/15.
 */
public class EffortViewModel extends ModelViewModel<Effort> implements Editable {
    private FunctionBasedValidator<Person> personValidator;
    private CompositeValidator allValidator;
    private ObjectProperty<Effort> effort;

    private ObjectProperty<LocalDate> endDateProperty = new SimpleObjectProperty<>(LocalDate.now());
    private ObjectProperty<LocalTime> endTimeProperty = new SimpleObjectProperty<>(LocalTime.now());

    private StringProperty endDateStringProperty;


    public EffortViewModel() {
        super();
        // TODO check that person is in team associated with this sprint, when assignment is working.
        personValidator = new FunctionBasedValidator<>(personProperty(),
                person -> person != null && organisationProperty().get().getPeople().contains(person),
                ValidationMessage.error("Person must exist"));

        allValidator = new CompositeValidator();
        allValidator.addValidators(personValidator);
        effort = new SimpleObjectProperty<>();

        endDateStringProperty = new SimpleStringProperty("");
        endDateStringProperty.bind(
                Bindings.createStringBinding(() -> endDateProperty().get() != null ?
                        endDateProperty().get().format(Utilities.DATE_FORMATTER)
                        : "", endDateProperty())
        );

        ChangeListener listener = (observable, oldValue, newValue) -> {
            LocalDate endDate = endDateProperty.get();
            LocalTime endTime = endTimeProperty().get();
            if (endTime == null) {
                endTime = LocalTime.now();
            }
            LocalDateTime dateTime = LocalDateTime.of(
                    endDate.getYear(), endDate.getMonth(), endDate.getDayOfMonth(),
                    endTime.getHour(), endTime.getMinute()
            );
            dateTime.plusHours(endTimeProperty.get().getHour());
            dateTime.plusMinutes(endTimeProperty.get().getMinute());
            endDateTimeProperty().setValue(dateTime);
        };
        endDateTimeProperty().addListener((observable, oldValue, newValue) -> System.out.println(newValue));

        endDateProperty.addListener(listener);
        endTimeProperty.addListener(listener);
        endDateTimeProperty().setValue(LocalDateTime.now());
    }

    @Override
    public void load(Effort item, Organisation organisation) {
        organisationProperty().set(organisation);
        effortObjectProperty().setValue(item);

        if (item != null) {
            modelWrapper.set(item);
        } else {
            modelWrapper.set(modelSupplier().get());
            modelWrapper.reset();
            modelWrapper.commit();
        }
        reload();
    }

    @Override
    protected Supplier<Effort> modelSupplier() {
        return Effort::new;
    }

    @Override
    public Command getCommand() {
        final Command command;

        if (effort.get() == null) {
            Effort effort = new Effort(personProperty().get(), taskProperty().get(), endDateTimeProperty().get(), durationProperty().get(), commentProperty().get());
            command = new CreateEffortCommand(effort, taskProperty().get());
        } else {
            if (!allValidation().isValid()) {
                LOGGER.log(Level.WARNING, "Fields are invalid, no command will be returned.");
                return null;
            } else if (!modelWrapper.isDirty()) {
                LOGGER.log(Level.WARNING, "Nothing changed. No command will be returned");
                return null;
            }

            final ArrayList<Command> changes = new ArrayList<>();
            super.addEditCommands.accept(changes);
            command = changes.size() == 1 ? changes.get(0) : new CompoundCommand("Edit Effort", changes);
        }
        return command;
    }

    public void commitEdit() {
        Command command = getCommand();
        if (command != null) {
            UndoManager.getUndoManager().doCommand(command);
        }
    }

    public void cancelEdit() {
        reload();
    }

    /** Model Fields **/

    public ObjectProperty<Person> personProperty() {
        return modelWrapper.field("person", Effort::getPerson, Effort::setPerson, null);
    }

    public ObjectProperty<Task> taskProperty() {
        return modelWrapper.field("task", Effort::getTask, Effort::setTask, null);
    }

    public ObjectProperty<LocalDateTime> endDateTimeProperty() {
        return modelWrapper.field("endDateTime", Effort::getEndDateTime, Effort::setEndDateTime, LocalDateTime.now());
    }

    public ObjectProperty<Duration> durationProperty() {
        return modelWrapper.field("duration", Effort::getDuration, Effort::setDuration, null);
    }

    public StringProperty commentProperty() {
        return modelWrapper.field("comment", Effort::getComment, Effort::setComment, "");
    }

    /** Extra Fields **/

    public ObjectProperty<LocalTime> endTimeProperty() {
        return endTimeProperty;
    }

    public ObjectProperty<LocalDate> endDateProperty() {
        return endDateProperty;
    }

    /** Validation **/

    public ValidationStatus personValidation() {
        return personValidator.getValidationStatus();
    }

    public ValidationStatus allValidation() {
        return allValidator.getValidationStatus();
    }

    public StringProperty endDateStringProperty() {
        return endDateStringProperty;
    }

    public ObjectProperty<Effort> effortObjectProperty() {
        return effort;
    }

    public ListProperty<Person> eligibleAssignees() {
        ListProperty<Person> eligableAssignees = new SimpleListProperty<>(FXCollections.observableArrayList());

        Function<Task, List<Person>> getEligibleAssignees = task -> {
            Optional<Sprint> sprintTaskBelongsTo = task.getStory().getBacklog().getProject().getReleases().stream()
                    .flatMap(release -> release.getSprints().stream())
                    .filter(sprint -> sprint.getStories().contains(task.getStory()))
                    .findAny();

            if (sprintTaskBelongsTo.isPresent()) {
                return sprintTaskBelongsTo.get().getTeam().getTeamMembers().stream()
                        .filter(person -> !task.getAssigneesObservable().contains(person))
                        .collect(Collectors.toList());
            } else {
                return new ArrayList<>();
            }
        };

        taskProperty().addListener((observable, oldValue, newValue) -> {
            eligableAssignees.setAll(getEligibleAssignees.apply(newValue));
        });
        eligableAssignees.setAll(taskProperty().get() != null ? getEligibleAssignees.apply(taskProperty().get()) : new ArrayList<>());

        return eligableAssignees;
    }
}