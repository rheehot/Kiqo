package seng302.group4.undo;

import seng302.group4.Organisation;
import seng302.group4.Skill;


/**
 * Command to add a Skill to a Project
 *
 */
public class CreateSkillCommand extends Command<Skill> {
    private final Skill skill;
    private final Organisation organisation;

    /**
     * @param skill Skill created
     * @param organisation Project that the skill is to be associated with
     */
    public CreateSkillCommand(final Skill skill, final Organisation organisation) {
        this.skill = skill;
        this.organisation = organisation;
    }

    @Override
    public Skill execute() {
        organisation.getSkills().add(skill);
        return skill;
    }

    @Override
    public void undo() {
        // Goodbye skill
        organisation.getSkills().remove(skill);
    }

    @Override
    public String toString() {
        return "<Create Skill: \"" + skill.getShortName() + "\">";
    }

    @Override
    public String getType() {
        return "Create Skill";
    }

}
