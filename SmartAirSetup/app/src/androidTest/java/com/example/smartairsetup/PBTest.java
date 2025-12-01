package com.example.smartairsetup;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class PBTest {
/*
    @Rule
    public ActivityScenarioRule<PBActivity> rule =
            new ActivityScenarioRule<>(PBActivity.class);

    @Test
    public void testParserValidInput() {
        rule.getScenario().onActivity(activity -> {
            activity.pbInput.setText("300");
            int result = activity.pefParser.parsePEF(activity.pbInput);
            assertEquals(300, result);
        });
    }

    @Test
    public void testEmptyInput() {
        rule.getScenario().onActivity(activity -> {
            activity.pbInput.setText("");
            int result = activity.pefParser.parsePEF(activity.pbInput);
            assertEquals(0, result);
        });
    }

    @Test
    public void testStoringPB() {
        rule.getScenario().onActivity(activity -> {
            activity.pefStorage = new ChildStorage();

            StorageChild entry = new StorageChild(300, 0, 0);
            activity.pefStorage.save("Alice", entry);

            StorageChild saved = activity.pefStorage.getAll().get("Alice");
            assertNotNull(saved);
            assertEquals(300, saved.getDailyPEF()); // PB stored in dailyPEF
        });
    }

    @Test
    public void testSavePB() {
        rule.getScenario().onActivity(activity -> {
            activity.pefStorage = new ChildStorage();

            activity.chooseChildButton.setText("Alice");
            activity.pbInput.setText("300");

            activity.savePB();

            StorageChild saved = activity.pefStorage.getAll().get("Alice");
            assertNotNull(saved);
            assertEquals(300, saved.getDailyPEF());
        });
    }

    @Test
    public void testNoChildSelected() {
        rule.getScenario().onActivity(activity -> {
            activity.pefStorage = new ChildStorage();

            String defaultChildText = activity.getString(R.string.choose_child);
            activity.chooseChildButton.setText(defaultChildText);

            activity.pbInput.setText("300");
            activity.savePB();

            assertTrue(activity.pefStorage.getAll().isEmpty());
        });
    }

    @Test
    public void testSaveWithEmptyPB() {
        rule.getScenario().onActivity(activity -> {
            activity.pefStorage = new ChildStorage();

            activity.chooseChildButton.setText("Alice");
            activity.pbInput.setText("");

            activity.savePB();

            assertTrue(activity.pefStorage.getAll().isEmpty());
        });
    }

    @Test
    public void testMultiChildrenPBSave() {
        rule.getScenario().onActivity(activity -> {
            activity.pefStorage = new ChildStorage();

            // Child 1
            activity.chooseChildButton.setText("Alice");
            activity.pbInput.setText("300");
            activity.savePB();

            // Child 2
            activity.chooseChildButton.setText("Bob");
            activity.pbInput.setText("350");
            activity.savePB();

            StorageChild alice = activity.pefStorage.getAll().get("Alice");
            StorageChild bob = activity.pefStorage.getAll().get("Bob");

            assertNotNull(alice);
            assertEquals(300, alice.getDailyPEF());

            assertNotNull(bob);
            assertEquals(350, bob.getDailyPEF());
        });
    }

 */
}