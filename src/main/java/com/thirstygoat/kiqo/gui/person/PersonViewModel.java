package com.thirstygoat.kiqo.gui.person;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;

import com.thirstygoat.kiqo.command.*;
import com.thirstygoat.kiqo.command.create.CreatePersonCommand;
import com.thirstygoat.kiqo.gui.ModelViewModel;
import com.thirstygoat.kiqo.model.*;
import com.thirstygoat.kiqo.util.*;

import de.saxsys.mvvmfx.utils.validation.*;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.collections.*;

public class PersonViewModel extends ModelViewModel<Person> {
    private ObservableRuleBasedValidator shortNameValidator;
    private ObservableRuleBasedValidator longNameValidator;
    private ObservableRuleBasedValidator userIdValidator;
    private ObservableRuleBasedValidator emailValidator;
    private ObservableRuleBasedValidator phoneNumberValidator;
    private ObservableRuleBasedValidator departmentValidator;
    private ObservableRuleBasedValidator descriptionValidator;
    private CompositeValidator allValidator;
	private ObjectBinding<ObservableList<Skill>> availableSkills;

    public PersonViewModel() {
        createValidators();
        
        availableSkills = Bindings.createObjectBinding(() -> { 
	    		if (organisationProperty().get() != null) {
					final ListProperty<Skill> skills = skills();
	                return organisationProperty().get().getSkills().stream()
	                        .filter(skill -> !skills.contains(skill))
	                        .collect(GoatCollectors.toObservableList());
	            } else {
	                return FXCollections.observableArrayList();
	            }
	        }, organisationProperty(), skills());
        organisationProperty().addListener((observable, oldValue, newValue) -> {
        	newValue.getSkills().addListener((ListChangeListener.Change<? extends Skill> change) -> {
        		availableSkills.invalidate();
        	});
        });
    }
    
    @Override
    protected Supplier<Person> modelSupplier() {
        return Person::new;
    }

    private void createValidators() {
        shortNameValidator = new ObservableRuleBasedValidator();
        BooleanBinding uniqueName = Bindings.createBooleanBinding(() -> 
            { 
                if (organisationProperty().get() != null) {
                    return Utilities.shortnameIsUnique(shortNameProperty().get(), modelWrapper.get(),
                                    organisationProperty().get().getPeople());
                } else {
                    return true; // no organisation means this isn't for real yet.
                }
            }, 
            shortNameProperty(), organisationProperty());
        shortNameValidator.addRule(shortNameProperty().isNotNull(), ValidationMessage.error("Name must not be empty"));
        shortNameValidator.addRule(shortNameProperty().length().greaterThan(0), ValidationMessage.error("Name must not be empty"));
        shortNameValidator.addRule(shortNameProperty().length().lessThan(Utilities.SHORT_NAME_MAX_LENGTH), ValidationMessage.error("Name must be less than " + Utilities.SHORT_NAME_MAX_LENGTH + " characters"));
        shortNameValidator.addRule(uniqueName, ValidationMessage.error("Name must be unique within organisation"));

        longNameValidator = new ObservableRuleBasedValidator(); // always true
        descriptionValidator = new ObservableRuleBasedValidator(); // always true
        userIdValidator = new ObservableRuleBasedValidator(); // always true
        emailValidator = new ObservableRuleBasedValidator(); // always true
        phoneNumberValidator = new ObservableRuleBasedValidator(); // always true
        departmentValidator = new ObservableRuleBasedValidator(); // always true
        
        allValidator = new CompositeValidator(shortNameValidator, descriptionValidator);
    }

    @Override
    public Command getCommand() {
        final Command command;

        if (!allValidation().isValid()) {
            LOGGER.log(Level.WARNING, "Fields are invalid, no command will be returned.");
            return null;
        } else if (!modelWrapper.isDifferent()) {
            LOGGER.log(Level.WARNING, "Nothing changed. No command will be returned");
            return null;
        }

        if (modelWrapper.get().getShortName().equals("")) { // Must be a new person
            final Person p = new Person(shortNameProperty().get(), longNameProperty().get(),
                    descriptionProperty().get(), userIdProperty().get(), emailProperty().get(),
                    phoneNumberProperty().get(), departmentProperty().get(), skills().get());
            command = new CreatePersonCommand(p, organisationProperty().get());
        } else {
            final ArrayList<Command> changes = new ArrayList<>();
            super.addEditCommands.accept(changes);

            if (!skills().get().equals(modelWrapper.get().getSkills())) {
                /** For some reason we need to create a new ArrayList here rather than just passing through
                 * skills().get() otherwise it doesn't work.
                 */
                final ArrayList<Skill> skills = new ArrayList<>();
                skills.addAll(skills().get());
                changes.add(new EditCommand<>(modelWrapper.get() , "skills", skills));
            }
            command = new CompoundCommand("Edit Person" , changes);
        }
        return command;
    }

    public StringProperty shortNameProperty() {
        return modelWrapper.field("shortName", Person::getShortName, Person::setShortName, "");
    }

    public StringProperty longNameProperty() {
        return modelWrapper.field("longName", Person::getLongName, Person::setLongName, "");
    }

    public StringProperty userIdProperty() {
        return modelWrapper.field("userId", Person::getUserId, Person::setUserId, "");
    }

    public StringProperty emailProperty() {
        return modelWrapper.field("emailAddress", Person::getEmailAddress, Person::setEmailAddress, "");
    }

    public StringProperty phoneNumberProperty() {
        return modelWrapper.field("phoneNumber", Person::getPhoneNumber, Person::setPhoneNumber, "");
    }

    public StringProperty departmentProperty() {
        return modelWrapper.field("department", Person::getDepartment, Person::setDepartment, "");
    }

    public ListProperty<Skill> skills() {
        return modelWrapper.field("skills", Person::getSkills, Person::setSkills, new ArrayList<Skill>());
    }

    public StringProperty descriptionProperty() {
        return modelWrapper.field("description", Person::getDescription, Person::setDescription, "");
    }

    protected ObjectBinding<ObservableList<Skill>> availableSkills() {
        return availableSkills;
    }
    
    public ValidationStatus shortNameValidation() {
        return shortNameValidator.getValidationStatus();
    }
    
    public ValidationStatus longNameValidation() {
        return longNameValidator.getValidationStatus();
    }
    
    public ValidationStatus userIdValidation() {
        return userIdValidator.getValidationStatus();
    }
    
    public ValidationStatus emailValidation() {
        return emailValidator.getValidationStatus();
    }
    
    public ValidationStatus phoneNumberValidation() {
        return phoneNumberValidator.getValidationStatus();
    }
    
    public ValidationStatus departmentValidation() {
        return departmentValidator.getValidationStatus();
    }
    
    public ValidationStatus descriptionValidation() {
        return descriptionValidator.getValidationStatus();
    }

    public ValidationStatus allValidation() {
        return allValidator.getValidationStatus();
    }
}
