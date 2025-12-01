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

import java.util.Calendar;
import java.util.Date;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

                // Sharing flags
                boolean shareRescueLogs = Boolean.TRUE.equals(childDoc.getBoolean("shareRescueLogs"));
                boolean shareControllerSummary = Boolean.TRUE.equals(childDoc.getBoolean("shareControllerSummary"));
                boolean shareSymptoms = Boolean.TRUE.equals(childDoc.getBoolean("shareSymptoms"));
                boolean sharePEF = Boolean.TRUE.equals(childDoc.getBoolean("sharePEF"));
                boolean shareTriageIncidents = Boolean.TRUE.equals(childDoc.getBoolean("shareTriageIncidents"));
                boolean shareSummaryCharts = Boolean.TRUE.equals(childDoc.getBoolean("shareSummaryCharts"));

                // Lists for time-series chart (PEF over time)
                List<String> pefDates = new ArrayList<>();
                List<Integer> pefValues = new ArrayList<>();

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

                    // Collect data for time-series chart
                    if (dailyPEF != null) {
                        pefDates.add(dateString);
                        pefValues.add(dailyPEF.intValue());
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

                    String zone = doc.getString("zone");
                    String message = doc.getString("message-triage");

                    // Additional flags
                    Boolean blueLipsNails = doc.getBoolean("blueLipsNails");
                    Boolean cantSpeakFullSentences = doc.getBoolean("cantSpeakFullSentences");
                    Boolean chestRetractions = doc.getBoolean("chestRetractions");

                    Long dailyPEF = doc.getLong("dailyPEF"); // optional, may be null

                    if (zone != null && !"Green".equalsIgnoreCase(zone)) {
                        String dateString = sdf.format(new Date(timestamp));

                        triageEvents.append(dateString)
                                .append(" | Guidance Shown: ").append(zone)
                                .append(" | Message: ").append(message != null ? message : "");

                        // Append additional flags
                        if (blueLipsNails != null) triageEvents.append(" | blueLipsNails: ").append(blueLipsNails);
                        if (cantSpeakFullSentences != null) triageEvents.append(" | cantSpeakFullSentences: ").append(cantSpeakFullSentences);
                        if (chestRetractions != null) triageEvents.append(" | chestRetractions: ").append(chestRetractions);

                        // Append dailyPEF if available
                        if (dailyPEF != null) triageEvents.append(" | Optional PEF: ").append(dailyPEF);

                        triageEvents.append("\n");
                    }
                }

                String rescueControllerSummary = buildRescueControllerSummary(
                        parentID,
                        childID,
                        startTimestamp,
                        endTimestamp
                );

                // --- NEW: Symptom burden summary (problem days) ---
                String symptomBurdenSummary = buildSymptomBurdenSummary(
                        parentID,
                        childID,
                        startTimestamp,
                        endTimestamp
                );

                createPdf(
                        childName,
                        childDOB,
                        parentEmail,
                        zoneTable.toString(),
                        triageEvents.toString(),
                        rescueControllerSummary,
                        symptomBurdenSummary,
                        greenCount,
                        yellowCount,
                        redCount,
                        totalCount,
                        startTimestamp,
                        endTimestamp,
                        sharePEF,
                        shareTriageIncidents,
                        shareSummaryCharts,
                        shareRescueLogs,
                        shareControllerSummary,
                        shareSymptoms,
                        pefDates,
                        pefValues
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
                           String rescueControllerSummary,
                           String symptomBurdenSummary,
                           int greenCount, int yellowCount, int redCount, int totalCount,
                           long startTimestamp, long endTimestamp,
                           boolean sharePEF,
                           boolean shareTriageIncidents,
                           boolean shareSummaryCharts,
                           boolean shareRescueLogs,
                           boolean shareControllerSummary,
                           boolean shareSymptoms,
                           List<String> pefDates,
                           List<Integer> pefValues)
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

        // 3. Rescue Frequency & Controller Adherence
        document.add(new Paragraph("3. Rescue Frequency & Controller Adherence", font));
        if (shareRescueLogs || shareControllerSummary) {
            if (rescueControllerSummary == null || rescueControllerSummary.trim().isEmpty()) {
                document.add(new Paragraph("No rescue/controller data available for this period."));
            } else {
                document.add(new Paragraph(rescueControllerSummary));
            }
        } else {
            document.add(new Paragraph("Parent has chosen not to share rescue/controller information with the provider."));
        }
        document.add(new Paragraph("\n"));

        // 4. Symptom Burden (Problem Days)
        document.add(new Paragraph("4. Symptom Burden (Problem Days)", font));
        if (shareSymptoms) {
            if (symptomBurdenSummary == null || symptomBurdenSummary.trim().isEmpty()) {
                document.add(new Paragraph("No symptom check-ins recorded for this period."));
            } else {
                document.add(new Paragraph(symptomBurdenSummary));
            }
        } else {
            document.add(new Paragraph("Parent has chosen not to share symptom history with the provider."));
        }
        document.add(new Paragraph("\n"));

        // 5. Zone Distribution (PEF)
        document.add(new Paragraph("5. Zone Distribution (PEF)", font));
        if (sharePEF) {
            document.add(new Paragraph(zoneTable));
        } else {
            document.add(new Paragraph("Parent has chosen not to share PEF-based zone history with the provider."));
        }
        document.add(new Paragraph("\n"));

        // 6. Noticeable Triage Events
        document.add(new Paragraph("6. Noticeable Triage Events", font));
        if (shareTriageIncidents) {
            if (triageEvents == null || triageEvents.isEmpty()) {
                document.add(new Paragraph("No incidents recorded."));
            } else {
                document.add(new Paragraph(triageEvents));
            }
        } else {
            document.add(new Paragraph("Parent has chosen not to share triage incident details with the provider."));
        }
        document.add(new Paragraph("\n"));

        // Pie chart
        document.add(new Paragraph("7. Zone Distribution Pie Chart (PEF)", font));
        if (shareSummaryCharts) {

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
            if (greyAngle > 0.01f) {
                drawPieSlice(canvas, centerX, centerY, radius,
                        startAngle + greenAngle + yellowAngle + redAngle, greyAngle, darkGray);
            }

            float legendX = centerX + radius + 40;
            float legendY = centerY + radius - 10;

            drawLegend(canvas, legendX, legendY, 12, 18, darkGreen, "Green");
            drawLegend(canvas, legendX, legendY - 18, 12, 18, darkYellow, "Yellow");
            drawLegend(canvas, legendX, legendY - 36, 12, 18, darkRed, "Red");
            drawLegend(canvas, legendX, legendY - 54, 12, 18, darkGray, "Invalid");
            document.add(new Paragraph("\n"));
        }

        if (shareSummaryCharts) {
            document.newPage();
        }
        // Time-series chart
        document.add(new Paragraph("8. Time-series chart (PEF over time)", font));
        if (sharePEF && pefValues != null && !pefValues.isEmpty()) {
            PdfContentByte chartCanvas = writer.getDirectContent();

            // Chart area (just under the title on this page)
            float chartLeft = 60f;
            float chartWidth = 400f;
            float chartHeight = 180f;

            // Place the chart right below the current text position
            float topY = writer.getVerticalPosition(true) - 20f; // small padding under the title
            float chartBottom = topY - chartHeight;

            // Safety: do not go too close to the bottom margin
            if (chartBottom < 60f) {
                chartBottom = 60f;
            }

            // ----- Grid background -----
            chartCanvas.saveState();
            chartCanvas.setLineWidth(0.2f);
            chartCanvas.setColorStroke(new BaseColor(220, 220, 220)); // light gray

            int gridRows = 6;
            int gridCols = 8;
            float rowHeight = chartHeight / (float) gridRows;
            float colWidth = chartWidth / (float) gridCols;

            // Horizontal grid lines
            for (int i = 0; i <= gridRows; i++) {
                float y = chartBottom + i * rowHeight;
                chartCanvas.moveTo(chartLeft, y);
                chartCanvas.lineTo(chartLeft + chartWidth, y);
            }

            // Vertical grid lines
            for (int j = 0; j <= gridCols; j++) {
                float x = chartLeft + j * colWidth;
                chartCanvas.moveTo(x, chartBottom);
                chartCanvas.lineTo(x, chartBottom + chartHeight);
            }

            chartCanvas.stroke();
            chartCanvas.restoreState();

            // ----- Axes -----
            chartCanvas.setColorStroke(BaseColor.BLACK);
            chartCanvas.setLineWidth(1f);

            // Y axis
            chartCanvas.moveTo(chartLeft, chartBottom);
            chartCanvas.lineTo(chartLeft, chartBottom + chartHeight);

            // X axis
            chartCanvas.moveTo(chartLeft, chartBottom);
            chartCanvas.lineTo(chartLeft + chartWidth, chartBottom);

            chartCanvas.stroke();

            // ----- Compute min / max PEF -----
            int minPEF = pefValues.get(0);
            int maxPEF = pefValues.get(0);
            for (int i = 1; i < pefValues.size(); i++) {
                int value = pefValues.get(i);
                if (value < minPEF) {
                    minPEF = value;
                }
                if (value > maxPEF) {
                    maxPEF = value;
                }
            }
            if (maxPEF == minPEF) {
                maxPEF = minPEF + 1; // avoid division by zero
            }

            int n = pefValues.size();
            float xStep = 0f;
            if (n > 1) {
                xStep = chartWidth / (float) (n - 1);
            }

            // ----- Draw PEF line -----
            BaseColor lineColor = new BaseColor(0, 150, 255); // blue-ish
            chartCanvas.setColorStroke(lineColor);
            chartCanvas.setLineWidth(2.5f);

            for (int i = 0; i < n; i++) {
                int value = pefValues.get(i);

                float x;
                if (n > 1) {
                    x = chartLeft + xStep * i;
                } else {
                    x = chartLeft + chartWidth / 2f;
                }

                float y = chartBottom
                        + (value - minPEF) * chartHeight / (float) (maxPEF - minPEF);

                if (i == 0) {
                    chartCanvas.moveTo(x, y);
                } else {
                    chartCanvas.lineTo(x, y);
                }
            }
            chartCanvas.stroke();

            // ----- Optional: small circles on each point -----
            chartCanvas.setColorFill(lineColor);
            for (int i = 0; i < n; i++) {
                int value = pefValues.get(i);

                float x;
                if (n > 1) {
                    x = chartLeft + xStep * i;
                } else {
                    x = chartLeft + chartWidth / 2f;
                }

                float y = chartBottom
                        + (value - minPEF) * chartHeight / (float) (maxPEF - minPEF);

                float r = 2.5f;
                chartCanvas.circle(x, y, r);
                chartCanvas.fill();
            }

            // ----- Axis labels (PEF on left, dates on bottom) -----
            try {
                BaseFont axisFont = BaseFont.createFont(
                        BaseFont.HELVETICA,
                        BaseFont.WINANSI,
                        BaseFont.EMBEDDED
                );
                chartCanvas.beginText();
                chartCanvas.setFontAndSize(axisFont, 10f);

                // Y-axis labels: min, mid, max
                int midPEF = (minPEF + maxPEF) / 2;

                chartCanvas.showTextAligned(
                        Element.ALIGN_RIGHT,
                        String.valueOf(minPEF),
                        chartLeft - 5f,
                        chartBottom - 2f,
                        0f
                );
                chartCanvas.showTextAligned(
                        Element.ALIGN_RIGHT,
                        String.valueOf(midPEF),
                        chartLeft - 5f,
                        chartBottom + chartHeight / 2f,
                        0f
                );
                chartCanvas.showTextAligned(
                        Element.ALIGN_RIGHT,
                        String.valueOf(maxPEF),
                        chartLeft - 5f,
                        chartBottom + chartHeight - 2f,
                        0f
                );

                // X-axis labels: first, middle, last dates (MM-dd)
                String firstDate = pefDates.get(0);
                String lastDate = pefDates.get(pefDates.size() - 1);
                String firstLabel = firstDate.length() >= 5 ? firstDate.substring(5) : firstDate;
                String lastLabel = lastDate.length() >= 5 ? lastDate.substring(5) : lastDate;

                chartCanvas.showTextAligned(
                        Element.ALIGN_CENTER,
                        firstLabel,
                        chartLeft,
                        chartBottom - 14f,
                        0f
                );
                chartCanvas.showTextAligned(
                        Element.ALIGN_CENTER,
                        lastLabel,
                        chartLeft + chartWidth,
                        chartBottom - 14f,
                        0f
                );

                if (pefDates.size() > 2) {
                    int midIndex = pefDates.size() / 2;
                    String midDate = pefDates.get(midIndex);
                    String midLabel = midDate.length() >= 5 ? midDate.substring(5) : midDate;
                    float midX = chartLeft + chartWidth / 2f;

                    chartCanvas.showTextAligned(
                            Element.ALIGN_CENTER,
                            midLabel,
                            midX,
                            chartBottom - 14f,
                            0f
                    );
                }
                // Axis titles
                chartCanvas.showTextAligned(
                        Element.ALIGN_LEFT,
                        "PEF Values",
                        chartLeft - 40f,                  // left side
                        chartBottom + chartHeight + 10f,  // above top
                        0f
                );

                chartCanvas.showTextAligned(
                        Element.ALIGN_RIGHT,
                        "Date",
                        chartLeft + chartWidth,           // right edge of chart
                        chartBottom - 28f,                // below x-axis labels
                        0f
                );
                chartCanvas.endText();
            } catch (Exception axisError) {
                // Ignore axis label errors
            }

        } else if (sharePEF) {
            document.add(new Paragraph("Not enough PEF data to draw time-series chart."));
        } else {
            document.add(new Paragraph("Parent has chosen not to share PEF time-series data."));
        }

        //Closing
        document.close();
        output.close();

        String msg = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? pdfUri.toString()
                : pdfFile.getAbsolutePath();

        ((android.app.Activity) context).runOnUiThread(() ->
                Toast.makeText(context, "PDF saved: " + msg, Toast.LENGTH_LONG).show()
        );
    }

    // Helper to compute rescue frequency + controller adherence summary text
    private String buildRescueControllerSummary(String parentID,
                                                String childID,
                                                long startTimestamp,
                                                long endTimestamp) throws Exception {

        // 1. Load controller schedule
        DocumentSnapshot scheduleDoc = Tasks.await(
                db.collection("users")
                        .document(parentID)
                        .collection("children")
                        .document(childID)
                        .collection("medicationSchedule")
                        .document("controller")
                        .get()
        );

        boolean mon = false, tue = false, wed = false, thu = false, fri = false, sat = false, sun = false;
        int dosesPerDay = 0;

        if (scheduleDoc.exists()) {
            mon = Boolean.TRUE.equals(scheduleDoc.getBoolean("mon"));
            tue = Boolean.TRUE.equals(scheduleDoc.getBoolean("tue"));
            wed = Boolean.TRUE.equals(scheduleDoc.getBoolean("wed"));
            thu = Boolean.TRUE.equals(scheduleDoc.getBoolean("thu"));
            fri = Boolean.TRUE.equals(scheduleDoc.getBoolean("fri"));
            sat = Boolean.TRUE.equals(scheduleDoc.getBoolean("sat"));
            sun = Boolean.TRUE.equals(scheduleDoc.getBoolean("sun"));

            Long dpd = scheduleDoc.getLong("dosesPerDay");
            if (dpd != null) {
                dosesPerDay = dpd.intValue();
            }
        }

        // 2. Controller logs within window
        QuerySnapshot controllerSnapshot = Tasks.await(
                db.collection("users")
                        .document(parentID)
                        .collection("children")
                        .document(childID)
                        .collection("medicationLogs_controller")
                        .get()
        );

        Set<String> controllerDays = new HashSet<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (QueryDocumentSnapshot doc : controllerSnapshot) {
            Long ts = doc.getLong("timestamp");
            if (ts == null || ts < startTimestamp || ts > endTimestamp) continue;

            String dateStr = doc.getString("date");
            if (dateStr == null) {
                dateStr = dateFormat.format(new Date(ts));
            }
            controllerDays.add(dateStr);
        }

        // 3. Rescue logs within window
        QuerySnapshot rescueSnapshot = Tasks.await(
                db.collection("users")
                        .document(parentID)
                        .collection("children")
                        .document(childID)
                        .collection("medicationLogs_rescue")
                        .get()
        );

        int totalRescueDoses = 0;
        Set<String> rescueDays = new HashSet<>();

        for (QueryDocumentSnapshot doc : rescueSnapshot) {
            Long ts = doc.getLong("timestamp");
            if (ts == null || ts < startTimestamp || ts > endTimestamp) continue;

            String dateStr = doc.getString("date");
            if (dateStr == null) {
                dateStr = dateFormat.format(new Date(ts));
            }

            Long doseCountLong = doc.getLong("doseCount");
            int doseCount = (doseCountLong != null) ? doseCountLong.intValue() : 0;
            if (doseCount <= 0) doseCount = 1;

            totalRescueDoses += doseCount;
            if (dateStr != null) {
                rescueDays.add(dateStr);
            }
        }

        // 4. Planned vs completed controller days
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startTimestamp);

        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(endTimestamp);

        int plannedDays = 0;
        int completedDays = 0;

        while (!cal.after(endCal)) {
            int dow = cal.get(Calendar.DAY_OF_WEEK);
            boolean scheduled = false;
            switch (dow) {
                case Calendar.MONDAY:    scheduled = mon; break;
                case Calendar.TUESDAY:   scheduled = tue; break;
                case Calendar.WEDNESDAY: scheduled = wed; break;
                case Calendar.THURSDAY:  scheduled = thu; break;
                case Calendar.FRIDAY:    scheduled = fri; break;
                case Calendar.SATURDAY:  scheduled = sat; break;
                case Calendar.SUNDAY:    scheduled = sun; break;
            }

            if (scheduled) {
                plannedDays++;
                String d = dateFormat.format(cal.getTime());
                if (controllerDays.contains(d)) {
                    completedDays++;
                }
            }

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        long daysInPeriod = 1L + (endTimestamp - startTimestamp) / (1000L * 60L * 60L * 24L);

        double adherencePct = -1.0;
        if (plannedDays > 0) {
            adherencePct = (completedDays * 100.0) / plannedDays;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("Controller schedule during this period:\n");
        if (scheduleDoc.exists()) {
            sb.append("• Doses per scheduled day: ").append(dosesPerDay).append("\n");
            sb.append("• Scheduled days of week: ");
            List<String> days = new ArrayList<>();
            if (mon) days.add("Mon");
            if (tue) days.add("Tue");
            if (wed) days.add("Wed");
            if (thu) days.add("Thu");
            if (fri) days.add("Fri");
            if (sat) days.add("Sat");
            if (sun) days.add("Sun");
            if (days.isEmpty()) days.add("None");
            sb.append(String.join(", ", days)).append("\n");
        } else {
            sb.append("• No controller schedule has been set for this child.\n");
        }

        sb.append("\nPlanned controller days in report window: ").append(plannedDays);
        if (plannedDays > 0) {
            sb.append("\nDays where controller was logged: ").append(completedDays);
            sb.append(String.format(Locale.US,
                    "\nController adherence: %.1f%% of planned days completed.",
                    adherencePct));
        } else {
            sb.append("\nCannot compute adherence (no schedule).");
        }

        sb.append("\n\nRescue inhaler usage in report window:");
        sb.append("\n• Days with any rescue use: ").append(rescueDays.size());
        sb.append("\n• Total rescue doses (puffs logged): ").append(totalRescueDoses);

        if (daysInPeriod > 0) {
            double perWeek = (totalRescueDoses * 7.0) / daysInPeriod;
            sb.append(String.format(Locale.US,
                    "\n• Approximate average rescue doses per week: %.1f",
                    perWeek));
        }

        return sb.toString();
    }

    // Helper to compute symptom burden summary (problem days)
    private String buildSymptomBurdenSummary(String parentID,
                                             String childID,
                                             long startTimestamp,
                                             long endTimestamp) throws Exception {

        QuerySnapshot checkinsSnapshot = Tasks.await(
                db.collection("users")
                        .document(parentID)
                        .collection("dailyCheckins")
                        .whereEqualTo("childId", childID)
                        .get()
        );

        if (checkinsSnapshot.isEmpty()) {
            return "";
        }

        Map<String, Integer> scorePerDate = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (QueryDocumentSnapshot doc : checkinsSnapshot) {
            Date createdAt = doc.getDate("createdAt");
            if (createdAt == null) continue;

            long ts = createdAt.getTime();
            if (ts < startTimestamp || ts > endTimestamp) continue;

            String dateStr = doc.getString("date");
            if (dateStr == null) {
                dateStr = dateFormat.format(createdAt);
            }

            String night = doc.getString("nightWaking");
            String activity = doc.getString("activityLimits");
            String cough = doc.getString("coughWheeze");

            int score = 0;
            if (isProblemSymptom(night)) score++;
            if (isProblemSymptom(activity)) score++;
            if (isProblemSymptom(cough)) score++;

            Integer existing = scorePerDate.get(dateStr);
            if (existing == null || score > existing) {
                scorePerDate.put(dateStr, score);
            }
        }

        if (scorePerDate.isEmpty()) {
            return "No symptom check-ins recorded for this period.";
        }

        int mildDays = 0, moderateDays = 0, severeDays = 0, totalProblemDays = 0;
        for (int score : scorePerDate.values()) {
            if (score <= 0) continue;
            totalProblemDays++;
            if (score == 1) mildDays++;
            else if (score == 2) moderateDays++;
            else severeDays++;
        }

        if (totalProblemDays == 0) {
            return "All recorded days were symptom-free (night waking, activity limits, cough/wheeze all OK).";
        }

        long daysInPeriod = 1L + (endTimestamp - startTimestamp) / (1000L * 60L * 60L * 24L);
        double pctProblem = (daysInPeriod > 0)
                ? (totalProblemDays * 100.0) / daysInPeriod
                : 0.0;

        StringBuilder sb = new StringBuilder();
        sb.append("Days with any symptom problems in this period: ")
                .append(totalProblemDays)
                .append(" day(s).");

        sb.append("\nBreakdown by maximum severity per day:");
        if (mildDays > 0) {
            sb.append("\n• Mild (1 problem area): ").append(mildDays).append(" day(s)");
        }
        if (moderateDays > 0) {
            sb.append("\n• Moderate (2 problem areas): ").append(moderateDays).append(" day(s)");
        }
        if (severeDays > 0) {
            sb.append("\n• Severe (3 problem areas): ").append(severeDays).append(" day(s)");
        }

        sb.append(String.format(Locale.US,
                "\n\nThis corresponds to symptom problems on approximately %.1f%% of days in the report window.",
                pctProblem));

        return sb.toString();
    }

    // Helper used by buildSymptomBurdenSummary (same logic as SymptomTrendActivity)
    private boolean isProblemSymptom(String value) {
        if (value == null) return false;
        value = value.toLowerCase(Locale.getDefault());
        return value.contains("some") || value.contains("a_lot");
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
