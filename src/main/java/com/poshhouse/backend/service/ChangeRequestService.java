package com.poshhouse.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poshhouse.backend.dto.change.ChangeRequestResponse;
import com.poshhouse.backend.dto.change.ChangeRequestReviewRequest;
import com.poshhouse.backend.dto.expense.BulkDeleteExpenseRequest;
import com.poshhouse.backend.dto.expense.ExpenseRequest;
import com.poshhouse.backend.dto.expense.ExpenseSplitRequest;
import com.poshhouse.backend.dto.grocery.BulkDeleteGroceryRequest;
import com.poshhouse.backend.dto.grocery.GroceryPurchaseRequest;
import com.poshhouse.backend.dto.meal.BulkMealUpdateRequest;
import com.poshhouse.backend.dto.meal.MealEntryRequest;
import com.poshhouse.backend.entity.ChangeRequest;
import com.poshhouse.backend.entity.ChangeRequestAction;
import com.poshhouse.backend.entity.ChangeRequestStatus;
import com.poshhouse.backend.entity.ChangeRequestTarget;
import com.poshhouse.backend.entity.GroceryPurchase;
import com.poshhouse.backend.entity.HouseExpense;
import com.poshhouse.backend.entity.SplitType;
import com.poshhouse.backend.entity.User;
import com.poshhouse.backend.exception.BadRequestException;
import com.poshhouse.backend.exception.ResourceNotFoundException;
import com.poshhouse.backend.repository.ChangeRequestRepository;
import com.poshhouse.backend.repository.UserRepository;
import com.poshhouse.backend.security.UserPrincipal;
import com.poshhouse.backend.util.MoneyUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeRequestService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private final ChangeRequestRepository changeRequestRepository;
    private final UserRepository userRepository;
    private final GroceryService groceryService;
    private final MealService mealService;
    private final ExpenseService expenseService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ChangeRequestResponse> listRequests(String month, ChangeRequestStatus status) {
        return changeRequestRepository.findAllByOrderByRequestedAtDesc().stream()
            .filter(changeRequest -> month == null || month.isBlank() || month.equals(changeRequest.getMonthKey()))
            .filter(changeRequest -> status == null || status == changeRequest.getStatus())
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ChangeRequestResponse submitGroceryCreateRequest(GroceryPurchaseRequest request, UserPrincipal principal) {
        User requester = requireUser(principal.getId());
        Long payerId = request.payerId() != null ? request.payerId() : requester.getId();
        User payer = requireUser(payerId);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("payerId", payer.getId());
        payload.put("payerName", payer.getUsername());
        payload.put("amount", MoneyUtils.scale(request.amount()).toPlainString());
        payload.put("purchaseDate", request.purchaseDate().toString());
        payload.put("description", normalizeText(request.description()));

        ChangeRequest changeRequest = changeRequestRepository.save(ChangeRequest.builder()
            .targetType(ChangeRequestTarget.GROCERY)
            .actionType(ChangeRequestAction.CREATE)
            .title("Add grocery expense")
            .monthKey(monthKeyFor(request.purchaseDate()))
            .payloadJson(writePayload(payload))
            .requestedBy(requester)
            .build());

        return toResponse(changeRequest);
    }

    @Transactional
    public ChangeRequestResponse submitGroceryDeleteRequest(BulkDeleteGroceryRequest request, UserPrincipal principal) {
        User requester = requireUser(principal.getId());
        List<GroceryPurchase> purchases = groceryService.getPurchases(request.purchaseIds());
        String monthKey = requireSingleMonth(
            purchases.stream().map(GroceryPurchase::getMonthKey).toList(),
            "Grocery removals must stay within the same month."
        );

        ArrayNode items = objectMapper.createArrayNode();
        purchases.stream()
            .sorted(Comparator.comparing(GroceryPurchase::getPurchaseDate).reversed())
            .forEach(purchase -> {
                ObjectNode item = items.addObject();
                item.put("purchaseId", purchase.getId());
                item.put("payerName", purchase.getPayer().getUsername());
                item.put("amount", MoneyUtils.scale(purchase.getAmount()).toPlainString());
                item.put("purchaseDate", purchase.getPurchaseDate().toString());
                item.put("description", normalizeText(purchase.getDescription()));
            });

        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("items", items);

        String title = purchases.size() == 1 ? "Remove grocery expense" : "Remove grocery expenses";
        ChangeRequest changeRequest = changeRequestRepository.save(ChangeRequest.builder()
            .targetType(ChangeRequestTarget.GROCERY)
            .actionType(ChangeRequestAction.DELETE)
            .title(title)
            .monthKey(monthKey)
            .payloadJson(writePayload(payload))
            .requestedBy(requester)
            .build());

        return toResponse(changeRequest);
    }

    @Transactional
    public ChangeRequestResponse submitMealChangeRequest(MealEntryRequest request, UserPrincipal principal) {
        User requester = requireUser(principal.getId());
        User member = requireUser(request.userId());
        int currentMeals = mealService.getCurrentMealCount(request.userId(), request.date());

        if (currentMeals == request.mealsCount()) {
            throw new BadRequestException("No meal change was detected.");
        }

        ChangeRequestAction action = resolveMealAction(currentMeals, request.mealsCount());

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("userId", member.getId());
        payload.put("username", member.getUsername());
        payload.put("date", request.date().toString());
        payload.put("previousMealsCount", currentMeals);
        payload.put("nextMealsCount", request.mealsCount());

        String title = switch (action) {
            case CREATE -> "Add meal entry";
            case UPDATE -> "Update meal entry";
            case DELETE -> "Remove meal entry";
            default -> "Meal change";
        };

        ChangeRequest changeRequest = changeRequestRepository.save(ChangeRequest.builder()
            .targetType(ChangeRequestTarget.MEAL)
            .actionType(action)
            .title(title)
            .monthKey(monthKeyFor(request.date()))
            .payloadJson(writePayload(payload))
            .requestedBy(requester)
            .build());

        return toResponse(changeRequest);
    }

    @Transactional
    public ChangeRequestResponse submitBulkMealChangeRequest(BulkMealUpdateRequest request, UserPrincipal principal) {
        User requester = requireUser(principal.getId());
        List<User> activeUsers = userRepository.findAllByActiveOrderByUsernameAsc(true);
        if (activeUsers.isEmpty()) {
            throw new BadRequestException("No active members are available for that meal request.");
        }

        ArrayNode members = objectMapper.createArrayNode();
        for (User user : activeUsers) {
            ObjectNode member = members.addObject();
            member.put("userId", user.getId());
            member.put("username", user.getUsername());
            member.put("previousMealsCount", mealService.getCurrentMealCount(user.getId(), request.date()));
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("date", request.date().toString());
        payload.put("mealsCount", request.mealsCount());
        payload.set("members", members);

        ChangeRequest changeRequest = changeRequestRepository.save(ChangeRequest.builder()
            .targetType(ChangeRequestTarget.MEAL)
            .actionType(ChangeRequestAction.BULK_UPDATE)
            .title("Bulk meal update")
            .monthKey(monthKeyFor(request.date()))
            .payloadJson(writePayload(payload))
            .requestedBy(requester)
            .build());

        return toResponse(changeRequest);
    }

    @Transactional
    public ChangeRequestResponse submitExpenseCreateRequest(ExpenseRequest request, UserPrincipal principal) {
        User requester = requireUser(principal.getId());
        expenseService.validateExpenseRequest(request);

        List<User> splitUsers = resolveExpenseUsers(request);
        Map<Long, String> usernames = splitUsers.stream()
            .collect(java.util.stream.Collectors.toMap(User::getId, User::getUsername, (left, right) -> left, LinkedHashMap::new));

        ArrayNode splits = objectMapper.createArrayNode();
        if (request.splits() != null && !request.splits().isEmpty()) {
            request.splits().forEach(split -> {
                ObjectNode splitNode = splits.addObject();
                splitNode.put("userId", split.userId());
                splitNode.put("username", usernames.get(split.userId()));
                splitNode.put("shareValue", MoneyUtils.scale(split.shareValue()).toPlainString());
            });
        } else {
            splitUsers.forEach(user -> {
                ObjectNode splitNode = splits.addObject();
                splitNode.put("userId", user.getId());
                splitNode.put("username", user.getUsername());
                splitNode.put("shareValue", "0.00");
            });
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("title", request.title().trim());
        payload.put("category", request.category().trim());
        payload.put("totalAmount", MoneyUtils.scale(request.totalAmount()).toPlainString());
        payload.put("expenseDate", request.expenseDate().toString());
        payload.put("splitType", request.splitType().name());
        payload.put("recurring", Boolean.TRUE.equals(request.recurring()));
        payload.set("splits", splits);

        ChangeRequest changeRequest = changeRequestRepository.save(ChangeRequest.builder()
            .targetType(ChangeRequestTarget.HOUSE_EXPENSE)
            .actionType(ChangeRequestAction.CREATE)
            .title("Add house expense")
            .monthKey(monthKeyFor(request.expenseDate()))
            .payloadJson(writePayload(payload))
            .requestedBy(requester)
            .build());

        return toResponse(changeRequest);
    }

    @Transactional
    public ChangeRequestResponse submitExpenseDeleteRequest(BulkDeleteExpenseRequest request, UserPrincipal principal) {
        User requester = requireUser(principal.getId());
        List<HouseExpense> expenses = expenseService.getExpenses(request.expenseIds());
        String monthKey = requireSingleMonth(
            expenses.stream().map(expense -> monthKeyFor(expense.getExpenseDate())).toList(),
            "Expense removals must stay within the same month."
        );

        ArrayNode items = objectMapper.createArrayNode();
        expenses.stream()
            .sorted(Comparator.comparing(HouseExpense::getExpenseDate).reversed())
            .forEach(expense -> {
                ObjectNode item = items.addObject();
                item.put("expenseId", expense.getId());
                item.put("title", expense.getTitle());
                item.put("category", expense.getCategory());
                item.put("totalAmount", MoneyUtils.scale(expense.getTotalAmount()).toPlainString());
                item.put("expenseDate", expense.getExpenseDate().toString());
            });

        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("items", items);

        String title = expenses.size() == 1 ? "Remove house expense" : "Remove house expenses";
        ChangeRequest changeRequest = changeRequestRepository.save(ChangeRequest.builder()
            .targetType(ChangeRequestTarget.HOUSE_EXPENSE)
            .actionType(ChangeRequestAction.DELETE)
            .title(title)
            .monthKey(monthKey)
            .payloadJson(writePayload(payload))
            .requestedBy(requester)
            .build());

        return toResponse(changeRequest);
    }

    @Transactional
    public ChangeRequestResponse reviewRequest(Long requestId, ChangeRequestReviewRequest request, UserPrincipal principal) {
        User reviewer = requireUser(principal.getId());
        ChangeRequest changeRequest = changeRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Change request not found."));

        if (changeRequest.getStatus() != ChangeRequestStatus.PENDING) {
            throw new BadRequestException("That change request has already been reviewed.");
        }

        if (Boolean.TRUE.equals(request.approved())) {
            applyApprovedRequest(changeRequest);
            changeRequest.setStatus(ChangeRequestStatus.APPROVED);
        } else {
            changeRequest.setStatus(ChangeRequestStatus.REJECTED);
        }

        changeRequest.setReviewedBy(reviewer);
        changeRequest.setReviewedAt(LocalDateTime.now());
        changeRequest.setReviewNote(normalizeText(request.reviewNote()));

        return toResponse(changeRequestRepository.save(changeRequest));
    }

    private void applyApprovedRequest(ChangeRequest changeRequest) {
        JsonNode payload = readPayload(changeRequest);
        switch (changeRequest.getTargetType()) {
            case GROCERY -> applyApprovedGroceryRequest(changeRequest.getActionType(), payload, changeRequest.getRequestedBy());
            case MEAL -> applyApprovedMealRequest(changeRequest.getActionType(), payload);
            case HOUSE_EXPENSE -> applyApprovedExpenseRequest(changeRequest.getActionType(), payload, changeRequest.getRequestedBy());
        }
    }

    private void applyApprovedGroceryRequest(ChangeRequestAction action, JsonNode payload, User requestedBy) {
        switch (action) {
            case CREATE -> groceryService.createPurchase(
                new GroceryPurchaseRequest(
                    payload.path("payerId").asLong(),
                    decimalValue(payload, "amount"),
                    LocalDate.parse(payload.path("purchaseDate").asText()),
                    textValue(payload, "description")
                ),
                requestedBy
            );
            case DELETE -> groceryService.deletePurchases(longValues(payload.path("items"), "purchaseId"));
            default -> throw new BadRequestException("Unsupported grocery request action.");
        }
    }

    private void applyApprovedMealRequest(ChangeRequestAction action, JsonNode payload) {
        switch (action) {
            case CREATE, UPDATE, DELETE -> mealService.upsertEntry(
                new MealEntryRequest(
                    payload.path("userId").asLong(),
                    LocalDate.parse(payload.path("date").asText()),
                    payload.path("nextMealsCount").asInt()
                )
            );
            case BULK_UPDATE -> mealService.bulkSetForDate(
                new BulkMealUpdateRequest(
                    LocalDate.parse(payload.path("date").asText()),
                    payload.path("mealsCount").asInt()
                ),
                longValues(payload.path("members"), "userId")
            );
        }
    }

    private void applyApprovedExpenseRequest(ChangeRequestAction action, JsonNode payload, User requestedBy) {
        switch (action) {
            case CREATE -> expenseService.createExpense(
                new ExpenseRequest(
                    payload.path("title").asText(),
                    payload.path("category").asText(),
                    decimalValue(payload, "totalAmount"),
                    LocalDate.parse(payload.path("expenseDate").asText()),
                    SplitType.valueOf(payload.path("splitType").asText()),
                    payload.path("recurring").asBoolean(false),
                    expenseSplits(payload.path("splits"))
                ),
                requestedBy
            );
            case DELETE -> expenseService.bulkDelete(longValues(payload.path("items"), "expenseId"));
            default -> throw new BadRequestException("Unsupported expense request action.");
        }
    }

    private List<ExpenseSplitRequest> expenseSplits(JsonNode splitsNode) {
        List<ExpenseSplitRequest> splits = new ArrayList<>();
        if (splitsNode == null || !splitsNode.isArray()) {
            return splits;
        }

        for (JsonNode splitNode : splitsNode) {
            splits.add(new ExpenseSplitRequest(
                splitNode.path("userId").asLong(),
                decimalValue(splitNode, "shareValue")
            ));
        }
        return splits;
    }

    private List<Long> longValues(JsonNode arrayNode, String fieldName) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }

        List<Long> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            values.add(item.path(fieldName).asLong());
        }
        return values;
    }

    private ChangeRequestResponse toResponse(ChangeRequest changeRequest) {
        JsonNode payload = readPayload(changeRequest);
        return new ChangeRequestResponse(
            changeRequest.getId(),
            changeRequest.getTitle(),
            changeRequest.getTargetType(),
            changeRequest.getActionType(),
            changeRequest.getStatus(),
            changeRequest.getMonthKey(),
            changeRequest.getRequestedBy().getId(),
            changeRequest.getRequestedBy().getUsername(),
            changeRequest.getReviewedBy() != null ? changeRequest.getReviewedBy().getId() : null,
            changeRequest.getReviewedBy() != null ? changeRequest.getReviewedBy().getUsername() : null,
            changeRequest.getRequestedAt(),
            changeRequest.getReviewedAt(),
            changeRequest.getReviewNote(),
            detailsFor(changeRequest, payload)
        );
    }

    private List<String> detailsFor(ChangeRequest changeRequest, JsonNode payload) {
        return switch (changeRequest.getTargetType()) {
            case GROCERY -> groceryDetails(changeRequest.getActionType(), payload);
            case MEAL -> mealDetails(changeRequest.getActionType(), payload);
            case HOUSE_EXPENSE -> expenseDetails(changeRequest.getActionType(), payload);
        };
    }

    private List<String> groceryDetails(ChangeRequestAction action, JsonNode payload) {
        List<String> details = new ArrayList<>();
        if (action == ChangeRequestAction.CREATE) {
            details.add("Payer: " + payload.path("payerName").asText());
            details.add("Amount: " + formatCurrency(decimalValue(payload, "amount")));
            details.add("Date: " + formatDisplayDate(payload.path("purchaseDate").asText()));
            details.add("Note: " + fallbackText(textValue(payload, "description"), "General grocery run"));
            return details;
        }

        for (JsonNode item : payload.path("items")) {
            details.add(
                item.path("payerName").asText()
                    + " | "
                    + formatCurrency(decimalValue(item, "amount"))
                    + " | "
                    + formatDisplayDate(item.path("purchaseDate").asText())
            );
        }
        return details;
    }

    private List<String> mealDetails(ChangeRequestAction action, JsonNode payload) {
        List<String> details = new ArrayList<>();
        if (action == ChangeRequestAction.BULK_UPDATE) {
            details.add("Date: " + formatDisplayDate(payload.path("date").asText()));
            details.add("Requested meals: " + payload.path("mealsCount").asInt());
            details.add("Members affected: " + payload.path("members").size());
            return details;
        }

        details.add("Member: " + payload.path("username").asText());
        details.add("Date: " + formatDisplayDate(payload.path("date").asText()));
        details.add("From: " + payload.path("previousMealsCount").asInt() + " meals");
        details.add("To: " + payload.path("nextMealsCount").asInt() + " meals");
        return details;
    }

    private List<String> expenseDetails(ChangeRequestAction action, JsonNode payload) {
        List<String> details = new ArrayList<>();
        if (action == ChangeRequestAction.DELETE) {
            for (JsonNode item : payload.path("items")) {
                details.add(
                    item.path("title").asText()
                        + " | "
                        + formatCurrency(decimalValue(item, "totalAmount"))
                        + " | "
                        + formatDisplayDate(item.path("expenseDate").asText())
                );
            }
            return details;
        }

        details.add("Category: " + payload.path("category").asText());
        details.add("Amount: " + formatCurrency(decimalValue(payload, "totalAmount")));
        details.add("Date: " + formatDisplayDate(payload.path("expenseDate").asText()));
        details.add("Split: " + payload.path("splitType").asText());
        if (payload.path("recurring").asBoolean(false)) {
            details.add("Recurring: Yes");
        }

        List<String> splitLabels = new ArrayList<>();
        for (JsonNode split : payload.path("splits")) {
            splitLabels.add(split.path("username").asText() + " = " + split.path("shareValue").asText());
        }
        if (!splitLabels.isEmpty()) {
            details.add("Members: " + String.join(", ", splitLabels));
        }
        return details;
    }

    private List<User> resolveExpenseUsers(ExpenseRequest request) {
        if (request.splits() == null || request.splits().isEmpty()) {
            return userRepository.findAllByActiveOrderByUsernameAsc(true);
        }

        List<Long> ids = request.splits().stream().map(ExpenseSplitRequest::userId).distinct().toList();
        List<User> users = userRepository.findAllById(ids);
        if (users.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more expense members could not be found.");
        }
        return users;
    }

    private ChangeRequestAction resolveMealAction(int currentMeals, int nextMeals) {
        if (nextMeals <= 0) {
            return ChangeRequestAction.DELETE;
        }
        if (currentMeals <= 0) {
            return ChangeRequestAction.CREATE;
        }
        return ChangeRequestAction.UPDATE;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private String writePayload(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not write the change request payload.", exception);
        }
    }

    private JsonNode readPayload(ChangeRequest changeRequest) {
        try {
            return objectMapper.readTree(changeRequest.getPayloadJson());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read the change request payload.", exception);
        }
    }

    private BigDecimal decimalValue(JsonNode node, String fieldName) {
        return MoneyUtils.scale(new BigDecimal(node.path(fieldName).asText("0")));
    }

    private String textValue(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText("");
        return value.isBlank() ? null : value;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String fallbackText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatCurrency(BigDecimal amount) {
        return "$" + MoneyUtils.scale(amount).toPlainString();
    }

    private String formatDisplayDate(String rawDate) {
        return DISPLAY_DATE_FORMATTER.format(LocalDate.parse(rawDate));
    }

    private String monthKeyFor(LocalDate date) {
        return date.format(MONTH_FORMATTER);
    }

    private String requireSingleMonth(List<String> monthKeys, String errorMessage) {
        String first = monthKeys.get(0);
        boolean multipleMonths = monthKeys.stream().anyMatch(monthKey -> !first.equals(monthKey));
        if (multipleMonths) {
            throw new BadRequestException(errorMessage);
        }
        return first;
    }
}
