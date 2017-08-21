package com.hopding.pdflib.factories;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.facebook.react.bridge.NoSuchKeyException;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;

/**
 * Create a PDPage object and applies actions described in JSON
 * to it, such as drawing text or images. The PDPage object can
 * be created anew, or from an existing document.
 */
class PDPageFactory {
    protected PDDocument document;
    protected PDPage page;
    protected PDPageContentStream stream;

    private PDPageFactory(PDDocument document, PDPage page) throws IOException {
        this.document = document;
        this.page     = page;
        this.stream   = new PDPageContentStream(document, page);
    }

            /* ----- Factory methods ----- */
    protected static PDPage create(PDDocument document, ReadableMap pageActions) throws IOException {
        PDPage page = new PDPage();
        PDPageFactory factory = new PDPageFactory(document, page);

        factory.setMediaBox(pageActions.getMap("mediaBox"));
        factory.applyActions(pageActions);
        factory.stream.close();
        return page;
    }

    protected static PDPage modify(PDDocument document, ReadableMap pageActions) throws IOException {
        int pageIndex = pageActions.getInt("pageIndex");
        PDPage page   = document.getPage(pageIndex);
        PDPageFactory factory = new PDPageFactory(document, page);

        factory.applyActions(pageActions);
        factory.stream.close();
        return page;
    }

            /* ----- Page actions (based on JSON structures sent over bridge) ----- */
    private void applyActions(ReadableMap pageActions) throws IOException {
        ReadableArray actions = pageActions.getArray("actions");
        for(int i = 0; i < actions.size(); i++) {
            ReadableMap action = actions.getMap(i);
            String type = action.getString("type");

            if (type.equals("text"))
                this.drawText(action);
            else if (type.equals("rectangle"))
                this.drawRectangle(action);
            else if (type.equals("image"))
                this.drawImage(action);
        }
    }

    private void setMediaBox(ReadableMap dimensions) {
        Integer[] coords = getCoords(dimensions, true);
        Integer[] dims   = getDims(dimensions, true);
        page.setMediaBox(new PDRectangle(coords[0], coords[1], dims[0], dims[1]));
    }

    private void drawText(ReadableMap textActions) throws NoSuchKeyException, IOException {
        String value = textActions.getString("value");
        int fontSize = textActions.getInt("fontSize");

        Integer[] coords = getCoords(textActions.getMap("position"), true);
        int[] rgbColor   = hexStringToRGB(textActions.getString("color"));

        stream.beginText();
        stream.setNonStrokingColor(rgbColor[0], rgbColor[1], rgbColor[2]);
        stream.setFont(PDType1Font.TIMES_ROMAN, fontSize);
        stream.newLineAtOffset(coords[0], coords[1]);
        stream.showText(value);
        stream.endText();
    }

    private void drawRectangle(ReadableMap rectActions) throws NoSuchKeyException, IOException {
        Integer[] coords = getCoords(rectActions, true);
        Integer[] dims   = getDims(rectActions, true);
        int[] rgbColor   = hexStringToRGB(rectActions.getString("color"));

        stream.addRect(coords[0], coords[1], dims[0], dims[1]);
        stream.setNonStrokingColor(rgbColor[0], rgbColor[1], rgbColor[2]);
        stream.fill();
    }

    private void drawImage(ReadableMap imageActions) throws NoSuchKeyException, IOException {
        String imageType = imageActions.getString("imageType");
        String imagePath = imageActions.getString("imagePath");
        Integer[] coords = getCoords(imageActions, true);
        Integer[] dims   = getDims(imageActions, false);

        if (imageType.equals("jpg")) {
            Bitmap bmpImage = BitmapFactory.decodeFile(imagePath);
            PDImageXObject image = JPEGFactory.createFromImage(document, bmpImage);
            if (dims[0] != null && dims[1] != null)
                stream.drawImage(image, coords[0], coords[1], dims[0], dims[1]);
            else
                stream.drawImage(image, coords[0], coords[1]);
        }
    }

            /* ----- Static utilities ----- */
    private static Integer[] getDims(ReadableMap dimsMap, boolean required) {
        return getIntegerKeyPair(dimsMap, "width", "height", required);
    }

    private static Integer[] getCoords(ReadableMap coordsMap, boolean required) {
        return getIntegerKeyPair(coordsMap, "x", "y", required);
    }

    private static Integer[] getIntegerKeyPair(ReadableMap map, String key1, String key2, boolean required) {
        Integer val1 = null;
        Integer val2 = null;
        try {
            val1 = map.getInt(key1);
            val2 = map.getInt(key2);
        } catch (NoSuchKeyException e) {
            if (required) throw e;
        }
        return new Integer[] { val1, val2 };
    }

    // We get a color as a hex string, e.g. "#F0F0F0" - so parse into RGB vals
    private static int[] hexStringToRGB(String hexString) {
        int colorR = Integer.valueOf( hexString.substring( 1, 3 ), 16 );
        int colorG = Integer.valueOf( hexString.substring( 3, 5 ), 16 );
        int colorB = Integer.valueOf( hexString.substring( 5, 7 ), 16 );
        return new int[] { colorR, colorG, colorB };
    }
}