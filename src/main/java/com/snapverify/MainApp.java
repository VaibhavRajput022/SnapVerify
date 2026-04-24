package com.snapverify;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import net.sourceforge.tess4j.Tesseract;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MainApp extends Application {

    private final ScreenshotRepository repository = new ScreenshotRepository();
    private final ObservableList<ScreenshotItem> displayedItems = FXCollections.observableArrayList();

    private final ListView<ScreenshotItem> listView = new ListView<>(displayedItems);
    private final ImageView imageView = new ImageView();
    private final TextArea ocrArea = new TextArea();
    private final TextField searchField = new TextField();
    private final Label statusLabel = new Label("Ready");

    private ScreenshotItem currentItem;

    @Override
    public void start(Stage stage) {

        // शीर्ष बार
        Label title = new Label("SnapVerify MVP");
        Button loadBtn = new Button("Load Folder");
        Button ocrBtn = new Button("Run OCR");

        searchField.setPromptText("Search by name / OCR");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox topBar = new HBox(12, title, spacer, searchField, loadBtn, ocrBtn);
        topBar.setPadding(new Insets(10));

        // List view
        listView.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(ScreenshotItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayName());
            }
        });

        listView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldItem, newItem) -> showItem(newItem));

        // Image preview (FIXED SIZE)
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(500);
        imageView.setFitHeight(350);

        StackPane imagePane = new StackPane(imageView);
        imagePane.setPadding(new Insets(10));
        imagePane.setStyle("-fx-background-color: #121212;");
        imagePane.setPrefSize(550, 400);
        imagePane.setMaxSize(600, 450);

        // OCR panel
        ocrArea.setWrapText(true);
        ocrArea.setEditable(false);

        VBox rightPane = new VBox(10, new Label("OCR Text"), ocrArea);
        rightPane.setPadding(new Insets(10));
        rightPane.setMinWidth(350);
        VBox.setVgrow(ocrArea, Priority.ALWAYS);

        // Split center
        SplitPane splitPane = new SplitPane(imagePane, rightPane);
        splitPane.setDividerPositions(0.55);

        // Left panel
        VBox leftPane = new VBox(10, new Label("Screenshots"), listView);
        leftPane.setPadding(new Insets(10));
        leftPane.setMinWidth(220);
        VBox.setVgrow(listView, Priority.ALWAYS);

        // Main layout
        BorderPane content = new BorderPane();
        content.setLeft(leftPane);
        content.setCenter(splitPane);

        // Bottom bar
        HBox bottomBar = new HBox(statusLabel);
        bottomBar.setPadding(new Insets(10));

        VBox root = new VBox(topBar, content, bottomBar);

        // Actions
        loadBtn.setOnAction(e -> loadFolder(stage));

        ocrBtn.setOnAction(e -> {
            if (currentItem != null) {
                runOCR(currentItem.file());
            }
        });

        searchField.textProperty()
                .addListener((obs, oldV, newV) -> applySearch(newV));

        // Window size FIX
        Scene scene = new Scene(root, 1000, 650);
        stage.setTitle("SnapVerify MVP");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    private void loadFolder(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Screenshot Folder");

        File dir = chooser.showDialog(stage);
        if (dir == null) return;

        repository.clear();
        displayedItems.clear();
        currentItem = null;
        imageView.setImage(null);
        ocrArea.clear();

        File[] files = dir.listFiles();
        if (files == null) {
            statusLabel.setText("Folder not readable");
            return;
        }

        List<ScreenshotItem> items = Arrays.stream(files)
                .filter(File::isFile)
                .filter(f -> {
                    String n = f.getName().toLowerCase();
                    return n.endsWith(".png") || n.endsWith(".jpg")
                            || n.endsWith(".jpeg") || n.endsWith(".bmp");
                })
                .map(ScreenshotItem::new)
                .collect(Collectors.toList());

        repository.addAll(items);
        displayedItems.setAll(items);

        statusLabel.setText("Loaded " + displayedItems.size() + " screenshots");

        if (!displayedItems.isEmpty()) {
            showItem(displayedItems.get(0));
        }
    }

    private void showItem(ScreenshotItem item) {
        if (item == null) return;

        currentItem = item;

        Image img = new Image(item.file().toURI().toString());
        imageView.setImage(img);

        // Auto OCR
        runOCR(item.file());
    }

    // OCR FIXED METHOD
    private void runOCR(File file) {

        statusLabel.setText("Running OCR...");
        ocrArea.setText("Processing...");

        new Thread(() -> {
            try {
                // ✅ Ensure tessdata path is always correct
                System.setProperty("TESSDATA_PREFIX", "C:/Program Files/Tesseract-OCR/tessdata");

                Tesseract tesseract = new Tesseract();
                tesseract.setLanguage("eng");

                // ✅ Read image safely
                java.awt.image.BufferedImage bufferedImage =
                        javax.imageio.ImageIO.read(file);

                // 🚨 THIS was your crash cause
                if (bufferedImage == null) {
                    throw new RuntimeException("Invalid or unsupported image format");
                }

                String text = tesseract.doOCR(bufferedImage);

                javafx.application.Platform.runLater(() -> {
                    ocrArea.setText(text);

                    if (currentItem != null) {
                        currentItem.setOcrText(text);
                    }

                    statusLabel.setText("OCR complete");
                });

            } catch (Exception e) {
                e.printStackTrace();

                javafx.application.Platform.runLater(() -> {
                    ocrArea.setText("");
                    statusLabel.setText("OCR failed: Invalid image or path");
                });
            }
        }).start();
    }

    private void applySearch(String query) {
        displayedItems.setAll(repository.search(query));
    }

    public static void main(String[] args) {
        launch(args);
    }
}