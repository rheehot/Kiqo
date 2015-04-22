package seng302.group4.undo;

import seng302.group4.Person;
import seng302.group4.Organisation;

/**
 * Command that adds a Person to a Project
 *
 * @author amy, Edited by Bradley 10/4/15
 *
 */
public class CreatePersonCommand extends Command<Person> {
    private final Person person;
    private final Organisation organisation;

    /**
     *
     * @param person
     *            Person to be added to project
     * @param organisation
     *            Project that person is to be added to
     */
    public CreatePersonCommand(final Person person, final Organisation organisation) {
        this.person = person;
        this.organisation = organisation;
    }

    @Override
    public Person execute() {
        organisation.getPeople().add(person);
        return person;
    }

    @Override
    public void undo() {
        // Goodbye person
        // But we'll keep a reference to you just in case we have to redo later
        // on...
        organisation.getPeople().remove(person);
    }

    @Override
    public String toString() {
        return "<Create Person: \"" + person.getShortName() + "\">";
    }

    @Override
    public String getType() {
        return "Create Person";
    }

}
