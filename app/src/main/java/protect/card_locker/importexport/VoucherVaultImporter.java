package protect.card_locker.importexport;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import protect.card_locker.CatimaBarcode;
import protect.card_locker.DBHelper;
import protect.card_locker.FormatException;
import protect.card_locker.LoyaltyCard;
import protect.card_locker.Utils;
import protect.card_locker.coverage.CoverageTool;

/**
 * Class for importing a database from CSV (Comma Separate Values)
 * formatted data.
 * <p>
 * The database's loyalty cards are expected to appear in the CSV data.
 * A header is expected for the each table showing the names of the columns.
 */
public class VoucherVaultImporter implements Importer {
    public static class ImportedData {
        public final List<LoyaltyCard> cards;

        ImportedData(final List<LoyaltyCard> cards) {
            this.cards = cards;
        }
    }

    public void importData(Context context, SQLiteDatabase database, File inputFile, char[] password) throws IOException, FormatException, JSONException, ParseException {
        InputStream input = new FileInputStream(inputFile);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        JSONArray jsonArray = new JSONArray(sb.toString());

        bufferedReader.close();
        input.close();

        ImportedData importedData = importJSON(jsonArray);
        saveAndDeduplicate(database, importedData);
    }

    public ImportedData importJSON(JSONArray jsonArray) throws FormatException, JSONException, ParseException {
        ImportedData importedData = new ImportedData(new ArrayList<>());

        // See https://github.com/tim-smart/vouchervault/issues/4#issuecomment-788226503 for more info
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonCard = jsonArray.getJSONObject(i);

            String store = jsonCard.getString("description");

            Date expiry = null;
            if (!jsonCard.isNull("expires")) {
                CoverageTool.setFunc4Flag(0);
                @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                expiry = dateFormat.parse(jsonCard.getString("expires"));
            }
            else{
                CoverageTool.setFunc4Flag(1);
            }

            BigDecimal balance = new BigDecimal("0");
            if (jsonCard.has("balanceMilliunits")) {
                CoverageTool.setFunc4Flag(2);

                if (!jsonCard.isNull("balanceMilliunits")) {
                    CoverageTool.setFunc4Flag(3);
                    balance = new BigDecimal(String.valueOf(jsonCard.getInt("balanceMilliunits") / 1000.0));
                }
                else{
                    CoverageTool.setFunc4Flag(4);
                }
            } else if (!jsonCard.isNull("balance")) {
                CoverageTool.setFunc4Flag(5);
                balance = new BigDecimal(String.valueOf(jsonCard.getDouble("balance")));
            }
            else{
                CoverageTool.setFunc4Flag(6);
            }

            Currency balanceType = Currency.getInstance("USD");

            String cardId = jsonCard.getString("code");

            CatimaBarcode barcodeType = null;

            String codeTypeFromJSON = jsonCard.getString("codeType");
            switch (codeTypeFromJSON) {
                case "CODE128":
                    CoverageTool.setFunc4Flag(7);
                    barcodeType = CatimaBarcode.fromBarcode(BarcodeFormat.CODE_128);
                    break;
                case "CODE39":
                    CoverageTool.setFunc4Flag(8);
                    barcodeType = CatimaBarcode.fromBarcode(BarcodeFormat.CODE_39);
                    break;
                case "EAN13":
                    CoverageTool.setFunc4Flag(9);
                    barcodeType = CatimaBarcode.fromBarcode(BarcodeFormat.EAN_13);
                    break;
                case "PDF417":
                    CoverageTool.setFunc4Flag(10);
                    barcodeType = CatimaBarcode.fromBarcode(BarcodeFormat.PDF_417);
                    break;
                case "QR":
                    CoverageTool.setFunc4Flag(11);
                    barcodeType = CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE);
                    break;
                case "TEXT":
                    CoverageTool.setFunc4Flag(12);
                    break;
                default:
                    CoverageTool.setFunc4Flag(13);
                    throw new FormatException("Unknown barcode type found: " + codeTypeFromJSON);
            }

            int headerColor;

            String colorFromJSON = jsonCard.getString("color");
            switch (colorFromJSON) {
                case "GREY":
                    CoverageTool.setFunc4Flag(14);
                    headerColor = Color.GRAY;
                    break;
                case "BLUE":
                    CoverageTool.setFunc4Flag(15);
                    headerColor = Color.BLUE;
                    break;
                case "GREEN":
                    CoverageTool.setFunc4Flag(16);
                    headerColor = Color.GREEN;
                    break;
                case "ORANGE":
                    CoverageTool.setFunc4Flag(17);
                    headerColor = Color.rgb(255, 165, 0);
                    break;
                case "PURPLE":
                    CoverageTool.setFunc4Flag(18);
                    headerColor = Color.rgb(128, 0, 128);
                    break;
                case "RED":
                    CoverageTool.setFunc4Flag(19);
                    headerColor = Color.RED;
                    break;
                case "YELLOW":
                    CoverageTool.setFunc4Flag(20);
                    headerColor = Color.YELLOW;
                    break;
                default:
                    CoverageTool.setFunc4Flag(21);
                    throw new FormatException("Unknown colour type found: " + colorFromJSON);
            }

            // use -1 for the ID, it will be ignored when inserting the card into the DB
            importedData.cards.add(new LoyaltyCard(
                    -1,
                    store,
                    "",
                    null,
                    expiry,
                    balance,
                    balanceType,
                    cardId,
                    null,
                    barcodeType,
                    StandardCharsets.ISO_8859_1,
                    headerColor,
                    0,
                    Utils.getUnixTime(),
                    DBHelper.DEFAULT_ZOOM_LEVEL,
                    DBHelper.DEFAULT_ZOOM_LEVEL_WIDTH,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }

        CoverageTool.setFunc4Flag(22);
        return importedData;
    }

    public void saveAndDeduplicate(SQLiteDatabase database, final ImportedData data) {
        // This format does not have IDs that can cause conflicts
        // Proper deduplication for all formats will be implemented later
        for (LoyaltyCard card : data.cards) {
            // Do not use card.id which is set to -1
            DBHelper.insertLoyaltyCard(database, card.store, card.note, card.validFrom, card.expiry, card.balance, card.balanceType,
                    card.cardId, card.barcodeId, card.barcodeType, card.barcodeEncoding, card.headerColor, card.starStatus, card.lastUsed, card.archiveStatus);
        }
    }
}