package com.example.smartairsetup;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PDFGenerator {

    private final Context context;
    private final FirebaseFirestore db;

    public PDFGenerator(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    public void generatePdf(String parentID, String childID, long startTimestamp, long endTimestamp) {
        new Thread(() -> {
            try {
                // Child info
                DocumentSnapshot childDoc = Tasks.await(
                        db.collection("users")
                                .document(parentID)
                                .collection("children")
                                .document(childID)
                                .get()
                );
                if (!childDoc.exists()) throw new IOException("Child not found");

                String childName = childDoc.getString("name");
                String childDOB = childDoc.getString("dateOfBirth");

                // Parent info
                DocumentSnapshot parentDoc = Tasks.await(
                        db.collection("users")
                                .document(parentID)
                                .get()
                );

                // Remove parent name field
                String parentEmail = parentDoc.exists() ? parentDoc.getString("email") : "";

                // PEF logs
                QuerySnapshot pefSnapshot = Tasks.await(
                        db.collection("users")
                                .document(parentID)
                                .collection("children")
                                .document(childID)
                                .collection("PEF")
                                .document("logs")
                                .collection("daily")
                                .get()
                );

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                StringBuilder zoneTable = new StringBuilder();
                zoneTable.append("Date       |  PEF  | Zone\n");
                zoneTable.append("--------------------------\n");

                int greenCount = 0, yellowCount = 0, redCount = 0;

                for (QueryDocumentSnapshot doc : pefSnapshot) {
                    Long timestamp = doc.getLong("timestamp");
                    if (timestamp == null || timestamp < startTimestamp || timestamp > endTimestamp) continue;

                    String dateString = doc.getString("date");
                    Long dailyPEF = doc.getLong("dailyPEF");
                    String zone = doc.getString("zone");
                    if (dateString == null || zone == null) continue;

                    zoneTable.append(dateString)
                            .append(" | ").append(dailyPEF != null ? dailyPEF : "---")
                            .append(" | ").append(zone)
                            .append("\n");

                    switch (zone.toUpperCase()) {
                        case "GREEN": greenCount++; break;
                        case "YELLOW": yellowCount++; break;
                        case "RED": redCount++; break;
                    }
                }

                int totalCount = greenCount + yellowCount + redCount;

                // TRIAGE
                QuerySnapshot triageSnapshot = Tasks.await(
                        db.collection("users")
                                .document(parentID)
                                .collection("children")
                                .document(childID)
                                .collection("triage")
                                .document("logs")
                                .collection("entries")
                                .get()
                );

                StringBuilder triageEvents = new StringBuilder();
                for (QueryDocumentSnapshot doc : triageSnapshot) {
                    Long timestamp = doc.getLong("timestamp");
                    if (timestamp == null || timestamp < startTimestamp || timestamp > endTimestamp) continue;

                    String zone = doc.getString("triage-zone");
                    String message = doc.getString("message-triage");
                    if (zone != null && !"Green".equalsIgnoreCase(zone)) {
                        String dateString = sdf.format(new Date(timestamp));
                        triageEvents.append(dateString)
                                .append(" | Zone: ").append(zone)
                                .append(" | Message: ").append(message != null ? message : "")
                                .append("\n");
                    }
                }

                createPdf(
                        childName,
                        childDOB,
                        parentEmail,
                        zoneTable.toString(),
                        triageEvents.toString(),
                        greenCount,
                        yellowCount,
                        redCount,
                        totalCount,
                        startTimestamp,
                        endTimestamp
                );

            } catch (Exception e) {
                e.printStackTrace();
                ((android.app.Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void createPdf(String childName, String dob,
                           String parentEmail,
                           String zoneTable, String triageEvents,
                           int greenCount, int yellowCount, int redCount, int totalCount,
                           long startTimestamp, long endTimestamp)
            throws IOException, DocumentException {

        Document document = new Document();
        OutputStream output;
        File pdfFile = null;
        Uri pdfUri = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "ChildReport.pdf");
            values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            pdfUri = context.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            output = context.getContentResolver().openOutputStream(pdfUri);
        } else {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) dir.mkdirs();
            pdfFile = new File(dir, "ChildReport.pdf");
            output = new FileOutputStream(pdfFile);
        }

        PdfWriter writer = PdfWriter.getInstance(document, output);
        document.open();

        // Font
        Font font;
        try {
            InputStream is = context.getAssets().open("fonts/nunito_regular.ttf");
            byte[] fontBytes = new byte[is.available()];
            is.read(fontBytes);
            is.close();
            BaseFont bf = BaseFont.createFont("nunito_regular.ttf", BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED, true, fontBytes, null);
            font = new Font(bf, 16);
        } catch (Exception e) {
            font = new Font(Font.FontFamily.HELVETICA, 16);
        }

        // Report title + period
        SimpleDateFormat sdfDisplay = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        String startDateStr = sdfDisplay.format(new Date(startTimestamp));
        String endDateStr = sdfDisplay.format(new Date(endTimestamp));

        document.add(new Paragraph("SMART AIR – Provider Medical Report", font));
        document.add(new Paragraph("Report Period: " + startDateStr + " – " + endDateStr, font));
        document.add(new Paragraph("\n"));

        // Child info
        document.add(new Paragraph("1. Child Information", font));
        document.add(new Paragraph("Name: " + childName));
        document.add(new Paragraph("Date of Birth: " + dob));
        document.add(new Paragraph("\n"));

        // Parent info (only email)
        document.add(new Paragraph("2. Parent Information", font));
        document.add(new Paragraph("Email: " + parentEmail));
        document.add(new Paragraph("\n"));

        // Zone Distribution
        document.add(new Paragraph("3. Zone Distribution (PEF)", font));
        document.add(new Paragraph(zoneTable));
        document.add(new Paragraph("\n"));

        // Triage events
        document.add(new Paragraph("4. Noticeable Triage Events", font));
        document.add(new Paragraph(triageEvents.isEmpty() ? "No incidents recorded" : triageEvents));
        document.add(new Paragraph("\n"));

        // Pie chart
        document.add(new Paragraph("5. Zone Distribution Pie Chart", font));

        PdfContentByte canvas = writer.getDirectContent();
        float centerX = 300;
        float centerY = writer.getVerticalPosition(true) - 100;
        float radius = 80;

        BaseColor darkGreen = new BaseColor(0, 100, 0);
        BaseColor darkYellow = new BaseColor(204, 204, 0);
        BaseColor darkRed = new BaseColor(139, 0, 0);
        BaseColor darkGray = new BaseColor(80, 80, 80);

        int totalDays = (int) ((endTimestamp - startTimestamp) / (1000L * 60 * 60 * 24)) + 1;
        float greenFraction = greenCount / (float) totalDays;
        float yellowFraction = yellowCount / (float) totalDays;
        float redFraction = redCount / (float) totalDays;

        float greenAngle = greenFraction * 360f;
        float yellowAngle = yellowFraction * 360f;
        float redAngle = redFraction * 360f;
        float greyAngle = 360f - (greenAngle + yellowAngle + redAngle);

        float startAngle = -90;

        drawPieSlice(canvas, centerX, centerY, radius, startAngle, greenAngle, darkGreen);
        drawPieSlice(canvas, centerX, centerY, radius, startAngle + greenAngle, yellowAngle, darkYellow);
        drawPieSlice(canvas, centerX, centerY, radius, startAngle + greenAngle + yellowAngle, redAngle, darkRed);
        if (greyAngle > 0.01f)
            drawPieSlice(canvas, centerX, centerY, radius, startAngle + greenAngle + yellowAngle + redAngle, greyAngle, darkGray);

        float legendX = centerX + radius + 40;
        float legendY = centerY + radius - 10;

        drawLegend(canvas, legendX, legendY, 12, 18, darkGreen, "Green");
        drawLegend(canvas, legendX, legendY - 18, 12, 18, darkYellow, "Yellow");
        drawLegend(canvas, legendX, legendY - 36, 12, 18, darkRed, "Red");
        drawLegend(canvas, legendX, legendY - 54, 12, 18, darkGray, "Invalid");

        document.close();
        output.close();

        String msg = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? pdfUri.toString()
                : pdfFile.getAbsolutePath();

        ((android.app.Activity) context).runOnUiThread(() ->
                Toast.makeText(context, "PDF saved: " + msg, Toast.LENGTH_LONG).show()
        );
    }

    private void drawPieSlice(PdfContentByte canvas, float centerX, float centerY,
                              float radius, float startAngle, float sweepAngle, BaseColor color) {
        if (sweepAngle <= 0.1f) return;

        canvas.setColorFill(color);
        canvas.moveTo(centerX, centerY);

        canvas.arc(centerX - radius, centerY - radius,
                centerX + radius, centerY + radius,
                startAngle, sweepAngle);

        canvas.lineTo(centerX, centerY);
        canvas.closePathFillStroke();
    }

    private void drawLegend(PdfContentByte canvas, float x, float y,
                            float boxSize, float spacing, BaseColor color, String label) {
        canvas.setColorFill(color);
        canvas.rectangle(x, y - boxSize, boxSize, boxSize);
        canvas.fill();

        canvas.beginText();
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
            canvas.setFontAndSize(bf, 12);
        } catch (Exception ignored) {}
        canvas.moveText(x + boxSize + 5, y - boxSize + 2);
        canvas.showText(label);
        canvas.endText();
    }
}
