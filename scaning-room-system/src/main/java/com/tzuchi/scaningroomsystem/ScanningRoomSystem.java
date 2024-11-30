package com.tzuchi.scaningroomsystem;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.stage.Screen;
import com.tzuchi.scaningroomsystem.AudioAnnouncementService;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
// Add these imports at the top of your file
import java.time.LocalDateTime;
import java.time.LocalDate;

import javafx.stage.FileChooser;
import java.util.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import java.util.concurrent.CompletableFuture;

public class ScanningRoomSystem extends Application {
    private static final String BASE_URL = "http://localhost:8080/api";

//    private static final String BASE_URL = "http://172.104.124.175:8888/TzuChiQueueingSystem-0.0.1-SNAPSHOT/api";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final AudioAnnouncementService audioService = new AudioAnnouncementService();


    private final Map<String, VBox> queueDisplays = new HashMap<>();
    private final Map<String, Label> latestNumberLabels = new HashMap<>();
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private final Map<String, Label> categoryStatsLabels = new HashMap<>();
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tzu Chi Scanning Room System");
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Tzu Chi Scanning Room System");
        showLoginScene();
        // Main horizontal layout

    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        audioService.stop();
    }
    private void testVideoPlayback() {
        // Replace this path with the actual path to your video file
        String videoPath = "C:\\Users\\tina_\\Desktop\\TzuChiVideo";

        // Verify file exists before trying to play it
        File videoFile = new File(videoPath);
        if (videoFile.exists()) {
            loadAndPlayVideo(videoPath);
        } else {
            showError("Video Error", "Video file not found at: " + videoPath);
        }
    }
    private void startPeriodicUpdates() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateAllQueues();
            }
        }, 0, 5000);  // Updates every 5 seconds
    }


    private VBox createTopSection() {
        VBox topSection = new VBox(10);
        topSection.setAlignment(Pos.CENTER);

        HBox callButtonsRow = new HBox(20);
        callButtonsRow.setAlignment(Pos.CENTER);

        String[] callButtons = {"2", "5", "8", "6"};
        for (String number : callButtons) {
            Button button = createStyledButton("Call " + number);
            button.setOnAction(e -> callNumber(number));
            callButtonsRow.getChildren().add(button);
        }

        topSection.getChildren().add(callButtonsRow);
        return topSection;
    }

    private HBox createMiddleSection() {
        HBox middleSection = new HBox(20);
        middleSection.setAlignment(Pos.CENTER);

        String[] columns = {"2", "5", "8", "6"};
        for (String column : columns) {
            VBox queueDisplay = createQueueDisplay(column);
            queueDisplays.put(column, queueDisplay);
            middleSection.getChildren().add(queueDisplay);
        }

        return middleSection;
    }

    private VBox createBottomSection() {
        VBox bottomSection = new VBox(10);
        bottomSection.setAlignment(Pos.CENTER);

        HBox returnButtonsRow = new HBox(20);
        returnButtonsRow.setAlignment(Pos.CENTER);

        String[] returnButtons = {"1", "4", "7", "3"};
        for (String number : returnButtons) {
            Button button = createStyledButton("Return " + number);
            button.setOnAction(e -> returnNumber(number));
            returnButtonsRow.getChildren().add(button);
        }

        bottomSection.getChildren().add(returnButtonsRow);
        return bottomSection;
    }

    private Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setStyle("""
            -fx-background-color: #2d5d7b;
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-padding: 10 20;
            -fx-min-width: 120px;
            """);
        return button;
    }

    private VBox createQueueDisplay(String column) {
        VBox display = new VBox(0);
        display.setAlignment(Pos.TOP_CENTER);
        display.setPadding(new Insets(0));
        display.setPrefWidth(190);
        display.setMaxHeight(Double.MAX_VALUE);
        display.setStyle("""
    -fx-border-color: #2d5d7b;
    -fx-border-width: 1;
    -fx-background-color: white;
    """);

        Label headerLabel = new Label("Column " + column);
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setPadding(new Insets(10));
        headerLabel.setStyle("-fx-background-color: #e8e8e8;");

        VBox queueList = new VBox(0);
        queueList.setAlignment(Pos.TOP_CENTER);
        queueList.setStyle("-fx-background-color: white;");

        // Pre-create empty line labels
        for (int i = 0; i < 300; i++) {
            Label lineLabel = new Label("");
            lineLabel.setMaxWidth(Double.MAX_VALUE);
            lineLabel.setPrefHeight(40);

            String backgroundColor = i < 4 ? "#e6f3ff" : "white";
            lineLabel.setStyle(String.format("""
        -fx-border-color: #cccccc;
        -fx-border-width: 0 0 1 0;
        -fx-padding: 10;
        -fx-font-size: 16px;
        -fx-font-weight: bold;
        -fx-alignment: center;
        -fx-background-color: %s;
        """, backgroundColor));

            queueList.getChildren().add(lineLabel);
        }

        ScrollPane scrollPane = new ScrollPane(queueList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setStyle("""
    -fx-background: white;
    -fx-background-color: white;
    -fx-border-width: 0;
    """);

        Label latestLabel = new Label("Latest: -");
        latestLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        latestLabel.setMaxWidth(Double.MAX_VALUE);
        latestLabel.setAlignment(Pos.CENTER);
        latestLabel.setPadding(new Insets(10));
        latestLabel.setStyle("-fx-background-color: #e8e8e8;");
        latestNumberLabels.put(column, latestLabel);

        // Create statistics container
        VBox statsContainer = createStatsLabel(column);

        display.getChildren().addAll(headerLabel, scrollPane, latestLabel, statsContainer);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return display;
    }
    private VBox createStatsLabel(String column) {
        VBox statsContainer = new VBox(5);
        statsContainer.setMaxWidth(Double.MAX_VALUE);
        statsContainer.setPadding(new Insets(5));
        statsContainer.setStyle("""
        -fx-background-color: #f5f5f5;
        -fx-border-color: #2d5d7b;
        -fx-border-width: 1 0 0 0;
    """);

        // Create labels for each statistic type
        Label leftInScanningLabel = new Label(getInitialLeftInScanningText(column));
        Label totalRegisteredLabel = new Label("Total Registered: 0");
        Label totalInQueueLabel = column.equals("2") ? new Label("Total in Queue: 0") : null;

        // Style the labels
        String labelStyle = """
        -fx-font-size: 12px;
        -fx-font-weight: bold;
        -fx-padding: 3;
        -fx-background-color: white;
        -fx-border-color: #cccccc;
        -fx-border-width: 1;
        -fx-background-radius: 3;
        -fx-border-radius: 3;
    """;

        leftInScanningLabel.setStyle(labelStyle);
        totalRegisteredLabel.setStyle(labelStyle);
        if (totalInQueueLabel != null) {
            totalInQueueLabel.setStyle(labelStyle);
        }

        leftInScanningLabel.setMaxWidth(Double.MAX_VALUE);
        totalRegisteredLabel.setMaxWidth(Double.MAX_VALUE);
        if (totalInQueueLabel != null) {
            totalInQueueLabel.setMaxWidth(Double.MAX_VALUE);
        }

        // Add labels to the container
        statsContainer.getChildren().add(leftInScanningLabel);
        statsContainer.getChildren().add(totalRegisteredLabel);
        if (totalInQueueLabel != null) {
            statsContainer.getChildren().add(totalInQueueLabel);
        }

        // Store labels in maps for later updates
        categoryStatsLabels.put(column + "_left", leftInScanningLabel);
        categoryStatsLabels.put(column + "_total_registered", totalRegisteredLabel);
        if (totalInQueueLabel != null) {
            categoryStatsLabels.put(column + "_total_queue", totalInQueueLabel);
        }

        return statsContainer;
    }
    private void showLoginScene() {
        VBox loginLayout = new VBox(10);
        loginLayout.setPadding(new Insets(10));
        loginLayout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Login");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 20));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Login");
        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: red;");

        loginButton.setOnAction(e -> authenticateUser(usernameField.getText(), passwordField.getText(), messageLabel));

        loginLayout.getChildren().addAll(titleLabel, usernameField, passwordField, loginButton, messageLabel);

        Scene loginScene = new Scene(loginLayout, 300, 200);
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }
    private void authenticateUser(String username, String password, Label messageLabel) {
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please fill in both fields.");
            return;
        }

        String endpoint = BASE_URL + "/auth/login";
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);

        try {
            String jsonBody = OBJECT_MAPPER.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(responseBody -> {
                        if (responseBody.equalsIgnoreCase("Login Successful")) {
                            Platform.runLater(this::showMainScene); // Show main scene on successful login
                        } else {
                            Platform.runLater(() -> messageLabel.setText("Invalid username or password."));
                        }
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> messageLabel.setText("Error connecting to server."));
                        return null;
                    });
        } catch (Exception e) {
            messageLabel.setText("Error: " + e.getMessage());
        }
    }

    private void showMainScene() {
        // Get screen dimensions
        javafx.geometry.Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        // Create main container
        VBox root = new VBox();
        root.setPrefSize(screenBounds.getWidth(), screenBounds.getHeight());
        root.setStyle("-fx-background-color: white;");

        // Create header
        HBox header = createHeader();
        root.getChildren().add(header);

        // Main content area
        HBox mainLayout = new HBox();
        mainLayout.setSpacing(10);
        mainLayout.setPadding(new Insets(10));
        mainLayout.setPrefSize(screenBounds.getWidth(), screenBounds.getHeight() - 80);

        // Left section for queues - now 45% of width
        HBox queueDisplaysContainer = new HBox(10);
        queueDisplaysContainer.setAlignment(Pos.TOP_CENTER);
        queueDisplaysContainer.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.45));

        // Add queue displays
        String[] columns = {"2", "5", "8", "6"};
        for (String column : columns) {
            VBox queueDisplay = createEnhancedQueueDisplay(column);
            // Each column takes 24% of the queue section width, leaving 1% spacing between columns
            queueDisplay.prefWidthProperty().bind(queueDisplaysContainer.widthProperty().multiply(0.24));
            queueDisplays.put(column, queueDisplay);
            queueDisplaysContainer.getChildren().add(queueDisplay);
        }

        // Right section for video - now 55% of width
        VBox videoSection = new VBox();
        videoSection.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.55));
        videoSection.setStyle("""
        -fx-border-color: rgb(22, 38, 74);
        -fx-border-width: 1;
        -fx-background-color: #f0f0f0;
        -fx-min-height: 400;
        """);

        // Setup video view with better sizing
        mediaView = new MediaView();
        mediaView.fitWidthProperty().bind(videoSection.widthProperty().multiply(0.95));
        mediaView.fitHeightProperty().bind(videoSection.heightProperty().multiply(0.95));
        mediaView.setPreserveRatio(true);

        videoSection.getChildren().add(mediaView);
        videoSection.setAlignment(Pos.CENTER);

        // Add all sections to main layout
        mainLayout.getChildren().addAll(queueDisplaysContainer, videoSection);
        root.getChildren().add(mainLayout);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        // Create scene
        Scene scene = new Scene(root);
        setupKeyboardHandling(scene);

        // Configure stage to start maximized
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();

        // Start periodic updates
        startPeriodicUpdates();
    }



    private HBox createHeader() {
        HBox header = new HBox();
        header.setStyle("""
        -fx-background-color: rgb(22, 38, 74);
        -fx-padding: 15 20;
        -fx-spacing: 20;
        -fx-min-height: 80;
        """);
        header.setAlignment(Pos.CENTER_LEFT);

        Label logoText = new Label("TC");
        logoText.setStyle("""
        -fx-text-fill: white;
        -fx-font-size: 28;
        -fx-font-weight: bold;
        -fx-min-width: 60;
        -fx-alignment: center;
        """);

        VBox titleBox = new VBox(5);
        Label mainTitle = new Label("SCREENING ROOM");
        mainTitle.setStyle("""
        -fx-font-size: 24;
        -fx-font-weight: bold;
        -fx-text-fill: white;
        """);
        Label subTitle = new Label("SYSTEM");
        subTitle.setStyle("""
        -fx-font-size: 18;
        -fx-text-fill: white;
        """);
        titleBox.getChildren().addAll(mainTitle, subTitle);

        header.getChildren().addAll(logoText, titleBox);
        return header;
    }

    private VBox createEnhancedQueueDisplay(String column) {
        VBox display = new VBox();
        display.setStyle("""
        -fx-background-color: white;
        -fx-border-color: rgb(22, 38, 74);
        -fx-border-width: 1;
        -fx-border-radius: 5 5 0 0;
        """);

        // Header that extends full width
        StackPane headerPane = new StackPane();
        headerPane.setStyle("""
        -fx-background-color: rgb(22, 38, 74);
        -fx-padding: 10;
        -fx-min-height: 45;
        """);
        headerPane.prefWidthProperty().bind(display.widthProperty());

        Label headerLabel = new Label("Column " + column);
        headerLabel.setStyle("""
        -fx-text-fill: white;
        -fx-font-size: 16;
        -fx-font-weight: bold;
        """);
        headerPane.getChildren().add(headerLabel);

        // Queue list with light blue background for first 4 items
        VBox queueList = new VBox();
        for (int i = 0; i < 300; i++) {
            Label lineLabel = new Label("");
            lineLabel.prefWidthProperty().bind(display.widthProperty());
            lineLabel.setMinHeight(40);

            String backgroundColor = i < 4 ? "#e6f3ff" : "white";
            lineLabel.setStyle(String.format("""
            -fx-padding: 10;
            -fx-font-size: 16;
            -fx-font-weight: bold;
            -fx-alignment: center;
            -fx-border-color: #e0e0e0;
            -fx-border-width: 0 0 1 0;
            -fx-background-color: %s;
            """, backgroundColor));

            queueList.getChildren().add(lineLabel);
        }

        // Scroll pane with proper sizing
        ScrollPane scrollPane = new ScrollPane(queueList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setStyle("""
        -fx-background: white;
        -fx-background-color: white;
        -fx-border-width: 0;
        """);

        // Latest number label
        Label latestLabel = new Label("Latest: -");
        latestLabel.setStyle("""
        -fx-padding: 10;
        -fx-font-size: 14;
        -fx-font-weight: bold;
        -fx-background-color: #f5f5f5;
        -fx-border-color: #e0e0e0;
        -fx-border-width: 1 0;
        -fx-alignment: center;
        """);
        latestLabel.prefWidthProperty().bind(display.widthProperty());
        latestNumberLabels.put(column, latestLabel);

        // Stats container with full width
        VBox statsContainer = createStatsLabel(column);
        statsContainer.prefWidthProperty().bind(display.widthProperty());

        display.getChildren().addAll(headerPane, scrollPane, latestLabel, statsContainer);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return display;
    }

    private VBox createQueueList() {
        VBox queueList = new VBox(0);
        queueList.setAlignment(Pos.TOP_CENTER);
        queueList.setStyle("-fx-background-color: white;");

        // Pre-create empty line labels
        for (int i = 0; i < 300; i++) {
            Label lineLabel = new Label("");
            lineLabel.setMaxWidth(Double.MAX_VALUE);
            lineLabel.setPrefHeight(40);

            String backgroundColor = i < 4 ? "#e6f3ff" : "white";
            lineLabel.setStyle(String.format("""
            -fx-border-color: #cccccc;
            -fx-border-width: 0 0 1 0;
            -fx-padding: 10;
            -fx-font-size: 16px;
            -fx-font-weight: bold;
            -fx-alignment: center;
            -fx-background-color: %s;
        """, backgroundColor));

            queueList.getChildren().add(lineLabel);
        }

        return queueList;
    }

    private ScrollPane createScrollPane(VBox content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setStyle("""
        -fx-background: white;
        -fx-background-color: white;
        -fx-border-width: 0;
        -fx-padding: 0;
    """);
        return scrollPane;
    }

    private Label createLatestLabel() {
        Label label = new Label("Latest: -");
        label.setStyle("""
        -fx-font-size: 14px;
        -fx-font-weight: bold;
        -fx-padding: 10;
        -fx-background-color: #f8f9fa;
        -fx-border-color: #e0e0e0;
        -fx-border-width: 1 0;
        -fx-alignment: center;
        -fx-max-width: infinity;
    """);
        return label;
    }
    private VBox createVideoSection() {
        VBox videoSection = new VBox();
        videoSection.setStyle("""
        -fx-border-color: rgb(22, 38, 74);
        -fx-border-width: 1;
        -fx-border-radius: 5;
        -fx-background-color: #f8f9fa;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 1);
        """);

        mediaView = new MediaView();
        mediaView.fitWidthProperty().bind(videoSection.widthProperty().multiply(0.95));
        mediaView.fitHeightProperty().bind(videoSection.heightProperty().multiply(0.95));
        mediaView.setPreserveRatio(true);

        videoSection.getChildren().add(mediaView);
        videoSection.setAlignment(Pos.CENTER);

        return videoSection;
    }

    private void setupKeyboardHandling(Scene scene) {
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                // Call numbers
                case NUMPAD2, DIGIT2 -> callNumber("2");
                case NUMPAD5, DIGIT5 -> callNumber("5");
                case NUMPAD8, DIGIT8 -> callNumber("8");
                case NUMPAD6, DIGIT6 -> callNumber("6");
                // Return numbers
                case NUMPAD1, DIGIT1 -> returnNumber("1");
                case NUMPAD4, DIGIT4 -> returnNumber("4");
                case NUMPAD7, DIGIT7 -> returnNumber("7");
                case NUMPAD3, DIGIT3 -> returnNumber("3");
                default -> {}
            }
        });
    }
    private String getInitialLeftInScanningText(String column) {
        return switch (column) {
            case "2" -> "Left - E: 0 | A: 0 | W: 0";
            case "5" -> "Left P: 0";
            case "8" -> "Left D: 0";
            case "6" -> "Left B: 0";
            default -> "";
        };
    }
    private String getInitialStatsText(String column) {
        return switch (column) {
            case "2" -> "E: 0 | A: 0 | W: 0";
            case "5" -> "Total P: 0";
            case "8" -> "Total D: 0";
            case "6" -> "Total B: 0";
            default -> "";
        };
    }
    private void callNumber(String column) {
        String endpoint = switch (column) {
            case "2" -> BASE_URL + "/call/highest";
            case "5" -> BASE_URL + "/call/nextP";
            case "6" -> BASE_URL + "/call/nextB";
            case "8" -> BASE_URL + "/call/nextD";
            default -> "";
        };

        if (!endpoint.isEmpty()) {
            System.out.println("Calling next number for column " + column + " at endpoint: " + endpoint);

            sendHttpRequest(endpoint, "POST", response -> {
                System.out.println("Call response received: " + response);

                try {
                    JsonNode responseNode = OBJECT_MAPPER.readTree(response);
                    String currentPatient = responseNode.has("currentPatient") ?
                            responseNode.get("currentPatient").asText() : null;

                    if (currentPatient != null) {
                        Platform.runLater(() -> {
                            updateLatestNumber(column, currentPatient);
                            audioService.announceNumber(currentPatient);
                            // Then fetch updated queue
                            String queueEndpoint = BASE_URL + "/row" + column;
                            sendHttpRequest(queueEndpoint, "GET", queueResponse -> {
                                Platform.runLater(() -> updateQueueDisplay(column, queueResponse));
                            });
                        });
                    }
                } catch (Exception e) {
                    System.err.println("Error processing response: " + e.getMessage());
                    Platform.runLater(() -> showError("Processing Error",
                            "Error processing response for column " + column + ": " + e.getMessage()));
                }
            });
        }
    }


    private void returnNumber(String button) {
        String column = switch (button) {
            case "1" -> "2";
            case "4" -> "5";
            case "7" -> "8";
            case "3" -> "6";
            default -> "";
        };

        if (!column.isEmpty()) {
            String queueEndpoint = BASE_URL + "/row" + column;

            // First get the current queue state
            sendHttpRequest(queueEndpoint, "GET", queueResponse -> {
                try {
                    JsonNode root = OBJECT_MAPPER.readTree(queueResponse);
                    JsonNode patients = root.get("patients");

                    if (patients != null && patients.isArray()) {
                        String withdrawEndpoint = BASE_URL + "/withdraw-row" + column+"?patientId=" ;

                        // Send withdraw request - no patientId needed as endpoint will handle it
                        sendHttpRequest(withdrawEndpoint, "PUT", withdrawResponse -> {
                            // Refresh queue display after withdrawal
                            sendHttpRequest(queueEndpoint, "GET", finalResponse -> {
                                Platform.runLater(() -> {
                                    updateQueueDisplay(column, finalResponse);
                                    // Find the new latest called number
                                    try {
                                        JsonNode updatedRoot = OBJECT_MAPPER.readTree(finalResponse);
                                        JsonNode updatedPatients = updatedRoot.get("patients");
                                        if (updatedPatients != null && updatedPatients.isArray()) {
                                            String latestId = "";
                                            int maxNumber = -1;
                                            for (JsonNode p : updatedPatients) {
                                                if (!p.get("inQueue").asBoolean()) {
                                                    String pid = p.get("patientId").asText();
                                                    int num = Integer.parseInt(pid.substring(1));
                                                    if (num > maxNumber) {
                                                        maxNumber = num;
                                                        latestId = pid;
                                                    }
                                                }
                                            }
                                            updateLatestNumber(column, latestId.isEmpty() ? "-" : latestId);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                            });
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showError("Error",
                            "Failed to process queue data: " + e.getMessage()));
                }
            });
        }
    }

    private void updateAllQueues() {
        String[] columns = {"2", "5", "8", "6"};
        for (String column : columns) {
            String endpoint = BASE_URL + "/row" + column;
            System.out.println("Updating queue for column " + column); // Debug print
            sendHttpRequest(endpoint, "GET", response -> {
                Platform.runLater(() -> updateQueueDisplay(column, response));
            });
        }
    }

    private void sendHttpRequest(String endpoint, String method, java.util.function.Consumer<String> responseHandler) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json");

            if (method.equals("PUT")) {
                requestBuilder.PUT(HttpRequest.BodyPublishers.noBody());
            } else if (method.equals("POST")) {
                requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
            } else {
                requestBuilder.GET();
            }

            HttpRequest request = requestBuilder.build();
            System.out.println("Sending " + method + " request to: " + endpoint); // Debug log

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        System.out.println("Response status: " + response.statusCode()); // Debug log
                        System.out.println("Response body: " + response.body()); // Debug log

                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            responseHandler.accept(response.body());
                        } else {
                            Platform.runLater(() -> {
                                try {
                                    JsonNode errorNode = OBJECT_MAPPER.readTree(response.body());
                                    String errorMessage = errorNode.has("message") ?
                                            errorNode.get("message").asText() :
                                            "Request failed with status: " + response.statusCode();
                                    showError("API Error", errorMessage + "\nEndpoint: " + endpoint);
                                } catch (Exception e) {
                                    showError("API Error",
                                            "Request failed with status: " + response.statusCode() +
                                                    "\nEndpoint: " + endpoint);
                                }
                            });
                        }
                    })
                    .exceptionally(e -> {
                        e.printStackTrace();
                        Platform.runLater(() -> showError("Connection Error",
                                "Failed to connect to: " + endpoint + "\nError: " + e.getMessage()));
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> showError("Request Error",
                    "Error creating request for: " + endpoint + "\nError: " + e.getMessage()));
        }
    }

    private void updateLatestNumber(String column, String patientId) {
        try {
            Label label = latestNumberLabels.get(column);
            if (label != null && patientId != null) {
                String displayId = patientId.trim().replaceAll("^\"|\"$", "");
                if (!displayId.isEmpty()) {
                    label.setText("Latest: " + displayId);
                    System.out.println("Updated latest number for column " + column + " to: " + displayId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating latest number for column " + column + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateQueueDisplay(String column, String responseBody) {
        try {
            VBox queueDisplay = queueDisplays.get(column);
            if (queueDisplay == null) return;

            ScrollPane scrollPane = (ScrollPane) queueDisplay.getChildren().get(1);
            VBox queueList = (VBox) scrollPane.getContent();

            // Reset all labels
            for (int i = 0; i < queueList.getChildren().size(); i++) {
                if (queueList.getChildren().get(i) instanceof Label) {
                    Label label = (Label) queueList.getChildren().get(i);
                    String backgroundColor = i < 4 ? "#e6f3ff" : "white";
                    label.setText("");
                    label.setStyle(String.format("""
                -fx-border-color: #cccccc;
                -fx-border-width: 0 0 1 0;
                -fx-padding: 10;
                -fx-font-size: 16px;
                -fx-font-weight: bold;
                -fx-alignment: center;
                -fx-background-color: %s;
                """, backgroundColor));
                }
            }

            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode patients = root.get("patients");

            // Initialize counters
            Map<Character, Integer> leftInScanning = new HashMap<>();
            Map<Character, Integer> highestNumbers = new HashMap<>();
            int totalInQueue = 0;

            if (patients != null && patients.isArray()) {
                int index = 0;
                for (JsonNode patient : patients) {
                    String patientId = patient.has("patientId") ? patient.get("patientId").asText() : "";
                    boolean inQueue = patient.has("inQueue") ? patient.get("inQueue").asBoolean(true) : true;

                    if (!patientId.isEmpty()) {
                        // Update display for in-queue patients
                        if (inQueue) {
                            if (index < queueList.getChildren().size()) {
                                Label label = (Label) queueList.getChildren().get(index);
                                String backgroundColor = index < 4 ? "#e6f3ff" : "white";
                                label.setText(patientId);
                                index++;
                            }

                            // Count in-queue patients
                            char category = patientId.charAt(0);
                            leftInScanning.merge(category, 1, Integer::sum);
                            totalInQueue++;
                        }

                        // Track highest number for each category
                        char category = patientId.charAt(0);
                        try {
                            int number = Integer.parseInt(patientId.substring(1));
                            highestNumbers.merge(category, number, Integer::max);
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing number from patientId: " + patientId);
                        }
                    }
                }
            }

            // Update statistics labels
            updateStatisticsLabels(column, leftInScanning, totalInQueue, highestNumbers);

        } catch (Exception e) {
            System.err.println("Error updating display for column " + column);
            e.printStackTrace();
        }
    }
    private void getRegistrationStationCount(String column) {
        String endpoint = BASE_URL + "/get/allRegister";
        sendHttpRequest(endpoint, "GET", response -> {
            try {
                JsonNode registrations = OBJECT_MAPPER.readTree(response);
                if (registrations.isArray()) {
                    Map<Integer, Integer> sectionCounts = new HashMap<>();

                    // Get today's date at start of day for comparison
                    LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();

                    // Count only today's registrations by section
                    for (JsonNode reg : registrations) {
                        // Parse registration timestamp
                        LocalDateTime regTime = LocalDateTime.parse(reg.get("registeredTime").asText());

                        // Only count if registration is from today
                        if (regTime.isAfter(today) || regTime.isEqual(today)) {
                            int section = reg.get("sectionNumber").asInt();
                            sectionCounts.merge(section, 1, Integer::sum);
                        }
                    }

                    // Update total registered label
                    Platform.runLater(() -> {
                        Label totalRegLabel = categoryStatsLabels.get(column + "_total_registered");
                        if (totalRegLabel != null) {
                            int sectionNumber = Integer.parseInt(column);
                            int count = sectionCounts.getOrDefault(sectionNumber, 0);
                            totalRegLabel.setText(String.format("Total Registered: %d", count));
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Error processing registration station data: " + e.getMessage());
            }
        });
    }
    private void updateStatisticsLabels(String column, Map<Character, Integer> leftInScanning,
                                        int totalInQueue, Map<Character, Integer> highestNumbers) {
        // Update left in scanning label
        Label leftLabel = categoryStatsLabels.get(column + "_left");
        if (leftLabel != null) {
            String leftText = switch (column) {
                case "2" -> String.format("Left - E: %d | A: %d | W: %d",
                        leftInScanning.getOrDefault('E', 0),
                        leftInScanning.getOrDefault('A', 0),
                        leftInScanning.getOrDefault('W', 0));
                case "5" -> String.format("Left P: %d", leftInScanning.getOrDefault('P', 0));
                case "8" -> String.format("Left D: %d", leftInScanning.getOrDefault('D', 0));
                case "6" -> String.format("Left B: %d", leftInScanning.getOrDefault('B', 0));
                default -> "";
            };
            leftLabel.setText(leftText);
        }

        // Update total registered label
        Label totalRegLabel = categoryStatsLabels.get(column + "_total_registered");
        if (totalRegLabel != null) {
            int totalRegistered = switch (column) {
                case "2" -> {
                    int eTotal = highestNumbers.getOrDefault('E', 0);
                    int aTotal = highestNumbers.getOrDefault('A', 0);
                    int wTotal = highestNumbers.getOrDefault('W', 0);
                    yield eTotal + aTotal + wTotal;
                }
                case "5" -> highestNumbers.getOrDefault('P', 0);
                case "8" -> highestNumbers.getOrDefault('D', 0);
                case "6" -> highestNumbers.getOrDefault('B', 0);
                default -> 0;
            };
            totalRegLabel.setText(String.format("Total Registered: %d", totalRegistered));
        }

        // Update total in queue label (only for column 2)
        if (column.equals("2")) {
            Label totalQueueLabel = categoryStatsLabels.get(column + "_total_queue");
            if (totalQueueLabel != null) {
                totalQueueLabel.setText(String.format("Total in Queue: %d", totalInQueue));
            }
        }
    }
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupVideoSection(VBox videoSection) {
        try {
            mediaView = new MediaView();
            mediaView.setFitWidth(780); // Adjust as needed
            mediaView.setFitHeight(870); // Adjust as needed
            mediaView.setPreserveRatio(true);

            // Replace the existing placeholder
            videoSection.getChildren().clear();
            videoSection.getChildren().add(mediaView);

            // Center the video
            videoSection.setAlignment(Pos.CENTER);
        } catch (Exception e) {
            System.err.println("Error setting up video section: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void loadAndPlayVideo(String videoPath) {
        try {
            // Stop any currently playing video
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            // Create new Media and MediaPlayer
            File videoFile = new File(videoPath);
            Media media = new Media(videoFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            // Set the MediaPlayer to the MediaView
            mediaView.setMediaPlayer(mediaPlayer);

            // Configure player settings
            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop video
            mediaPlayer.setMute(true);  // Add this line to mute the video

            // Handle errors
            mediaPlayer.setOnError(() -> {
                System.err.println("Media Player Error: " + mediaPlayer.getError());
                showError("Video Playback Error",
                        "Error playing video: " + mediaPlayer.getError().getMessage());
            });

            // Start playback
            mediaPlayer.play();

        } catch (Exception e) {
            System.err.println("Error loading video: " + e.getMessage());
            showError("Video Loading Error",
                    "Error loading video file: " + e.getMessage());
        }
    }



    public static void main(String[] args) {
        launch(args);
    }
}