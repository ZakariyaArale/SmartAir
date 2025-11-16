package com.example.smartairsetup;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class PefTest {
    @Rule
    public ActivityScenarioRule<PEFActivity> rule =
            new ActivityScenarioRule<>(PEFActivity.class);

    @Test
    public void testPEFParserValidInput() {
        rule.getScenario().onActivity(activity -> {
            activity.dailyPEFInput.setText("400");
            int result = activity.pefParser.parsePEF(activity.dailyPEFInput);
            assertEquals(400, result);
        });
    }

    @Test
    public void testParserEmptyInput() {
        rule.getScenario().onActivity(activity -> {
            activity.dailyPEFInput.setText("");
            int result = activity.pefParser.parsePEF(activity.dailyPEFInput);
            assertEquals(0, result);
        });
    }

    @Test
    public void testStoring() {
        rule.getScenario().onActivity(activity -> {
            activity.pefStorage = new ChildStorage();

            StorageChild entry = new StorageChild(400, 350, 360);
            activity.pefStorage.save("Alice", entry);

            StorageChild saved = activity.pefStorage.getAll().get("Alice");
            assertNotNull(saved);

            assertEquals(400, saved.getDailyPEF());
            assertEquals(350, saved.getPrePEF());
            assertEquals(360, saved.getPostPEF());
        });
    }

    @Test
    public void testSavePEF() {
        rule.getScenario().onActivity(activity -> {
            activity.pefStorage = new ChildStorage();

            activity.chooseChildButton.setText("Alice");

            activity.dailyPEFInput.setText("400");
            activity.preMedicationPB.setText("350");
            activity.postMedicationPB.setText("360");

            activity.savePEF();

            StorageChild saved = activity.pefStorage.getAll().get("Alice");
            assertNotNull(saved);
            assertEquals(400, saved.getDailyPEF());
            assertEquals(350, saved.getPrePEF());
            assertEquals(360, saved.getPostPEF());
        });
    }

    @Test
    public void testEmptyDailyPEF() {
        rule.getScenario().onActivity(activity -> {
            activity.pefStorage = new ChildStorage();

            activity.chooseChildButton.setText("Alice");
            activity.dailyPEFInput.setText("");

            int result = activity.pefParser.parsePEF(activity.dailyPEFInput);
            assertEquals(0, result);
        });
    }

    @Test
    public void testMultiChildrenSave() {
        rule.getScenario().onActivity(activity -> {
            activity.pefStorage = new ChildStorage();

            // Child 1
            activity.chooseChildButton.setText("Alice");
            activity.dailyPEFInput.setText("400");
            activity.preMedicationPB.setText("350");
            activity.postMedicationPB.setText("360");
            activity.savePEF();

            // Child 2
            activity.chooseChildButton.setText("Bob");
            activity.dailyPEFInput.setText("450");
            activity.preMedicationPB.setText(""); // optional pre
            activity.postMedicationPB.setText(""); // optional post
            activity.savePEF();

            StorageChild alice = activity.pefStorage.getAll().get("Alice");
            StorageChild bob = activity.pefStorage.getAll().get("Bob");

            assertNotNull(alice);
            assertEquals(400, alice.getDailyPEF());
            assertEquals(350, alice.getPrePEF());
            assertEquals(360, alice.getPostPEF());

            assertNotNull(bob);
            assertEquals(450, bob.getDailyPEF());
            assertEquals(0, bob.getPrePEF());
            assertEquals(0, bob.getPostPEF());
        });
    }

    @Test
    public void testSaveNoChildSelected() {
        rule.getScenario().onActivity(activity -> {
            activity.pefStorage = new ChildStorage();

            String defaultChildText = activity.getString(R.string.choose_child);
            activity.chooseChildButton.setText(defaultChildText);

            activity.dailyPEFInput.setText("400");
            activity.preMedicationPB.setText("350");
            activity.postMedicationPB.setText("360");

            activity.savePEF();

            assertTrue(activity.pefStorage.getAll().isEmpty());
        });
    }

    @Test
    public void testSaveWithEmptyDailyPEF() {
        rule.getScenario().onActivity(activity -> {
            activity.pefStorage = new ChildStorage();

            activity.chooseChildButton.setText("Alice");
            activity.dailyPEFInput.setText("");
            activity.preMedicationPB.setText("350");
            activity.postMedicationPB.setText("360");

            activity.savePEF();

            assertTrue(activity.pefStorage.getAll().isEmpty());
        });
    }
}
