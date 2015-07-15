package com.thirstygoat.kiqo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;

import com.thirstygoat.kiqo.model.Backlog;
import com.thirstygoat.kiqo.model.Project;
import com.thirstygoat.kiqo.model.Scale;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;

import com.google.gson.JsonSyntaxException;
import com.thirstygoat.kiqo.model.Organisation;

// Methods are run in alphabetical order
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PersistenceManagerTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    Organisation organisation = null;

    /**
     * Tests that attempting to load a non-existent project file throws a {@link FileNotFoundException}.
     *
     * @throws Exception Exception
     */
    @Test
    public void testLoad_fileNotFound() throws Exception {
        thrown.expect(FileNotFoundException.class);

        final Organisation p = PersistenceManager.loadOrganisation(new File("a/non/existent/file/path"));
    }

    /**
     * Tests that attempting to load a badly-formed project file throws a {@link JsonSyntaxException}.
     *
     * @throws Exception Exception
     */

    @Test
    public void testLoad_invalidProjectFormat() throws Exception {
        thrown.expect(JsonSyntaxException.class);
        final File f = testFolder.newFile("test.json");

        try (final FileWriter fw = new FileWriter(f)) {
            fw.write("{"); // lone opening brace == bad json
        }

        final Organisation p = PersistenceManager.loadOrganisation(f);
    }

    /**
     * Tests that persistence manager handles scales okay.
     *
     * @throws Exception Exception
     */
    @Test
    public void test_scalePersistence() throws Exception {
        // Set scale and save to temporary file
        ClassLoader classLoader = getClass().getClassLoader();
        File f = new File(classLoader.getResource("scaletest.json").getFile());
        Organisation orgBefore = PersistenceManager.loadOrganisation(f);
        Project proj = orgBefore.getProjects().get(0);
        Backlog backlog = proj.getBacklogs().get(0);
        backlog.setScale(Scale.TSHIRT_SIZE);
        final File tmp = testFolder.newFile("test.json");
        PersistenceManager.saveOrganisation(tmp, orgBefore);

        // Load temporary file and check that scale has persisted
        Organisation orgAfter = PersistenceManager.loadOrganisation(tmp);
        proj = orgAfter.getProjects().get(0);
        backlog = proj.getBacklogs().get(0);
        Assert.assertTrue(backlog.getScale().equals(Scale.TSHIRT_SIZE));
    }
}
