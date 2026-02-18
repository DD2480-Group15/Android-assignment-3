package protect.card_locker;

import android.os.Build;
import android.view.View;
import android.os.Looper;
import android.app.Dialog;
import android.widget.TextView;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowDialog;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;


import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;
import protect.card_locker.viewmodels.LoyaltyCardEditActivityViewModel;

@RunWith(RobolectricTestRunner.class)
public class LoyaltyCardEditActivityTest {

    private LoyaltyCardEditActivity activity;
    private LoyaltyCardEditActivity.ChooseCardImage listener;
    private LoyaltyCardEditActivityViewModel mockViewModel;
    private LoyaltyCard mockLoyaltyCard;

    @Before
    public void setUp() {
        mockViewModel = Mockito.mock(LoyaltyCardEditActivityViewModel.class);
        mockLoyaltyCard = Mockito.mock(LoyaltyCard.class);
        Mockito.when(mockViewModel.getLoyaltyCard()).thenReturn(mockLoyaltyCard);

        activity = Robolectric.buildActivity(LoyaltyCardEditActivity.class)
            .setup()
            .get();

        listener = activity.new ChooseCardImage();
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
        View v = new View(activity);
        v.setId(-1); 

        listener.onClick(v);
    }
}
