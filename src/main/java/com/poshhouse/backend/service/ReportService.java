package com.poshhouse.backend.service;

import com.poshhouse.backend.dto.change.ChangeRequestResponse;
import com.poshhouse.backend.dto.expense.ExpenseResponse;
import com.poshhouse.backend.dto.meal.MealEntryResponse;
import com.poshhouse.backend.dto.meal.MealMatrixResponse;
import com.poshhouse.backend.dto.settlement.SettlementResponse;
import com.poshhouse.backend.dto.settlement.SettlementUserDetailDto;
import com.poshhouse.backend.util.MonthWindow;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final float MARGIN = 48f;
    private static final float BODY_FONT_SIZE = 11f;
    private static final float TITLE_FONT_SIZE = 19f;
    private static final float SUBTITLE_FONT_SIZE = 11f;
    private static final float LEADING = 16f;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private final MealService mealService;
    private final ExpenseService expenseService;
    private final SettlementService settlementService;
    private final ChangeRequestService changeRequestService;

    @Transactional(readOnly = true)
    public byte[] generateReport(String reportType, String month) {
        MonthWindow monthWindow = MonthWindow.from(month);
        return switch (reportType) {
            case "meals" -> renderPdf(
                "Meal Summary",
                monthWindow.monthKey(),
                mealLines(mealService.getMatrix(monthWindow.monthKey()))
            );
            case "expenses" -> renderPdf(
                "Expense Summary",
                monthWindow.monthKey(),
                expenseLines(expenseService.listExpenses(monthWindow.monthKey()))
            );
            case "change-requests" -> renderPdf(
                "Change Request Log",
                monthWindow.monthKey(),
                changeRequestLines(changeRequestService.listRequests(monthWindow.monthKey(), null))
            );
            case "balances" -> renderPdf(
                "Balance Report",
                monthWindow.monthKey(),
                balanceLines(settlementService.getSettlement(monthWindow.monthKey()))
            );
            default -> throw new IllegalArgumentException("Unsupported report type.");
        };
    }

    private List<String> mealLines(MealMatrixResponse matrix) {
        List<String> lines = new ArrayList<>();
        int totalMeals = matrix.entries().stream().mapToInt(MealEntryResponse::mealsCount).sum();
        lines.add("Recorded meals: " + totalMeals);
        lines.add("Members tracked: " + matrix.users().size());
        lines.add("");
        lines.add("Daily breakdown");

        Map<LocalDate, List<MealEntryResponse>> byDate = new LinkedHashMap<>();
        matrix.entries().stream()
            .sorted(Comparator.comparing(MealEntryResponse::date))
            .forEach(entry -> byDate.computeIfAbsent(entry.date(), ignored -> new ArrayList<>()).add(entry));

        if (byDate.isEmpty()) {
            lines.add("No approved meal entries were recorded for this month.");
            return lines;
        }

        for (Map.Entry<LocalDate, List<MealEntryResponse>> day : byDate.entrySet()) {
            int dailyTotal = day.getValue().stream().mapToInt(MealEntryResponse::mealsCount).sum();
            lines.add(formatDate(day.getKey()) + " | total meals: " + dailyTotal);

            Map<Long, Integer> mealsByUser = new LinkedHashMap<>();
            day.getValue().forEach(entry -> mealsByUser.put(entry.userId(), entry.mealsCount()));
            matrix.users().forEach(user -> lines.add("  - " + user.username() + ": " + mealsByUser.getOrDefault(user.id(), 0)));
        }

        return lines;
    }

    private List<String> expenseLines(List<ExpenseResponse> expenses) {
        List<String> lines = new ArrayList<>();
        BigDecimal totalAmount = expenses.stream()
            .map(ExpenseResponse::totalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        lines.add("Approved house expenses: " + expenses.size());
        lines.add("Total amount: " + formatCurrency(totalAmount));
        lines.add("");

        if (expenses.isEmpty()) {
            lines.add("No approved house expenses were recorded for this month.");
            return lines;
        }

        for (ExpenseResponse expense : expenses) {
            lines.add(
                expense.title()
                    + " | "
                    + expense.category()
                    + " | "
                    + expense.splitType()
                    + " | "
                    + formatCurrency(expense.totalAmount())
            );
            lines.add("  Date: " + formatDate(expense.expenseDate()) + " | Created by: " + expense.createdByName());
            expense.splits().forEach(split -> lines.add(
                "  - " + split.username() + ": share " + split.shareValue() + ", computed " + formatCurrency(split.computedAmount())
            ));
        }

        return lines;
    }

    private List<String> changeRequestLines(List<ChangeRequestResponse> requests) {
        List<String> lines = new ArrayList<>();
        long pendingCount = requests.stream().filter(request -> request.status().name().equals("PENDING")).count();
        long approvedCount = requests.stream().filter(request -> request.status().name().equals("APPROVED")).count();
        long rejectedCount = requests.stream().filter(request -> request.status().name().equals("REJECTED")).count();

        lines.add("Requests logged: " + requests.size());
        lines.add("Pending: " + pendingCount + " | Approved: " + approvedCount + " | Rejected: " + rejectedCount);
        lines.add("");

        if (requests.isEmpty()) {
            lines.add("No change requests were submitted for this month.");
            return lines;
        }

        for (ChangeRequestResponse request : requests) {
            lines.add(
                "#" + request.id()
                    + " | "
                    + request.status()
                    + " | "
                    + request.targetType()
                    + " "
                    + request.actionType()
                    + " | "
                    + request.title()
            );
            lines.add("  Requested by: " + request.requestedByName() + " on " + request.requestedAt());
            if (request.reviewedByName() != null) {
                lines.add("  Reviewed by: " + request.reviewedByName() + " on " + request.reviewedAt());
            }
            if (request.reviewNote() != null && !request.reviewNote().isBlank()) {
                lines.add("  Review note: " + request.reviewNote());
            }
            request.details().forEach(detail -> lines.add("  - " + detail));
        }

        return lines;
    }

    private List<String> balanceLines(SettlementResponse settlement) {
        List<String> lines = new ArrayList<>();
        lines.add("Total grocery: " + formatCurrency(settlement.summary().totalGrocery()));
        lines.add("Total meals: " + settlement.summary().totalMeals());
        lines.add("Cost per meal: " + formatCurrency(settlement.summary().costPerMeal()));
        lines.add("Total house expenses: " + formatCurrency(settlement.summary().totalHouseExpenses()));
        lines.add("");
        lines.add("Member balances");

        for (SettlementUserDetailDto detail : settlement.userDetails()) {
            lines.add(detail.username() + " | Active: " + detail.active());
            lines.add("  Meals: " + detail.mealsCount() + " | Grocery paid: " + formatCurrency(detail.groceryPaid()));
            lines.add("  Food balance: " + formatCurrency(detail.foodBalance()) + " | House share: " + formatCurrency(detail.houseShare()));
            lines.add("  Total owed: " + formatCurrency(detail.totalOwed()));
        }

        return lines;
    }

    private byte[] renderPdf(String title, String monthKey, List<String> lines) {
        try (PDDocument document = new PDDocument()) {
            PdfCursor cursor = new PdfCursor(document);
            cursor.writeTitle(title);
            cursor.writeSubtitle("Month: " + monthKey);
            cursor.writeBlankLine();

            for (String line : lines) {
                cursor.writeLine(line);
            }

            cursor.close();

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                document.save(outputStream);
                return outputStream.toByteArray();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate the PDF report.", exception);
        }
    }

    private String formatCurrency(BigDecimal value) {
        return "$" + value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDate(LocalDate date) {
        return DATE_FORMATTER.format(date);
    }

    private static final class PdfCursor {

        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float yPosition;

        private PdfCursor(PDDocument document) throws IOException {
            this.document = document;
            addPage();
        }

        private void writeTitle(String text) throws IOException {
            writeText(text, PDType1Font.HELVETICA_BOLD, TITLE_FONT_SIZE, true);
        }

        private void writeSubtitle(String text) throws IOException {
            writeText(text, PDType1Font.HELVETICA_OBLIQUE, SUBTITLE_FONT_SIZE, false);
        }

        private void writeBlankLine() throws IOException {
            ensureSpace(LEADING);
            yPosition -= LEADING;
        }

        private void writeLine(String text) throws IOException {
            if (text == null) {
                return;
            }

            List<String> wrapped = wrap(text, 100);
            for (String line : wrapped) {
                writeText(line, PDType1Font.HELVETICA, BODY_FONT_SIZE, false);
            }
        }

        private void writeText(String text, PDType1Font font, float fontSize, boolean addGapAfter) throws IOException {
            ensureSpace(LEADING);
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText(text);
            contentStream.endText();
            yPosition -= addGapAfter ? LEADING * 1.3f : LEADING;
        }

        private void ensureSpace(float required) throws IOException {
            if (yPosition - required <= MARGIN) {
                addPage();
            }
        }

        private void addPage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }

            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            yPosition = page.getMediaBox().getHeight() - MARGIN;
        }

        private List<String> wrap(String text, int maxChars) {
            List<String> lines = new ArrayList<>();
            String remaining = text;
            while (remaining.length() > maxChars) {
                int breakIndex = remaining.lastIndexOf(' ', maxChars);
                if (breakIndex <= 0) {
                    breakIndex = maxChars;
                }
                lines.add(remaining.substring(0, breakIndex));
                remaining = remaining.substring(breakIndex).trim();
            }
            lines.add(remaining);
            return lines;
        }

        private void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }
    }
}
