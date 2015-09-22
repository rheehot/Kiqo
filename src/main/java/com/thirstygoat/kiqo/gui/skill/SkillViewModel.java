package com.thirstygoat.kiqo.gui.skill;

import java.util.ArrayList;
import java.util.function.Supplier;

import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import com.thirstygoat.kiqo.command.*;
import com.thirstygoat.kiqo.command.create.CreateSkillCommand;
import com.thirstygoat.kiqo.gui.Loadable;
import com.thirstygoat.kiqo.gui.ModelViewModel;
import com.thirstygoat.kiqo.model.*;
import com.thirstygoat.kiqo.util.*;

import de.saxsys.mvvmfx.ViewModel;
import de.saxsys.mvvmfx.utils.mapping.ModelWrapper;
import de.saxsys.mvvmfx.utils.validation.*;

public class SkillViewModel extends ModelViewModel<Skill> {

    private ObservableRuleBasedValidator nameValidator;
    private ObservableRuleBasedValidator descriptionValidator;
    private CompositeValidator allValidator;
    
    public SkillViewModel() {
    	super();
        createValidators();
    }
    
    private void createValidators() {
        nameValidator = new ObservableRuleBasedValidator();
        BooleanBinding uniqueName = Bindings.createBooleanBinding(() -> 
            { 
                if (organisationProperty().get() != null) {
                    return Utilities.shortnameIsUnique(nameProperty().get(), modelWrapper.get(), organisationProperty().get().getSkills());
                } else {
                    return true; // no organisation means this isn't for real yet.
                }
            }, 
            nameProperty());
        nameValidator.addRule(nameProperty().isNotNull(), ValidationMessage.error("Name must not be empty"));
        nameValidator.addRule(nameProperty().length().greaterThan(0), ValidationMessage.error("Name must not be empty"));
        nameValidator.addRule(nameProperty().length().lessThan(20), ValidationMessage.error("Name must be less than 20 characters"));
        nameValidator.addRule(uniqueName, ValidationMessage.error("Name must be unique within organisation"));

        descriptionValidator = new ObservableRuleBasedValidator(); // always true
        
        allValidator = new CompositeValidator(nameValidator, descriptionValidator);
    }

    @Override
	public Command getCommand() {
        final Command command;
        if (modelWrapper.get().getShortName().equals("")) { // edit
            final ArrayList<Command> changes = new ArrayList<>();
            super.addEditCommands.accept(changes);
            if (changes.size() > 0) {
                command = new CompoundCommand("Edit Skill", changes);
            } else {
                command = null;
            }
        } else { // new
            final Skill s = new Skill(nameProperty().get(), descriptionProperty().get());
            command = new CreateSkillCommand(s, organisationProperty().get());
        }
        return command;
    }

    protected StringProperty nameProperty() {
        return modelWrapper.field("shortName", Skill::getShortName, Skill::setShortName, "");
    }

    protected StringProperty descriptionProperty() {
        return modelWrapper.field("description", Skill::getDescription, Skill::setDescription, "");
    }
    
    protected ValidationStatus nameValidation() {
        return nameValidator.getValidationStatus();
    }
    
    protected ValidationStatus descriptionValidation() {
        return descriptionValidator.getValidationStatus();
    }

    protected ValidationStatus allValidation() {
        return allValidator.getValidationStatus();
    }
    
    public void onFirstChanged(Runnable runnable) {
    	ChangeListener<? super Boolean> listener = new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				runnable.run();
	    		observable.removeListener(this); // this listener will only be fired once
			}
    	};
		modelWrapper.dirtyProperty().addListener(listener);
	}

	@Override
	protected Supplier<Skill> modelSupplier() {
		// TODO Auto-generated method stub
		return null;
	}
}
