package com.drishti.dataviztool;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert; // Needed for Help Dialog
import javafx.scene.control.Alert.AlertType; // Needed for Help Dialog Type


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


import javafx.beans.property.SimpleStringProperty;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.PieChart;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;

import javax.imageio.ImageIO;


public class HelloController {

    // FXML UI Elements
    @FXML
    private Label infoLabel;
    @FXML
    private Label filePathLabel;
    @FXML
    private TableView<Map<String, String>> dataTable;

    // Combo Boxes for user selections
    @FXML
    private ComboBox<String> categoryColumnComboBox;
    @FXML
    private ComboBox<String> valueColumnComboBox;
    @FXML
    private ComboBox<String> chartTypeComboBox;
    @FXML
    private ComboBox<String> aggregationTypeComboBox;
    @FXML
    private ComboBox<String> delimiterComboBox;

    // Text Fields for chart customization
    @FXML
    private TextField chartTitleField;
    @FXML
    private Label xAxisLabel;
    @FXML
    private Label yAxisLabel;

    // Original TextFields to hold user input for chart axis labels
    @FXML
    private TextField xAxisLabelField;
    @FXML
    private TextField yAxisLabelField;


    // Container for dynamic filter rows
    @FXML
    private VBox filterRowsContainer;
    @FXML
    private Button addFilterButton;
    @FXML
    private Button clearAllFiltersButton;
    @FXML
    private ComboBox<String> globalAndOrComboBox;

    // Container for dynamically loaded charts
    @FXML
    private StackPane chartContainer;

    // Buttons for actions
    @FXML
    private javafx.scene.control.Button saveChartButton;
    @FXML
    private javafx.scene.control.Button exportDataButton;

    // Data storage
    private ObservableList<Map<String, String>> loadedData;
    private ObservableList<Map<String, String>> currentDisplayedData;
    private String[] columnHeaders;

    // Regex pattern to check if a string is likely a number (integer or double)
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    // Helper class to store a single filter condition
    private static class FilterCondition {
        final String column;
        final String operator;
        final String value;

        public FilterCondition(String column, String operator, String value) {
            this.column = column;
            this.operator = operator;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Column: '" + column + "', Operator: '" + operator + "', Value: '" + value + "'";
        }
    }


    // --- Initialization ---
    @FXML
    public void initialize() {
        // Populate Delimiter ComboBox with common options
        ObservableList<String> delimiters = FXCollections.observableArrayList(
                ",", ";", "\t", "|"
        );
        delimiterComboBox.setItems(delimiters);
        delimiterComboBox.getSelectionModel().select(","); // Default to comma

        // Populate Global AND/OR ComboBox
        ObservableList<String> andOrOptions = FXCollections.observableArrayList(
                "AND", "OR"
        );
        globalAndOrComboBox.setItems(andOrOptions);
        globalAndOrComboBox.getSelectionModel().select("AND"); // Default to AND

        // Initially disable filter-related buttons until data is loaded
        addFilterButton.setDisable(true);
        clearAllFiltersButton.setDisable(true);
        globalAndOrComboBox.setDisable(true);

        // Add listener to chartTypeComboBox to change axis labels
        chartTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if ("Geographic Scatter Plot".equals(newVal)) {
                // Change labels for geographic plot
                xAxisLabel.setText("Latitude Column:");
                yAxisLabel.setText("Longitude Column:");
                aggregationTypeComboBox.setDisable(true);
                aggregationTypeComboBox.getSelectionModel().select("None"); // Force "None" for Geo plots
            } else {
                // Revert labels for other chart types
                xAxisLabel.setText("X-Axis (Category):");
                yAxisLabel.setText("Y-Axis (Value):");
                aggregationTypeComboBox.setDisable(false); // Re-enable for other chart types
            }
        });
    }

    /**
     * Shows a modal dialog with help and guidance based on the selected chart type.
     */
    @FXML
    protected void onShowHelpClick() {
        final String selectedChartType = chartTypeComboBox.getSelectionModel().getSelectedItem();
        final String title = "Chart Guidance";
        String content;

        if (selectedChartType == null) {
            content = "1. Load a CSV File first.\n2. Select an X-Axis, Y-Axis, and Chart Type to begin visualization.";
        } else {
            content = getChartGuidanceText(selectedChartType);
        }

        final Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("DataViz Tool Help");
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.getDialogPane().getStyleClass().add("help-dialog");
        alert.showAndWait();
    }

    /**
     * Provides specific instructions for the selected chart type.
     */
    private String getChartGuidanceText(final String chartType) {
        switch (chartType) {
            case "Bar Chart":
            case "Line Chart":
            case "Scatter Chart":
                return "Type: " + chartType + "\n\n"
                        + "Requirements:\n"
                        + "  - X-Axis: Text/Category column (e.g., Country, Product).\n"
                        + "  - Y-Axis: Numeric column (e.g., Sales, Population).\n"
                        + "  - Aggregation: Use 'Sum', 'Average', or 'Count' to summarize data by category. Use 'None' only if the Y-Axis is numeric and you want to plot every single row.\n"
                        + "\nTip: 'Sum' is best for Bar Charts; 'None' or 'Average' is often best for Line/Scatter Charts.";

            case "Pie Chart":
                return "Type: Pie Chart\n\n"
                        + "Requirements:\n"
                        + "  - Pie Charts visualize parts of a whole.\n"
                        + "  - X-Axis: Category/Grouping column (e.g., Continent).\n"
                        + "  - Aggregation: MUST be 'Sum' or 'Count'. 'None' is not allowed.\n"
                        + "\nTip: The size of each slice will be proportional to the aggregated value.";

            case "Geographic Scatter Plot":
                return "Type: Geographic Scatter Plot\n\n"
                        + "Requirements:\n"
                        + "  - X-Axis: Column containing **Latitude** (numeric, -90 to 90).\n"
                        + "  - Y-Axis: Column containing **Longitude** (numeric, -180 to 180).\n"
                        + "  - Aggregation: Forced to 'None'.\n"
                        + "  - Point Size: The size of the red dot is scaled by the numeric values in the Longitude column (you must select a numeric column here).\n"
                        + "  - Map File: Requires 'world_map.png' in the resources folder.";

            default:
                return "No specific guidance available for this chart type. Please make sure X and Y axes are selected correctly and consider using aggregation.";
        }
    }


    // --- Event Handlers (continued) ---

    /**
     * Handles the "Load CSV File" button click event.
     * Opens a file chooser, reads the selected CSV, populates the table,
     * and initializes UI controls for charting and filtering.
     */
    @FXML
    protected void onLoadCsvFileClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV File");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
        fileChooser.getExtensionFilters().add(extFilter);

        final Stage stage = (Stage) filePathLabel.getScene().getWindow(); // Use final for Stage
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            filePathLabel.setText("Selected: " + selectedFile.getAbsolutePath());
            infoLabel.setText("File '" + selectedFile.getName() + "' selected. Loading data...");

            final String selectedDelimiter = delimiterComboBox.getSelectionModel().getSelectedItem(); // Use final
            if (selectedDelimiter == null || selectedDelimiter.isEmpty()) {
                infoLabel.setText("Please select a delimiter before loading the file.");
                return;
            }
            final String delimiterRegex = Pattern.quote(selectedDelimiter); // Use final


            try {
                final List<String> lines = Files.readAllLines(Paths.get(selectedFile.getAbsolutePath())); // Use final

                if (lines.isEmpty()) {
                    infoLabel.setText("The selected CSV file is empty.");
                    clearAndDisableUI();
                    return;
                }

                String[] headers = lines.get(0).split(delimiterRegex);
                for (int i = 0; i < headers.length; i++) {
                    headers[i] = headers[i].trim();
                }
                this.columnHeaders = headers;

                if (headers.length == 0) {
                    infoLabel.setText("CSV file has no headers or invalid format.");
                    clearAndDisableUI();
                    return;
                }

                dataTable.getColumns().clear();
                for (final String header : headers) { // Use final for iteration variable
                    final String finalHeader = header; // Use final local copy
                    TableColumn<Map<String, String>, String> column = new TableColumn<>(finalHeader);
                    column.setCellValueFactory(cellData -> {
                        final Map<String, String> rowMap = cellData.getValue(); // Use final
                        return new SimpleStringProperty(rowMap.get(finalHeader));
                    });
                    dataTable.getColumns().add(column);
                }

                ObservableList<Map<String, String>> items = FXCollections.observableArrayList();
                for (int i = 1; i < lines.size(); i++) {
                    final String line = lines.get(i); // Use final
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    String[] data = line.split(delimiterRegex);
                    Map<String, String> row = new HashMap<>();

                    for (int colIndex = 0; colIndex < Math.min(headers.length, data.length); colIndex++) {
                        row.put(headers[colIndex], data[colIndex].trim());
                    }
                    items.add(row);
                }
                this.loadedData = items;
                this.currentDisplayedData = FXCollections.observableArrayList(loadedData);

                dataTable.setItems(currentDisplayedData);
                infoLabel.setText("File '" + selectedFile.getName() + "' loaded and displayed in table. Rows: " + items.size());

                ObservableList<String> headerOptions = FXCollections.observableArrayList(headers);
                categoryColumnComboBox.setItems(headerOptions);
                valueColumnComboBox.setItems(headerOptions);

                // Populate Chart Type ComboBox with ALL desired chart types
                ObservableList<String> chartTypes = FXCollections.observableArrayList(
                        "Bar Chart", "Line Chart", "Scatter Chart", "Pie Chart", "Geographic Scatter Plot"
                );
                chartTypeComboBox.setItems(chartTypes);

                ObservableList<String> aggregationTypes = FXCollections.observableArrayList(
                        "None", "Sum", "Average", "Count"
                );
                aggregationTypeComboBox.setItems(aggregationTypes);

                enableUIControls(true);
                addFilterButton.setDisable(false);
                clearAllFiltersButton.setDisable(false);
                globalAndOrComboBox.setDisable(false);

                onAddFilterButtonClick();


                if (headers.length >= 2) {
                    final String header0 = headers[0]; // Use final local copies for updateChartClick
                    final String header1 = headers[1]; // Use final local copies for updateChartClick

                    categoryColumnComboBox.getSelectionModel().select(header0);
                    valueColumnComboBox.getSelectionModel().select(header1);
                    chartTypeComboBox.getSelectionModel().select("Bar Chart");
                    aggregationTypeComboBox.getSelectionModel().select("None");

                    chartTitleField.setText("Chart of " + header1 + " by " + header0);
                    xAxisLabelField.setText(header0);
                    yAxisLabelField.setText(header1);

                    onUpdateChartClick();
                } else {
                    infoLabel.setText("File loaded. Not enough columns for automatic charting (need at least 2). Please select columns manually if applicable.");
                    chartContainer.getChildren().clear();
                    chartTitleField.setText("");
                    xAxisLabelField.setText("");
                    yAxisLabelField.setText("");
                }

            } catch (IOException e) {
                infoLabel.setText("Error reading file: " + e.getMessage());
                System.err.println("Error reading CSV file: " + e.getMessage());
                e.printStackTrace();
                clearAndDisableUI();
            } catch (Exception e) {
                infoLabel.setText("Error processing CSV data: " + e.getMessage());
                System.err.println("Error processing CSV data: " + e.getMessage());
                e.printStackTrace();
                clearAndDisableUI();
            }
        } else {
            filePathLabel.setText("No file selected.");
            infoLabel.setText("File selection cancelled.");
            clearAndDisableUI();
        }
    }

    /**
     * Dynamically adds a new filter row to the UI.
     */
    @FXML
    protected void onAddFilterButtonClick() {
        final HBox filterRow = new HBox(10); // Use final
        filterRow.getStyleClass().add("filter-row");

        ComboBox<String> columnComboBox = new ComboBox<>();
        columnComboBox.setPromptText("Select Column");
        if (columnHeaders != null) {
            columnComboBox.setItems(FXCollections.observableArrayList(columnHeaders));
        }
        columnComboBox.getStyleClass().add("input-combo");
        columnComboBox.setPrefWidth(150);

        ComboBox<String> operatorComboBox = new ComboBox<>();
        operatorComboBox.setPromptText("Operator");
        ObservableList<String> filterOperators = FXCollections.observableArrayList(
                "contains", "=", ">", "<", ">=", "<="
        );
        operatorComboBox.setItems(filterOperators);
        operatorComboBox.getStyleClass().add("input-combo");
        operatorComboBox.setPrefWidth(80);

        TextField valueField = new TextField();
        valueField.setPromptText("Enter value");
        valueField.getStyleClass().add("input-text");
        HBox.setHgrow(valueField, javafx.scene.layout.Priority.ALWAYS);

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().add("danger-button");
        removeButton.setOnAction(event -> filterRowsContainer.getChildren().remove(filterRow)); // filterRow is effectively final

        filterRow.getChildren().addAll(columnComboBox, operatorComboBox, valueField, removeButton);
        filterRowsContainer.getChildren().add(filterRow);

        if (columnHeaders != null && columnHeaders.length > 0) {
            columnComboBox.getSelectionModel().select(0);
        }
        operatorComboBox.getSelectionModel().select("contains");
    }

    /**
     * Clears all dynamically added filter rows and resets the displayed data.
     */
    @FXML
    protected void onClearAllFiltersClick() {
        filterRowsContainer.getChildren().clear();
        final int loadedDataSize = loadedData != null ? loadedData.size() : 0; // Use final local copy
        infoLabel.setText("All filters cleared. Displaying all " + loadedDataSize + " rows.");
        if (loadedData != null) {
            currentDisplayedData = FXCollections.observableArrayList(loadedData);
            dataTable.setItems(currentDisplayedData);
        }
        onUpdateChartClick();
    }


    /**
     * Handles the "Apply All Filters" button click event.
     * Collects all filter conditions and applies them using the selected global AND/OR logic.
     */
    @FXML
    protected void onApplyFilterClick() {
        if (loadedData == null || loadedData.isEmpty()) {
            infoLabel.setText("No data loaded to filter.");
            return;
        }

        if (filterRowsContainer.getChildren().isEmpty()) {
            infoLabel.setText("No filters added. Displaying all data.");
            currentDisplayedData = FXCollections.observableArrayList(loadedData);
            dataTable.setItems(currentDisplayedData);
            onUpdateChartClick();
            return;
        }

        final String globalLogic = globalAndOrComboBox.getSelectionModel().getSelectedItem(); // Use final
        if (globalLogic == null || globalLogic.isEmpty()) {
            infoLabel.setText("Please select a global filter logic (AND/OR).");
            return;
        }

        final List<FilterCondition> conditions = new ArrayList<>(); // Use final
        for (Node node : filterRowsContainer.getChildren()) {
            if (node instanceof HBox filterRow) {
                ComboBox<String> columnComboBox = (ComboBox<String>) filterRow.getChildren().get(0);
                ComboBox<String> operatorComboBox = (ComboBox<String>) filterRow.getChildren().get(1);
                TextField valueField = (TextField) filterRow.getChildren().get(2);

                String filterColumn = columnComboBox.getSelectionModel().getSelectedItem();
                String filterOperator = operatorComboBox.getSelectionModel().getSelectedItem();
                String filterValue = valueField.getText();

                if (filterColumn != null && !filterColumn.isEmpty() &&
                        filterOperator != null && !filterOperator.isEmpty() &&
                        filterValue != null && !filterValue.isEmpty()) {
                    conditions.add(new FilterCondition(filterColumn, filterOperator, filterValue));
                }
            }
        }

        if (conditions.isEmpty()) {
            infoLabel.setText("No valid filter conditions to apply. Displaying all data.");
            currentDisplayedData = FXCollections.observableArrayList(loadedData);
            dataTable.setItems(currentDisplayedData);
            onUpdateChartClick();
            return;
        }

        ObservableList<Map<String, String>> finalFilteredData = FXCollections.observableArrayList();

        if ("AND".equals(globalLogic)) {
            ObservableList<Map<String, String>> currentFilteredResult = FXCollections.observableArrayList(loadedData);
            for (final FilterCondition condition : conditions) { // Use final for iteration variable
                currentFilteredResult = applySingleFilter(currentFilteredResult, condition);
                if (currentFilteredResult.isEmpty()) {
                    break;
                }
            }
            finalFilteredData.addAll(currentFilteredResult);
        } else if ("OR".equals(globalLogic)) {
            Set<Map<String, String>> uniqueRows = new HashSet<>();
            for (final FilterCondition condition : conditions) { // Use final for iteration variable
                ObservableList<Map<String, String>> resultOfSingleFilter = applySingleFilter(loadedData, condition);
                uniqueRows.addAll(resultOfSingleFilter);
            }
            finalFilteredData.addAll(uniqueRows);
        }

        currentDisplayedData = finalFilteredData;
        dataTable.setItems(currentDisplayedData);

        if (currentDisplayedData.isEmpty()) {
            infoLabel.setText("No rows match the applied filters with '" + globalLogic + "' logic.");
            chartContainer.getChildren().clear();
            saveChartButton.setDisable(true);
            exportDataButton.setDisable(true);
        } else {
            final int finalFilteredSize = currentDisplayedData.size(); // Use final local copy
            infoLabel.setText("Filters applied with '" + globalLogic + "' logic: " + finalFilteredSize + " rows match. Updating chart...");
            onUpdateChartClick();
        }
    }

    /**
     * Helper method to apply a single filter condition to a given list of data.
     * @param dataToFilter The list of data to apply the filter to.
     * @param condition The single filter condition to apply.
     * @return An ObservableList containing rows that match the single filter condition.
     */
    private ObservableList<Map<String, String>> applySingleFilter(List<Map<String, String>> dataToFilter, final FilterCondition condition) { // Use final for condition
        ObservableList<Map<String, String>> result = FXCollections.observableArrayList();
        final boolean isNumericColumn = isColumnLikelyNumeric(condition.column); // Use final

        try {
            double numericFilterValue = 0.0;
            if (isNumericColumn && !condition.operator.equals("contains")) {
                numericFilterValue = Double.parseDouble(condition.value);
            }
            final double finalNumericFilterValue = numericFilterValue; // Use final

            for (final Map<String, String> row : dataToFilter) { // Use final for iteration variable
                final String columnValue = row.get(condition.column); // Use final
                if (columnValue == null) {
                    continue;
                }

                boolean matches = false;
                if (condition.operator.equals("contains")) {
                    matches = columnValue.toLowerCase().contains(condition.value.toLowerCase());
                } else if (isNumericColumn) {
                    try {
                        final double cellNumericValue = Double.parseDouble(columnValue); // Use final
                        switch (condition.operator) {
                            case "=":
                                matches = (cellNumericValue == finalNumericFilterValue);
                                break;
                            case ">":
                                matches = (cellNumericValue > finalNumericFilterValue);
                                break;
                            case "<":
                                matches = (cellNumericValue < finalNumericFilterValue);
                                break;
                            case ">=":
                                matches = (cellNumericValue >= finalNumericFilterValue);
                                break;
                            case "<=":
                                matches = (cellNumericValue <= finalNumericFilterValue);
                                break;
                        }
                    } catch (NumberFormatException e) {
                        matches = false;
                    }
                }

                if (matches) {
                    result.add(row);
                }
            }
        } catch (NumberFormatException e) {
            infoLabel.setText("Error: Filter value '" + condition.value + "' is not a valid number for the selected numeric operator in filter for column '" + condition.column + "'.");
            return FXCollections.observableArrayList();
        }
        return result;
    }


    /**
     * Helper method to determine if a column is likely numeric by sampling its data.
     * This is a heuristic and might not be perfectly accurate for all data sets.
     * @param columnName The name of the column to check.
     * @return true if the column appears to contain mostly numeric data, false otherwise.
     */
    private boolean isColumnLikelyNumeric(final String columnName) { // Use final
        if (loadedData == null || loadedData.isEmpty()) {
            return false;
        }
        int numericCount = 0;
        int totalChecked = 0;
        final int sampleSize = Math.min(loadedData.size(), 100); // Use final

        for (int i = 0; i < sampleSize; i++) {
            final Map<String, String> row = loadedData.get(i); // Use final
            final String value = row.get(columnName); // Use final
            if (value != null && !value.trim().isEmpty()) {
                totalChecked++;
                if (NUMERIC_PATTERN.matcher(value.trim()).matches()) {
                    numericCount++;
                }
            }
        }
        return totalChecked > 0 && (double) numericCount / totalChecked >= 0.8;
    }


    /**
     * Handles the "Update Chart" button click event.
     * Validates user selections and calls the renderChart method.
     */
    @FXML
    protected void onUpdateChartClick() {
        final String categoryColumnHeader = categoryColumnComboBox.getSelectionModel().getSelectedItem(); // Use final
        final String valueColumnHeader = valueColumnComboBox.getSelectionModel().getSelectedItem(); // Use final
        final String selectedChartType = chartTypeComboBox.getSelectionModel().getSelectedItem(); // Use final
        final String selectedAggregationType = aggregationTypeComboBox.getSelectionModel().getSelectedItem(); // Use final

        final String customChartTitle = chartTitleField.getText(); // Use final
        final String customXAxisLabel = xAxisLabelField.getText(); // Use final
        final String customYAxisLabel = yAxisLabelField.getText(); // Use final


        // --- Input Validation ---
        if (currentDisplayedData == null || currentDisplayedData.isEmpty()) {
            infoLabel.setText("No data available to chart. Please load a CSV file or adjust filter.");
            chartContainer.getChildren().clear();
            saveChartButton.setDisable(true);
            exportDataButton.setDisable(true);
            return;
        }

        if (categoryColumnHeader == null || categoryColumnHeader.isEmpty()) {
            infoLabel.setText("Please select a column for the X-Axis (Category) or Latitude.");
            chartContainer.getChildren().clear();
            saveChartButton.setDisable(true);
            exportDataButton.setDisable(true);
            return;
        }

        if (valueColumnHeader == null || valueColumnHeader.isEmpty()) {
            infoLabel.setText("Please select a column for the Y-Axis (Value) or Longitude.");
            chartContainer.getChildren().clear();
            saveChartButton.setDisable(true);
            exportDataButton.setDisable(true);
            return;
        }

        if (selectedChartType == null || selectedChartType.isEmpty()) {
            infoLabel.setText("Please select a Chart Type.");
            chartContainer.getChildren().clear();
            saveChartButton.setDisable(true);
            exportDataButton.setDisable(true);
            return;
        }

        // Specific validation for Geographic Scatter Plot
        if ("Geographic Scatter Plot".equals(selectedChartType)) {
            if (!isColumnLikelyNumeric(categoryColumnHeader)) {
                infoLabel.setText("For Geographic Scatter Plot, the X-Axis (Latitude) column must contain numeric data.");
                chartContainer.getChildren().clear();
                saveChartButton.setDisable(true);
                exportDataButton.setDisable(true);
                return;
            }
            if (!isColumnLikelyNumeric(valueColumnHeader)) {
                infoLabel.setText("For Geographic Scatter Plot, the Y-Axis (Longitude) column must contain numeric data.");
                chartContainer.getChildren().clear();
                saveChartButton.setDisable(true);
                exportDataButton.setDisable(true);
                return;
            }
            // Aggregation is not typically used directly for geographic scatter points, but can be used for point size/color.
            // For now, we'll just ensure it's not "None" if we intend to use it for visual encoding later.
            // If "None" is selected, we'll just plot points without additional encoding.
            aggregationTypeComboBox.setDisable(true);
            aggregationTypeComboBox.getSelectionModel().select("None"); // Force "None" for Geo plots
        } else { // Standard chart types (Bar, Line, Scatter, Pie)
            aggregationTypeComboBox.setDisable(false); // Re-enable for other chart types

            if (selectedAggregationType == null || selectedAggregationType.isEmpty()) {
                infoLabel.setText("Please select an Aggregation Type.");
                chartContainer.getChildren().clear();
                saveChartButton.setDisable(true);
                exportDataButton.setDisable(true);
                return;
            }

            if ("Pie Chart".equals(selectedChartType) && "None".equals(selectedAggregationType)) {
                infoLabel.setText("Pie charts require aggregation (Sum, Average, or Count). 'None' is not suitable.");
                chartContainer.getChildren().clear();
                saveChartButton.setDisable(true);
                exportDataButton.setDisable(true);
                return;
            }

            if (!"Pie Chart".equals(selectedChartType) && "None".equals(selectedAggregationType) && !isColumnLikelyNumeric(valueColumnHeader)) {
                infoLabel.setText("For Bar, Line, or Scatter charts with 'None' aggregation, the Y-Axis (Value) column must contain numeric data. Please select a numeric column or a different aggregation type.");
                chartContainer.getChildren().clear();
                saveChartButton.setDisable(true);
                exportDataButton.setDisable(true);
                return;
            }

            if (categoryColumnHeader.equals(valueColumnHeader)) {
                infoLabel.setText("X-Axis and Y-Axis columns cannot be the same. Please select different columns.");
                chartContainer.getChildren().clear();
                saveChartButton.setDisable(true);
                exportDataButton.setDisable(true);
                return;
            }
        }


        // Call the generic renderChart method with currentDisplayedData
        renderChart(selectedChartType, currentDisplayedData, categoryColumnHeader, valueColumnHeader,
                selectedAggregationType, customChartTitle, customXAxisLabel, customYAxisLabel);

        infoLabel.setText("Chart updated: " + selectedChartType + " with X: '" + categoryColumnHeader + "', Y: '" + valueColumnHeader + "', Aggregation: '" + selectedAggregationType + "'.");
        saveChartButton.setDisable(false);
        exportDataButton.setDisable(false);
    }

    /**
     * Handles the "Save Chart as PNG" button click event.
     * Takes a snapshot of the current chart and saves it as a PNG file.
     */
    @FXML
    protected void onSaveChartClick() {
        if (chartContainer.getChildren().isEmpty()) {
            infoLabel.setText("No chart to save. Please generate a chart first.");
            return;
        }

        final Node chartNode = chartContainer.getChildren().get(0); // Use final
        // Allow saving of StackPane directly if it contains the map and points
        if (!(chartNode instanceof XYChart) && !(chartNode instanceof PieChart) && !(chartNode instanceof StackPane)) {
            infoLabel.setText("Cannot save: The displayed element is not a recognized chart type or container.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Chart as PNG");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png");
        fileChooser.getExtensionFilters().add(extFilter);

        String defaultFileName = "";
        if (chartNode instanceof XYChart) {
            defaultFileName = ((XYChart) chartNode).getTitle();
        } else if (chartNode instanceof PieChart) {
            defaultFileName = ((PieChart) chartNode).getTitle();
        } else if (chartNode instanceof StackPane && chartNode.getId() != null && chartNode.getId().equals("geographicMapContainer")) {
            defaultFileName = "Geographic_Scatter_Plot"; // Default for map
        }


        if (defaultFileName == null || defaultFileName.trim().isEmpty()) {
            defaultFileName = "chart_" + System.currentTimeMillis();
        } else {
            defaultFileName = defaultFileName.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
        }
        fileChooser.setInitialFileName(defaultFileName + ".png");

        final Stage stage = (Stage) saveChartButton.getScene().getWindow(); // Use final
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                WritableImage writableImage = new WritableImage(
                        (int) chartNode.getBoundsInLocal().getWidth(),
                        (int) chartNode.getBoundsInLocal().getHeight()
                );
                chartNode.snapshot(null, writableImage);

                // Check if the image is empty or invalid before writing
                if (writableImage.getWidth() > 0 && writableImage.getHeight() > 0) {
                    ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", file);
                    infoLabel.setText("Chart saved successfully to: " + file.getAbsolutePath());
                } else {
                    infoLabel.setText("Error saving chart: Generated image is empty or invalid.");
                    System.err.println("Error saving chart: WritableImage dimensions are 0 or less.");
                }
            } catch (IOException ex) {
                infoLabel.setText("Error saving chart: " + ex.getMessage() + ". Check file permissions or disk space.");
                System.err.println("IOException saving chart: " + ex.getMessage());
                ex.printStackTrace();
            } catch (Exception ex) {
                infoLabel.setText("An unexpected error occurred while saving the chart: " + ex.getMessage());
                System.err.println("Unexpected error saving chart: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            infoLabel.setText("Chart save cancelled.");
        }
    }

    /**
     * Handles the "Export Data to CSV" button click event.
     * Exports the current displayed data (raw or filtered) to a new CSV file.
     */
    @FXML
    protected void onExportDataClick() {
        if (currentDisplayedData == null || currentDisplayedData.isEmpty()) {
            infoLabel.setText("No data available to export.");
            return;
        }

        if (columnHeaders == null || columnHeaders.length == 0) {
            infoLabel.setText("Cannot export: No column headers found.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Data to CSV");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
        fileChooser.getExtensionFilters().add(extFilter);

        fileChooser.setInitialFileName("exported_data_" + System.currentTimeMillis() + ".csv");

        final Stage stage = (Stage) exportDataButton.getScene().getWindow(); // Use final
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(String.join(",", columnHeaders));
                writer.newLine();

                for (final Map<String, String> row : currentDisplayedData) { // Use final for iteration variable
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < columnHeaders.length; i++) {
                        final String header = columnHeaders[i]; // Use final
                        String value = row.getOrDefault(header, "");
                        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                            value = "\"" + value.replace("\"", "\"\"") + "\"";
                        }
                        sb.append(value);
                        if (i < columnHeaders.length - 1) {
                            sb.append(",");
                        }
                    }
                    writer.write(sb.toString());
                    writer.newLine();
                }
                infoLabel.setText("Data exported successfully to: " + file.getAbsolutePath());
            } catch (IOException ex) {
                infoLabel.setText("Error exporting data: " + ex.getMessage());
                System.err.println("Error exporting data: " + ex.getMessage());
                ex.printStackTrace();
            } catch (Exception ex) {
                infoLabel.setText("An unexpected error occurred while exporting data: " + ex.getMessage());
                System.err.println("Unexpected error exporting data: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            infoLabel.setText("Data export cancelled.");
        }
    }


    /**
     * Renders the chart based on the selected type, data, column headers, aggregation type,
     * and user-defined chart customization.
     * This method dynamically creates and displays the appropriate chart in the chartContainer.
     *
     * @param chartType The type of chart to render (e.g., "Bar Chart", "Line Chart", "Scatter Chart", "Pie Chart").
     * @param dataToChart The list of maps representing the data to be charted (can be raw or filtered).
     * @param categoryColumnHeader The header for the column to be used as the X-axis (category for XY, slice name for Pie, Latitude for Geo).
     * @param valueColumnHeader The header for the column to be used as the Y-axis (value for XY, slice value for Pie, Longitude for Geo).
     * @param aggregationType The type of aggregation to perform (e.g., "None", "Sum", "Average", "Count").
     * @param customChartTitle The user-defined title for the chart.
     * @param customXAxisLabel The user-defined label for the X-axis.
     * @param customYAxisLabel The user-defined label for the Y-axis.
     */
    private void renderChart(final String chartType, final List<Map<String, String>> dataToChart,
                             final String categoryColumnHeader, final String valueColumnHeader,
                             final String aggregationType, final String customChartTitle,
                             final String customXAxisLabel, final String customYAxisLabel) {
        chartContainer.getChildren().clear();

        Map<String, Double> aggregatedDataForChart = new LinkedHashMap<>();
        Map<String, Integer> counts = new HashMap<>();

        // Aggregation logic is only applied to non-geographic charts
        if (!"Geographic Scatter Plot".equals(chartType)) {
            for (final Map<String, String> row : dataToChart) { // Use final
                final String category = row.get(categoryColumnHeader); // Use final
                final String valueStr = row.get(valueColumnHeader); // Use final

                if (category == null || category.isEmpty()) {
                    System.err.println("Skipping row due to missing category for aggregation: " + row);
                    continue;
                }

                double numericValue = 0.0;
                boolean isNumeric = false;
                if (valueStr != null && !valueStr.isEmpty()) {
                    try {
                        numericValue = Double.parseDouble(valueStr);
                        isNumeric = true;
                    } catch (NumberFormatException e) {
                        System.err.println("Non-numeric value encountered for aggregation: '" + valueStr + "' in column '" + valueColumnHeader + "'. Skipping for sum/average.");
                    }
                }
                final double finalNumericValue = numericValue; // Use final

                aggregatedDataForChart.putIfAbsent(category, 0.0);
                counts.putIfAbsent(category, 0);

                if (isNumeric) {
                    switch (aggregationType) {
                        case "Sum":
                        case "Average":
                            aggregatedDataForChart.put(category, aggregatedDataForChart.get(category) + finalNumericValue);
                            counts.put(category, counts.get(category) + 1);
                            break;
                        case "Count":
                            counts.put(category, counts.get(category) + 1);
                            aggregatedDataForChart.put(category, (double)counts.get(category));
                            break;
                        case "None":
                            if (chartType.equals("Scatter Chart") || chartType.equals("Line Chart")) {
                                aggregatedDataForChart.put(category + "_" + System.nanoTime(), finalNumericValue);
                            } else {
                                aggregatedDataForChart.put(category, finalNumericValue);
                            }
                            break;
                    }
                } else if ("Count".equals(aggregationType) && valueStr != null && !valueStr.isEmpty()) {
                    counts.put(category, counts.get(category) + 1);
                    aggregatedDataForChart.put(category, (double)counts.get(category));
                } else if ("None".equals(aggregationType) && valueStr != null && !valueStr.isEmpty()) {
                    System.err.println("Skipping non-numeric Y-value for 'None' aggregation (only numeric Y-values are plotted): " + valueStr);
                }
            }

            if ("Average".equals(aggregationType)) {
                for (final Map.Entry<String, Double> entry : aggregatedDataForChart.entrySet()) { // Use final
                    final String category = entry.getKey(); // Use final
                    final int count = counts.getOrDefault(category, 0); // Use final
                    if (count > 0) {
                        aggregatedDataForChart.put(category, entry.getValue() / count);
                    } else {
                        aggregatedDataForChart.put(category, 0.0);
                    }
                }
            }
        }


        Node newChartNode = null;

        if ("Pie Chart".equals(chartType)) {
            if (aggregatedDataForChart.isEmpty()) {
                infoLabel.setText("No valid data points generated for the Pie Chart with current selections and filter. Check console for details.");
                saveChartButton.setDisable(true);
                exportDataButton.setDisable(true);
                return;
            }
            PieChart pieChart = new PieChart();
            pieChart.setTitle(customChartTitle != null && !customChartTitle.isEmpty() ? customChartTitle : "Pie Chart of " + valueColumnHeader + " by " + categoryColumnHeader + " (" + aggregationType + ")");

            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            aggregatedDataForChart.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()))); // entry is final by default

            pieChart.setData(pieChartData);
            newChartNode = pieChart;

        } else if ("Geographic Scatter Plot".equals(chartType)) {
            // Geographic Scatter Plot
            final StackPane geographicMapContainer = new StackPane(); // Use final
            geographicMapContainer.setId("geographicMapContainer"); // Set an ID for easier reference for saving
            // Bind the preferred size of the StackPane to its parent's size
            geographicMapContainer.prefWidthProperty().bind(chartContainer.widthProperty());
            geographicMapContainer.prefHeightProperty().bind(chartContainer.heightProperty());

            // Load the map image from resources
            Image mapImage = null;
            ImageView mapView = null;
            try {
                mapImage = new Image(getClass().getResourceAsStream("/com/drishti/dataviztool/world_map.png"));
                if (mapImage != null && !mapImage.isError()) {
                    mapView = new ImageView(mapImage);
                    mapView.fitWidthProperty().bind(geographicMapContainer.widthProperty());
                    mapView.fitHeightProperty().bind(geographicMapContainer.heightProperty());
                    mapView.setPreserveRatio(false); // Stretch to fit container
                    geographicMapContainer.getChildren().add(mapView);
                } else {
                    System.err.println("Error loading map image: " + (mapImage != null ? mapImage.getException().getMessage() : "Image is null."));
                    infoLabel.setText("Error loading map image. Using plain background. Make sure 'world_map.png' is in resources.");
                    geographicMapContainer.setStyle("-fx-background-color: #ADD8E6;"); // Fallback to plain background
                }
            } catch (Exception e) {
                System.err.println("Exception loading map image: " + e.getMessage());
                infoLabel.setText("Error loading map image. Using plain background. Make sure 'world_map.png' is in resources.");
                geographicMapContainer.setStyle("-fx-background-color: #ADD8E6;"); // Fallback to plain background
            }


            // Find min/max for scaling point size based on valueColumnHeader (Population)
            double minVal = Double.MAX_VALUE;
            double maxVal = Double.MIN_VALUE;
            boolean hasNumericValueColumnForScaling = false;

            for (final Map<String, String> row : dataToChart) { // Use final
                final String valStr = row.get(valueColumnHeader); // Use final
                if (valStr != null && !valStr.isEmpty() && NUMERIC_PATTERN.matcher(valStr.trim()).matches()) {
                    try {
                        final double val = Double.parseDouble(valStr); // Use final
                        if (val < minVal) minVal = val;
                        if (val > maxVal) maxVal = val;
                        hasNumericValueColumnForScaling = true;
                    } catch (NumberFormatException e) {
                        // Ignore non-numeric values for scaling
                    }
                }
            }

            final double finalMinVal = minVal; // Use final
            final double finalMaxVal = maxVal; // Use final
            final boolean finalHasNumericValueColumnForScaling = hasNumericValueColumnForScaling; // Use final
            final double minRadius = 3; // Use final
            final double maxRadius = 15; // Use final

            // Add a listener to the geographicMapContainer's width and height properties
            // to re-plot points when the container size changes.
            geographicMapContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0) {
                    replotGeographicPoints(geographicMapContainer, dataToChart, categoryColumnHeader, valueColumnHeader, finalMinVal, finalMaxVal, minRadius, maxRadius, finalHasNumericValueColumnForScaling);
                }
            });
            geographicMapContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0) {
                    replotGeographicPoints(geographicMapContainer, dataToChart, categoryColumnHeader, valueColumnHeader, finalMinVal, finalMaxVal, minRadius, maxRadius, finalHasNumericValueColumnForScaling);
                }
            });

            // Initial plotting of points
            replotGeographicPoints(geographicMapContainer, dataToChart, categoryColumnHeader, valueColumnHeader, finalMinVal, finalMaxVal, minRadius, maxRadius, finalHasNumericValueColumnForScaling);

            newChartNode = geographicMapContainer;

        } else { // XY Charts (Bar, Line, Scatter)
            if (aggregatedDataForChart.isEmpty()) {
                infoLabel.setText("No valid data points generated for the chart with current selections and filter. Check console for details.");
                saveChartButton.setDisable(true);
                exportDataButton.setDisable(true);
                return;
            }

            CategoryAxis newXAxis = new CategoryAxis();
            NumberAxis newYAxis = new NumberAxis();

            newXAxis.setLabel(customXAxisLabel != null && !customXAxisLabel.isEmpty() ? customXAxisLabel : categoryColumnHeader);
            final String defaultYAxisLabel = valueColumnHeader + ("None".equals(aggregationType) ? "" : " (" + aggregationType + ")"); // Use final
            newYAxis.setLabel(customYAxisLabel != null && !customYAxisLabel.isEmpty() ? customYAxisLabel : defaultYAxisLabel);

            XYChart<String, Number> xyChart = null;
            switch (chartType) {
                case "Bar Chart":
                    xyChart = new BarChart<>(newXAxis, newYAxis);
                    break;
                case "Line Chart":
                    xyChart = new LineChart<>(newXAxis, newYAxis);
                    break;
                case "Scatter Chart":
                    xyChart = new ScatterChart<>(newXAxis, newYAxis);
                    break;
            }

            final String defaultChartTitle = chartType + " of " + valueColumnHeader + " by " + categoryColumnHeader + ("None".equals(aggregationType) ? "" : " (" + aggregationType + ")"); // Use final
            xyChart.setTitle(customChartTitle != null && !customChartTitle.isEmpty() ? customChartTitle : defaultChartTitle);
            xyChart.setLegendVisible(false);

            final XYChart<String, Number> finalXyChart = xyChart; // Use final for lambda

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(valueColumnHeader + ("None".equals(aggregationType) ? "" : " (" + aggregationType + ")"));

            // --- Dynamic width adjustment for XY Charts with many categories to enable scrolling ---
            if (chartType.equals("Bar Chart") || chartType.equals("Line Chart") || chartType.equals("Scatter Chart")) {
                final int numCategories = aggregatedDataForChart.size(); // Use final
                final double minCategoryWidth = 50.0; // Use final
                final double calculatedPrefWidth = numCategories * minCategoryWidth; // Use final

                // Ensure the chart is wide enough to prevent labels/bars from overlapping, enabling ScrollPane
                // We're setting prefWidth on the chart itself, not the StackPane or ScrollPane
                final double defaultChartContainerWidth = 600.0; // Use final
                if (calculatedPrefWidth > defaultChartContainerWidth) {
                    finalXyChart.setPrefWidth(calculatedPrefWidth);
                    // Also adjust the X-axis tick label rotation if too many categories
                    if (numCategories > 10) { // Arbitrary threshold
                        newXAxis.setTickLabelRotation(90);
                    } else {
                        newXAxis.setTickLabelRotation(0);
                    }
                } else {
                    // Reset prefWidth to allow it to fit container if it's not too wide
                    finalXyChart.setPrefWidth(Region.USE_COMPUTED_SIZE); // Let parent decide, or set to a default if needed
                    newXAxis.setTickLabelRotation(0);
                }
            }
            // --- END Dynamic adjustment ---


            // --- DEBUGGING OUTPUT FOR CHART DATA ---
            System.out.println("\n--- Chart Data after Aggregation/Processing for " + chartType + " ---");
            if (aggregatedDataForChart.isEmpty()) {
                System.out.println("Aggregated data map is EMPTY. No data to plot.");
            } else {
                aggregatedDataForChart.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            System.out.println("Category: " + entry.getKey() + ", Value: " + entry.getValue());
                            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                        });
            }
            System.out.println("------------------------------------------------------------------\n");
            // --- END DEBUGGING OUTPUT ---


            if (!series.getData().isEmpty()) {
                finalXyChart.getData().add(series);
            } else {
                infoLabel.setText("No valid data found to plot the chart with current selections and filter. Check console for details.");
            }
            newChartNode = finalXyChart;
        }

        if (newChartNode != null) {
            chartContainer.getChildren().add(newChartNode);
            saveChartButton.setDisable(false); // Enable save button if chart is rendered
        } else {
            infoLabel.setText("Failed to create chart. Please check selections.");
            saveChartButton.setDisable(true); // Disable save button on failure
        }
    }

    /**
     * Helper method to replot geographic points when the map container resizes.
     * This avoids having to recreate the entire StackPane and ImageView.
     */
    private void replotGeographicPoints(final StackPane geographicMapContainer, final List<Map<String, String>> dataToChart,
                                        final String categoryColumnHeader, final String valueColumnHeader,
                                        final double minVal, final double maxVal, final double minRadius, final double maxRadius,
                                        final boolean hasNumericValueColumnForScaling) {
        // Remove existing data points (Circles), but keep the map image
        geographicMapContainer.getChildren().removeIf(node -> node instanceof Circle);

        final double currentMapWidth = geographicMapContainer.getWidth(); // Use final
        final double currentMapHeight = geographicMapContainer.getHeight(); // Use final

        if (currentMapWidth <= 0 || currentMapHeight <= 0) {
            // Cannot plot if container has no dimensions yet
            return;
        }

        for (final Map<String, String> row : dataToChart) { // Use final
            final String latStr = row.get(categoryColumnHeader); // Use final
            final String lonStr = row.get(valueColumnHeader);    // Use final
            final String valStr = row.get(valueColumnHeader);    // Use final

            if (latStr == null || latStr.isEmpty() || lonStr == null || lonStr.isEmpty()) {
                continue; // Skip rows with missing coordinates
            }

            try {
                double latitude = Double.parseDouble(latStr);
                double longitude = Double.parseDouble(lonStr);

                // Clamp values to valid ranges
                latitude = Math.max(-90.0, Math.min(90.0, latitude));
                longitude = Math.max(-180.0, Math.min(180.0, longitude));

                // Calculate X position: (longitude + 180) / 360 * mapWidth
                final double xPos = ((longitude + 180.0) / 360.0) * currentMapWidth; // Use final

                // Calculate Y position: (90 - latitude) / 180 * mapHeight (inverted for JavaFX Y-axis)
                final double yPos = ((90.0 - latitude) / 180.0) * currentMapHeight; // Use final

                double currentRadius = minRadius;
                if (hasNumericValueColumnForScaling && maxVal > minVal) {
                    try {
                        final double val = Double.parseDouble(valStr); // Use final
                        // Ensure no division by zero if maxVal == minVal
                        if (maxVal != minVal) {
                            currentRadius = minRadius + ((val - minVal) / (maxVal - minVal)) * (maxRadius - minRadius);
                        } else {
                            currentRadius = minRadius; // All values are the same, use minRadius
                        }
                    } catch (NumberFormatException e) {
                        // Keep minRadius if value is not numeric for scaling
                    }
                }

                Circle dataPoint = new Circle(currentRadius, Color.RED);
                dataPoint.setTranslateX(xPos - currentMapWidth / 2); // Adjust for StackPane center alignment
                dataPoint.setTranslateY(yPos - currentMapHeight / 2); // Adjust for StackPane center alignment
                dataPoint.setStroke(Color.BLACK);
                dataPoint.setStrokeWidth(1);
                dataPoint.setOpacity(0.7);

                geographicMapContainer.getChildren().add(dataPoint);

            } catch (NumberFormatException e) {
                System.err.println("Skipping non-numeric Latitude/Longitude/Value during replot: Lat='" + latStr + "', Lon='" + lonStr + "', Val='" + valStr + "'");
            }
        }
    }


    /**
     * Helper method to clear data and disable UI controls when no file is loaded or an error occurs.
     */
    private void clearAndDisableUI() {
        loadedData = null;
        currentDisplayedData = FXCollections.observableArrayList();
        columnHeaders = null;

        dataTable.getColumns().clear();
        dataTable.getItems().clear();
        chartContainer.getChildren().clear();

        categoryColumnComboBox.getItems().clear();
        valueColumnComboBox.getItems().clear();
        chartTypeComboBox.getItems().clear();
        aggregationTypeComboBox.getItems().clear();
        // filterColumnComboBox.getItems().clear(); // Removed as multi-filter UI handles this
        // filterOperatorComboBox.getItems().clear(); // Removed as multi-filter UI handles this
        filterRowsContainer.getChildren().clear(); // Clear dynamic filter rows

        chartTitleField.setText("");
        xAxisLabelField.setText("");
        yAxisLabelField.setText("");
        // filterValueField.setText(""); // Removed as multi-filter UI handles this

        enableUIControls(false);
    }

    /**
     * Helper method to enable or disable relevant UI controls.
     * @param enable true to enable, false to disable.
     */
    private void enableUIControls(final boolean enable) { // Use final
        categoryColumnComboBox.setDisable(!enable);
        valueColumnComboBox.setDisable(!enable);
        chartTypeComboBox.setDisable(!enable);
        // Aggregation combo box is handled specifically in onUpdateChartClick based on chart type
        // aggregationTypeComboBox.setDisable(!enable); // Keep this commented out, handled by listener
        chartTitleField.setDisable(!enable);
        xAxisLabelField.setDisable(!enable);
        yAxisLabelField.setDisable(!enable);
        saveChartButton.setDisable(!enable);
        exportDataButton.setDisable(!enable);
        delimiterComboBox.setDisable(!enable);
        addFilterButton.setDisable(true); // Always disable filter buttons until data is loaded
        clearAllFiltersButton.setDisable(true); // Always disable filter buttons until data is loaded
        globalAndOrComboBox.setDisable(true); // Always disable global AND/OR until data is loaded

        // Reset axis labels to default when disabling, or if no file is loaded
        if (!enable) {
            xAxisLabel.setText("X-Axis (Category):");
            yAxisLabel.setText("Y-Axis (Value):");
        }
    }
}

