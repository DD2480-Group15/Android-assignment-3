package protect.card_locker;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.os.Looper;
import android.app.Dialog;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowDialog;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.robolectric.Shadows.shadowOf;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import protect.card_locker.async.TaskHandler;
import protect.card_locker.viewmodels.LoyaltyCardEditActivityViewModel;

@RunWith(RobolectricTestRunner.class)
public class LoyaltyCardEditActivityTest {

    private ActivityController<LoyaltyCardEditActivity> controller;
    private LoyaltyCardEditActivity activity;
    private LoyaltyCardEditActivity.ChooseCardImage listener;
    private LoyaltyCardEditActivityViewModel mockViewModel;
    private LoyaltyCard card;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(LoyaltyCardEditActivity.class)
            .create()
            .start();

        activity = controller.get();

        mockViewModel = Mockito.mock(LoyaltyCardEditActivityViewModel.class);
        activity.viewModel = mockViewModel;

        card = new LoyaltyCard();
        card.store = "";
        card.note = "";
        card.cardId = "1";
        card.barcodeId = "";
        card.barcodeType = CatimaBarcode.fromPrettyName("Aztec"); // Set to default "noBarcode" if null
        card.barcodeEncoding = StandardCharsets.UTF_8;
        card.balanceType = Currency.getInstance("EUR");
        card.balance = new BigDecimal("10"); // Set to BigDecimal("0") if null
        card.validFrom = new Date(0);
        card.expiry = new Date(1);
        card.headerColor = Color.BLACK;

        Mockito.when(mockViewModel.getLoyaltyCard()).thenReturn(card);

        // Setup mock TaskHandler to prevent NullPointerException
        TaskHandler mockTaskHandler = Mockito.mock(TaskHandler.class);
        Mockito.when(mockViewModel.getTaskHandler()).thenReturn(mockTaskHandler);

        Mockito.doNothing().when(mockTaskHandler)
                .flushTaskList(
                        Mockito.eq(TaskHandler.TYPE.BARCODE),
                        Mockito.anyBoolean(),
                        Mockito.anyBoolean(),
                        Mockito.anyBoolean());

        Mockito.doNothing().when(mockTaskHandler)
                .executeTask(
                        Mockito.eq(TaskHandler.TYPE.BARCODE),
                        Mockito.any());

        listener = activity.new ChooseCardImage();
    }

    @After
    public void tearDown() {
        if (activity != null && activity.mDatabase != null && activity.mDatabase.isOpen()) {
            activity.mDatabase.close();
        }
    }

    /**
     * Tests the behavior of the {@code onResume} method when no chips are currently in the
     * {@code ChipGroup} and there is a matching group ID between the existing database groups
     * and the loyalty card groups.
     * <p>
     * Test setup:
     * <ol>
     *     <li>The chip group is ensured to be empty before calling {@code onResume}</li>
     *     <li>
     *         The {@code DBHelper} is mocked to return a group list when calling
     *         {@code getGroups} and another list when calling {@code getLoyaltyCardGroups}.
     *         The IDs of the groups in these lists match.
     *     </li>
     * </ol>
     * Expected outcome: one chip is added to the chip group, and it is checked.
     */
    @Test
    public void onResume_noChipsSameGroupId_addsCheckedChip() {
        Mockito.when(mockViewModel.getAddGroup()).thenReturn("notSameGroupId");
        Mockito.when(mockViewModel.getLoyaltyCardId()).thenReturn(1); // adjust if needed

        ChipGroup groupsChips = activity.groupsChips;
        // Ensure it's empty so that groupsChips.getChildCount() == 0 is true (branch 8)
        assertNotNull(groupsChips);
        assertEquals(0, groupsChips.getChildCount());

        String groupId = "sameGroupId";
        Group g1 = new Group(groupId, 0);
        List<Group> existingGroups = Collections.singletonList(g1);
        Group g2 = new Group(groupId, 0);
        List<Group> loyaltyCardGroups = Collections.singletonList(g2);

        try (MockedStatic<DBHelper> dbh = mockStatic(DBHelper.class)) {
            dbh.when(() -> DBHelper.getGroups(any(SQLiteDatabase.class)))
                    .thenReturn(existingGroups);

            dbh.when(() -> DBHelper.getLoyaltyCardGroups(any(SQLiteDatabase.class), Mockito.anyInt()))
                    .thenReturn(loyaltyCardGroups);

            // Act
            activity.onResume();

            dbh.verify(() -> DBHelper.getGroups(any(SQLiteDatabase.class)), Mockito.atLeastOnce());
            dbh.verify(() -> DBHelper.getLoyaltyCardGroups(any(SQLiteDatabase.class), Mockito.anyInt()), Mockito.atLeastOnce());
        }

        // Assert: chip added (because groups exist), and checked (because IDs match)
        assertEquals(1, groupsChips.getChildCount());
        View child = groupsChips.getChildAt(0);
        assertTrue(child instanceof Chip);

        Chip chip = (Chip) child;
        assertEquals(groupId, chip.getText().toString());
        assertTrue("Chip should be checked when IDs match", chip.isChecked());
        assertEquals(View.VISIBLE, groupsChips.getVisibility());
    }

    /**
     * Tests the behavior of the {@code onResume} method when there are no existing chips
     * and no loyalty card groups associated with the activity.
     * <p>
     * Test setup:
     * <ol>
     *     <li>The chip group is ensured to be empty before calling {@code onResume}</li>
     *     <li>
     *         The {@code DBHelper} is mocked to return a non empty group list when calling
     *         {@code getGroups} and an empty list when calling {@code getLoyaltyCardGroups}.
     *     </li>
     * </ol>
     * Expected outcome: one Chip is added to the chip group, and it is unchecked.
     */
    @Test
    public void onResume_noChipsEmptyLoyaltyCardGroups_AddsUncheckedChip() {
        Mockito.when(mockViewModel.getAddGroup()).thenReturn("notSameGroupId");
        Mockito.when(mockViewModel.getLoyaltyCardId()).thenReturn(1); // adjust if needed

        ChipGroup groupsChips = activity.groupsChips;
        // Ensure it's empty so that groupsChips.getChildCount() == 0 is true (branch 8)
        assertNotNull(groupsChips);
        assertEquals(0, groupsChips.getChildCount());

        String groupId = "groupId";
        Group g1 = new Group(groupId, 0);
        List<Group> existingGroups = Collections.singletonList(g1);
        List<Group> loyaltyCardGroups = Collections.emptyList();

        try (MockedStatic<DBHelper> dbh = mockStatic(DBHelper.class)) {
            dbh.when(() -> DBHelper.getGroups(any(SQLiteDatabase.class)))
                    .thenReturn(existingGroups);

            dbh.when(() -> DBHelper.getLoyaltyCardGroups(any(SQLiteDatabase.class), Mockito.anyInt()))
                    .thenReturn(loyaltyCardGroups);

            // Act
            activity.onResume();

            dbh.verify(() -> DBHelper.getGroups(any(SQLiteDatabase.class)), Mockito.atLeastOnce());
            dbh.verify(() -> DBHelper.getLoyaltyCardGroups(any(SQLiteDatabase.class), Mockito.anyInt()), Mockito.atLeastOnce());
        }

        // Assert: chip added (because groups exist), and checked (because IDs match)
        assertEquals(1, groupsChips.getChildCount());
        View child = groupsChips.getChildAt(0);
        assertTrue(child instanceof Chip);

        Chip chip = (Chip) child;
        assertEquals(groupId, chip.getText().toString());
        assertFalse("Chip should be unchecked when there are no LoyaltyCardGroups", chip.isChecked());
        assertEquals(View.VISIBLE, groupsChips.getVisibility());
    }

    /**
     * Tests the behavior of the {@code onResume} method when the {@code balance} of the card is null.
     * <p>
     * Test setup:
     * <ol>
     *     <li>The {@code balance} field of the {@code card} object is set to null.</li>
     * </ol>
     * Expected outcome: The {@code balance} field of the {@code card} object is set to {@code BigDecimal.ZERO}.
     */
    @Test
    public void onResume_balanceIsNull_setsBalanceToZero() {
        card.balance = null;

        activity.onResume();

        assertNotNull(card.balance);
        assertEquals(BigDecimal.ZERO, card.balance);
    }

    /**
     * Tests the behavior of the activity's {@code onResume} method when the header color is white.
     * <p>
     * Test setup:
     * <ol>
     *     <li>The header color is set to white.</li>
     *     <li>One chip is added to the chip group to skip that step (not necessary for this test)</li>
     *     <li>When calling {@code Utils.needsDarkForeground}, it returns true.</li>
     * </ol>
     * Expected outcome: The background color of {@code thumbnailEditIcon} is black, and a color filter is applied.
     */
    @Test
    public void onResume_whiteHeaderColor_setsDarkForeground() {
        card.headerColor = Color.WHITE;

        activity.groupsChips.addView(new View(activity));
        assertTrue(activity.groupsChips.getChildCount() > 0);

        try (MockedStatic<Utils> utils = Mockito.mockStatic(Utils.class)) {
            utils.when(() -> Utils.needsDarkForeground(Mockito.anyInt())).thenReturn(true);

            // Act
            activity.onResume();
        }

        // Assert
        assertTrue(activity.thumbnailEditIcon.getBackground() instanceof ColorDrawable);
        int bg = ((ColorDrawable) activity.thumbnailEditIcon.getBackground()).getColor();
        assertEquals(Color.BLACK, bg);
        assertNotNull(activity.thumbnailEditIcon.getColorFilter());
    }

    /**
     * Verifies that {@code ChooseImage::onClick} generate the correct dialog
     * when the view id is set to {@code frontImageHolder}
     * <p>
     * Test setup: view id is set to be frontImageHolder and simulated through robolectric shadow
     * to see the dialog
     * Expected outcome: Dialog exsists and has frontImage title
     * </p>
     */
    @Test
    public void onClick_FrontImageHolder_ShowDialogCorrecly() {
        activity = controller.resume().get();
        View v = activity.findViewById(R.id.frontImageHolder);
        v.performClick();

        shadowOf(Looper.getMainLooper()).runToEndOfTasks(); //simulate Android to show the dialog
        Dialog d = ShadowDialog.getLatestDialog();
        String expected = activity.getString(R.string.setFrontImage);
        TextView titleView = d.findViewById(
            androidx.appcompat.R.id.alertTitle
        );

        assertNotNull("No dialog shown", d);
        assertEquals(titleView.getText().toString(), expected);
    }

    /**
     * Verifies that {@code ChooseImage::onClick} generate the correct dialog
     * when the view id is set to {@code backImageHolder}
     * <p>
     * Test setup: view id is set to be backImageHolder and simulated through robolectric shadow
     * to see the dialog
     * Expected outcome: Dialog exsists and has backImage title
     * </p>
     */
    @Test
    public void onClick_backImageHolder_ShowDialogCorrecly() {
        activity = controller.resume().get();
        View v = activity.findViewById(R.id.backImageHolder);
        v.performClick();

        shadowOf(Looper.getMainLooper()).runToEndOfTasks(); //simulate Android to show the dialog
        Dialog d = ShadowDialog.getLatestDialog();
        String expected = activity.getString(R.string.setBackImage);
        TextView titleView = d.findViewById(
            androidx.appcompat.R.id.alertTitle
        );

        assertNotNull("No dialog shown", d);
        assertEquals(titleView.getText().toString(), expected);
    }

    /**
     * Verifies that {@code ChooseImage::onClick} generate the correct dialog
     * when the view id is set to {@code thumbnail}
     * <p>
     * Test setup: view id is set to be thumbnail and simulated through robolectric shadow
     * to see the dialog
     * Expected outcome: Dialog exsists and has thumbnail title
     * </p>
     */
    @Test
    public void onClick_thumbnail_ShowDialogCorrecly() {
        activity = controller.resume().get();
        View v = activity.findViewById(R.id.thumbnail);
        v.performClick();

        shadowOf(Looper.getMainLooper()).runToEndOfTasks(); //simulate Android to show the dialog
        Dialog d = ShadowDialog.getLatestDialog();
        String expected = activity.getString(R.string.setIcon);
        TextView titleView = d.findViewById(
            androidx.appcompat.R.id.alertTitle
        );

        assertNotNull("No dialog shown", d);
        assertEquals(titleView.getText().toString(), expected);
    }

    /**
     * Verifies that {@code ChooseImage::onClick} throw IllegalArgumentException 
     * when the view id is not frontImage, backImage, or thumbnail
     * <p>
     * Test setup: view id is set to be -1
     * Expected outcome: throw IllegalArgumentException
     * </p>
     */
    @Test(expected = IllegalArgumentException.class)
    public void onClick_InvalidImageId_ThrowsException() {
        activity = controller.resume().get();
        View v = new View(activity);
        v.setId(-1); 

        listener.onClick(v);
    }
}
