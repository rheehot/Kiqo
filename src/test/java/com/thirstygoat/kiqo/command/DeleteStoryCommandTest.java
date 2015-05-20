package com.thirstygoat.kiqo.command;

import com.thirstygoat.kiqo.model.Person;
import com.thirstygoat.kiqo.model.Project;
import com.thirstygoat.kiqo.model.Story;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by leroy on 15/05/2015
 */
public class DeleteStoryCommandTest {
    private Story story;
    private Project project;
    private Person person;
    private DeleteStoryCommand command;

    @Before
    public void setup() {
        project = new Project("", "");
        story = new Story("story1", "Story One", "descr", person, project, 9);
        project.observableStories().add(story);
        command = new DeleteStoryCommand(story);
    }

    @Test
    public void deleteStory_StoryRemovedFromProject() {
        Assert.assertTrue(project.getStories().contains(story));

        command.execute();

        Assert.assertFalse(project.getStories().contains(story));
    }

    @Test
    public void undoDeleteStory_StoryAddedBackToProject() {
        Assert.assertTrue(project.getStories().contains(story));

        command.execute();

        Assert.assertFalse(project.getStories().contains(story));

        command.undo();

        Assert.assertTrue(project.getStories().contains(story));
    }
}