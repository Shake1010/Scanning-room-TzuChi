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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javafx.stage.FileChooser;
import java.util.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class ScanningRoomSystem extends Application {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<String, VBox> queueDisplays = new HashMap<>();
    private final Map<String, Label> latestNumberLabels = new HashMap<>();
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tzu Chi Scanning Room System");

        // Main horizontal layout
        HBox mainLayout = new HBox(10);
        mainLayout.setPadding(new Insets(10));
        mainLayout.setStyle("-fx-background-color: white;");

        // Left section for queues
        VBox queuesSection = new VBox(10);
        queuesSection.setPrefWidth(800);
        queuesSection.setMaxHeight(Double.MAX_VALUE);

        // Queue displays container
        HBox queueDisplaysContainer = new HBox(5);
        queueDisplaysContainer.setAlignment(Pos.TOP_CENTER);

        // Add queue displays
        String[] columns = {"2", "5", "8", "6"};
        for (String column : columns) {
            VBox queueDisplay = createQueueDisplay(column);
            queueDisplays.put(column, queueDisplay);
            queueDisplaysContainer.getChildren().add(queueDisplay);
            HBox.setHgrow(queueDisplay, Priority.ALWAYS);
        }

        queuesSection.getChildren().add(queueDisplaysContainer);

        // Right section for video
        VBox videoSection = new VBox();
        videoSection.setPrefWidth(800);
        videoSection.setStyle("""
    -fx-border-color: #2d5d7b;
    -fx-border-width: 2;
    -fx-background-color: #f0f0f0;
    """);

        // Setup video components
        mediaView = new MediaView();
        mediaView.setFitWidth(780);
        mediaView.setFitHeight(870);
        mediaView.setPreserveRatio(true);

        // IMPORTANT: Insert your video path here
        String videoPath = "C:\\Users\\tina_\\Desktop\\TzuChiVideo\\【名人蔬食】甘佳鑫 茹素的力量.mp4"; // Example: "C:/Videos/myvideo.mp4" or "/Users/name/Videos/video.mp4"
        loadAndPlayVideo(videoPath);

        // Add mediaView to video section
        videoSection.getChildren().add(mediaView);
        videoSection.setAlignment(Pos.CENTER);

        mainLayout.getChildren().addAll(queuesSection, videoSection);
        HBox.setHgrow(videoSection, Priority.ALWAYS);

        Scene scene = new Scene(mainLayout, 1600, 900);

        // Add keyboard event handling
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case NUMPAD2 -> callNumber("2");
                case NUMPAD5 -> callNumber("5");
                case NUMPAD8 -> callNumber("8");
                case NUMPAD6 -> callNumber("6");
                case NUMPAD1 -> returnNumber("1");
                case NUMPAD4 -> returnNumber("4");
                case NUMPAD7 -> returnNumber("7");
                case NUMPAD3 -> returnNumber("3");
                default -> {}
            }
        });

        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);
        primaryStage.show();

        startPeriodicUpdates();
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
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

            // Highlight first 4 boxes
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

        display.getChildren().addAll(headerLabel, scrollPane, latestLabel);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return display;
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
        String endpoint = switch (button) {
            case "1" -> BASE_URL + "/withdraw-row2";
            case "4" -> BASE_URL + "/withdraw-row5";
            case "7" -> BASE_URL + "/withdraw-row8";
            case "3" -> BASE_URL + "/withdraw-row6";
            default -> "";
        };

        if (!endpoint.isEmpty()) {
            // Get current queue state
            String column = switch (button) {
                case "1" -> "2";
                case "4" -> "5";
                case "7" -> "8";
                case "3" -> "6";
                default -> "";
            };

            String queueEndpoint = BASE_URL + "/row" + column;
            sendHttpRequest(queueEndpoint, "GET", queueResponse -> {
                try {
                    JsonNode root = OBJECT_MAPPER.readTree(queueResponse);
                    JsonNode patients = root.get("patients");

                    if (patients != null && patients.isArray() && patients.size() > 0) {
                        JsonNode firstPatient = patients.get(0);
                        String patientId = firstPatient.has("patientId") ?
                                firstPatient.get("patientId").asText() : "";

                        if (!patientId.isEmpty()) {
                            // Add patientId to withdraw endpoint
                            String withdrawEndpoint = endpoint + "?patientId=" + patientId;
                            System.out.println("Sending withdraw request to: " + withdrawEndpoint);

                            // Send withdraw request
                            sendHttpRequest(withdrawEndpoint, "PUT", withdrawResponse -> {
                                System.out.println("Withdraw response: " + withdrawResponse);
                                // Refresh queue display after withdrawal
                                sendHttpRequest(queueEndpoint, "GET", finalResponse -> {
                                    Platform.runLater(() -> {
                                        updateQueueDisplay(column, finalResponse);
                                        // Update latest number with previous patient
                                        if (patients.size() > 1) {
                                            String prevPatient = patients.get(1).get("patientId").asText();
                                            updateLatestNumber(column, prevPatient);
                                        } else {
                                            updateLatestNumber(column, "-");
                                        }
                                    });
                                });
                            });
                        }
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
            System.out.println("Updating display for column " + column + " with response: " + responseBody);

            VBox queueDisplay = queueDisplays.get(column);
            if (queueDisplay == null) return;

            ScrollPane scrollPane = (ScrollPane) queueDisplay.getChildren().get(1);
            VBox queueList = (VBox) scrollPane.getContent();

            // Clear existing text but maintain highlight colors
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

            if (patients != null && patients.isArray()) {
                int index = 0;
                for (JsonNode patient : patients) {
                    if (index >= queueList.getChildren().size()) break;

                    String patientId = patient.has("patientId") ? patient.get("patientId").asText() : "";
                    boolean inQueue = patient.has("inQueue") ? patient.get("inQueue").asBoolean(true) : true;

                    if (!patientId.isEmpty() && inQueue) {
                        Label label = (Label) queueList.getChildren().get(index);
                        String backgroundColor = index < 4 ? "#e6f3ff" : "white";
                        label.setText(patientId);
                        index++;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error updating display for column " + column);
            System.err.println("Response body: " + responseBody);
            e.printStackTrace();
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