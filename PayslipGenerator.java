import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;

public class PayslipGenerator extends Application {

    public static class Employee {
        private String empId;
        private String name;
        private String dept;
        private String design;
        private double basicSalary;
        private String panNumber;
        private HashMap<String, Double> monthlySalary = new HashMap<>();

        public Employee(String empId, String name, String dept, String design, double salary, String panNumber) {
            this.empId = empId;
            this.name = name;
            this.dept = dept;
            this.design = design;
            this.basicSalary = salary;
            this.panNumber = panNumber;
        }

        public String getEmpId() { return empId; }
        public String getName() { return name; }
        public String getDept() { return dept; }
        public String getDesign() { return design; }
        public double getBasicSalary() { return basicSalary; }
        public String getPanNumber() { return panNumber; }

        public double getNetSalary() {
            double pf = 0.12 * basicSalary;
            double hra = 0.20 * basicSalary;
            double da = 0.10 * basicSalary;
            return basicSalary + hra + da - pf;
        }

        public double getSalary(String month, int year) {
            String key = month + "-" + year;
            return monthlySalary.getOrDefault(key, basicSalary);
        }

        public void setMonthlySalary(String month, int year, double amount) {
            monthlySalary.put(month + "-" + year, amount);
        }

        public String generateMonthlyPayslip(String month, int year) {
            double salary = getSalary(month, year);
            double pf = 0.12 * salary;
            double hra = 0.20 * salary;
            double da = 0.10 * salary;
            double net = salary + hra + da - pf;

            String filename = empId + "_" + month + "_" + year + "_Payslip.txt";

            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {

                writer.println("------ PAYSLIP (" + month + " " + year + ") ------");
                writer.println("Employee ID   : " + empId);
                writer.println("Employee Name : " + name);
                writer.println("Department    : " + dept);
                writer.println("Designation   : " + design);
                writer.println("PAN Number    : " + panNumber);
                writer.println("Basic Salary  : " + salary);
                writer.println("PF  (12%)     : " + pf);
                writer.println("HRA (20%)     : " + hra);
                writer.println("DA  (10%)     : " + da);
                writer.println("Net Salary    : " + net);
                writer.println("----------------------------------------");

                return "Success: Payslip generated as " + filename;

            } catch (Exception e) {
                return "Error: Could not generate payslip file. " + e.getMessage();
            }
        }
    }

    private ObservableList<Employee> employeeList = FXCollections.observableArrayList();
    private ObservableList<String> departmentList = FXCollections.observableArrayList("CSE", "ECE", "IT");
    private TableView<Employee> table = new TableView<>();
    private TextField tfSearch = new TextField();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        employeeList.add(new Employee("CSE101", "Arun", "CSE", "Asst. Prof", 50000, "ABCDE1234F"));
        employeeList.add(new Employee("ECE101", "Bala", "ECE", "Professor", 55000, "PQRSX9876Z"));
        employeeList.add(new Employee("IT101", "Priya", "IT", "Asst. Prof", 70000, "LMNOP5432Q"));

        BorderPane root = new BorderPane();
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab empTab = new Tab("Staff", createEmployeeView());
        Tab deptTab = new Tab("Departments", createDepartmentView());

        tabPane.getTabs().addAll(empTab, deptTab);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 950, 650);
        primaryStage.setTitle("Payslip Generator for Staff");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createEmployeeView() {
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));

        tfSearch.setPromptText("Search by Name or ID...");
        tfSearch.setPrefWidth(200);

        ComboBox<String> cbDeptFilter = new ComboBox<>(departmentList);
        cbDeptFilter.setPromptText("Filter Dept");

        TextField tfMinSalary = new TextField();
        tfMinSalary.setPromptText("Min Salary");
        tfMinSalary.setPrefWidth(100);

        TextField tfMaxSalary = new TextField();
        tfMaxSalary.setPromptText("Max Salary");
        tfMaxSalary.setPrefWidth(100);

        Button btnClearFilter = new Button("Clear Filters");

        topBar.getChildren().addAll(
                new Label("Search:"), tfSearch,
                cbDeptFilter,
                new Label("Min Salary:"), tfMinSalary,
                new Label("Max Salary:"), tfMaxSalary,
                btnClearFilter
        );

        TableColumn<Employee, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("empId"));

        TableColumn<Employee, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Employee, String> colDept = new TableColumn<>("Dept");
        colDept.setCellValueFactory(new PropertyValueFactory<>("dept"));

        TableColumn<Employee, String> colDesign = new TableColumn<>("Designation");
        colDesign.setCellValueFactory(new PropertyValueFactory<>("design"));

        TableColumn<Employee, Double> colSalary = new TableColumn<>("Basic Salary");
        colSalary.setCellValueFactory(new PropertyValueFactory<>("basicSalary"));

        TableColumn<Employee, Double> colNet = new TableColumn<>("Net Salary (Est.)");
        colNet.setCellValueFactory(cell ->
                new SimpleDoubleProperty(cell.getValue().getNetSalary()).asObject());

        table.getColumns().setAll(colId, colName, colDept, colDesign, colSalary, colNet);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        FilteredList<Employee> filteredData = new FilteredList<>(employeeList, p -> true);
        table.setItems(filteredData);

        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(emp -> filterLogic(
                    emp, tfSearch.getText(), cbDeptFilter.getValue(),
                    tfMinSalary.getText(), tfMaxSalary.getText()));
        });

        cbDeptFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(emp -> filterLogic(
                    emp, tfSearch.getText(), cbDeptFilter.getValue(),
                    tfMinSalary.getText(), tfMaxSalary.getText()));
        });

        tfMinSalary.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(emp -> filterLogic(
                    emp, tfSearch.getText(), cbDeptFilter.getValue(),
                    tfMinSalary.getText(), tfMaxSalary.getText()));
        });

        tfMaxSalary.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(emp -> filterLogic(
                    emp, tfSearch.getText(), cbDeptFilter.getValue(),
                    tfMinSalary.getText(), tfMaxSalary.getText()));
        });

        btnClearFilter.setOnAction(e -> {
            tfSearch.clear();
            cbDeptFilter.getSelectionModel().clearSelection();
            tfMinSalary.clear();
            tfMaxSalary.clear();
            filteredData.setPredicate(emp -> true);
        });

        HBox actionBar = new HBox(10);
        actionBar.setPadding(new Insets(10));

        Button btnAdd = new Button("Add Employee");
        Button btnPayslip = new Button("Generate Payslip");
        Button btnUpdateSalary = new Button("Update Monthly Salary");

        btnAdd.setOnAction(e -> showAddEmployeeDialog());

        btnPayslip.setOnAction(e -> {
            Employee selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showPayslipDialog(selected);
            else showAlert("Warning", "Please select an employee.");
        });

        btnUpdateSalary.setOnAction(e -> {
            Employee selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showUpdateSalaryDialog(selected);
            else showAlert("Warning", "Please select an employee.");
        });

        actionBar.getChildren().addAll(btnAdd, btnPayslip, btnUpdateSalary);

        VBox layout = new VBox(10);
        layout.getChildren().addAll(topBar, table, actionBar);

        return layout;
    }

    private boolean filterLogic(Employee emp,
                                String searchText,
                                String deptFilter,
                                String minSalaryText,
                                String maxSalaryText) {

        boolean matchesSearch =
                (searchText == null || searchText.isEmpty()) ||
                        emp.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                        emp.getEmpId().toLowerCase().contains(searchText.toLowerCase());

        boolean matchesDept =
                (deptFilter == null) || emp.getDept().equalsIgnoreCase(deptFilter);

        double net = emp.getNetSalary();

        double min = 0;
        double max = Double.MAX_VALUE;

        try {
            if (minSalaryText != null && !minSalaryText.isEmpty()) {
                min = Double.parseDouble(minSalaryText);
            }
        } catch (Exception ignored) {}

        try {
            if (maxSalaryText != null && !maxSalaryText.isEmpty()) {
                max = Double.parseDouble(maxSalaryText);
            }
        } catch (Exception ignored) {}

        boolean matchesSalary = (net >= min && net <= max);

        return matchesSearch && matchesDept && matchesSalary;
    }

    private VBox createDepartmentView() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));

        ListView<String> deptListView = new ListView<>(departmentList);

        HBox inputBox = new HBox(10);
        TextField tfNewDept = new TextField();
        tfNewDept.setPromptText("New Department Name");
        Button btnAddDept = new Button("Add");

        btnAddDept.setOnAction(e -> {
            String newDept = tfNewDept.getText().trim();
            if (!newDept.isEmpty() && !departmentList.contains(newDept)) {
                departmentList.add(newDept);
                tfNewDept.clear();
            } else {
                showAlert("Error", "Invalid or Duplicate Department");
            }
        });

        inputBox.getChildren().addAll(tfNewDept, btnAddDept);
        layout.getChildren().addAll(new Label("Manage Departments"), deptListView, inputBox);
        return layout;
    }

    private void showAddEmployeeDialog() {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Employee");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField tfId = new TextField();
        TextField tfName = new TextField();
        ComboBox<String> cbDept = new ComboBox<>(departmentList);
        TextField tfDesign = new TextField();
        TextField tfSalary = new TextField();
        TextField tfPan = new TextField();

        tfPan.setPromptText("PAN Number");

        grid.add(new Label("ID:"), 0, 0); grid.add(tfId, 1, 0);
        grid.add(new Label("Name:"), 0, 1); grid.add(tfName, 1, 1);
        grid.add(new Label("Dept:"), 0, 2); grid.add(cbDept, 1, 2);
        grid.add(new Label("Designation:"), 0, 3); grid.add(tfDesign, 1, 3);
        grid.add(new Label("Salary:"), 0, 4); grid.add(tfSalary, 1, 4);
        grid.add(new Label("PAN Number:"), 0, 5); grid.add(tfPan, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {

                try {
                    double sal = Double.parseDouble(tfSalary.getText());

                    String deptValue = cbDept.getValue();
                    if (deptValue == null) {
                        showAlert("Error", "Please select a Department.");
                        return;
                    }

                    employeeList.add(new Employee(
                            tfId.getText(),
                            tfName.getText(),
                            deptValue,
                            tfDesign.getText(),
                            sal,
                            tfPan.getText()
                    ));

                    showAlert("Success", "Employee Added Successfully!");

                } catch (Exception ex) {
                    showAlert("Error", "Invalid Input!");
                }
            }
        });
    }

    private void showPayslipDialog(Employee e) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Generate Payslip");
        dialog.setHeaderText("Generate Payslip for " + e.getName());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> cbMonth = new ComboBox<>();
        cbMonth.getItems().addAll(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December");

        ComboBox<Integer> cbYear = new ComboBox<>();
        for (int y = 2015; y <= 2035; y++) cbYear.getItems().add(y);

        cbMonth.setPromptText("Select Month");
        cbYear.setPromptText("Select Year");

        grid.add(new Label("Month:"), 0, 0); grid.add(cbMonth, 1, 0);
        grid.add(new Label("Year:"), 0, 1); grid.add(cbYear, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String month = cbMonth.getValue();
                    Integer year = cbYear.getValue();

                    if (month == null || year == null) {
                        showAlert("Error", "Select both Month and Year.");
                        return;
                    }

                    String result = e.generateMonthlyPayslip(month, year);
                    showAlert("Payslip Status", result);

                } catch (Exception ex) {
                    showAlert("Error", "Something went wrong.");
                }
            }
        });
    }

    private void showUpdateSalaryDialog(Employee e) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Monthly Salary");
        dialog.setHeaderText("Override Basic Salary for " + e.getName());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField tfMonth = new TextField();
        TextField tfYear = new TextField();
        TextField tfAmount = new TextField();

        tfMonth.setPromptText("Enter Month");
        tfYear.setPromptText("Enter Year (2024..)");
        tfAmount.setPromptText("New Basic Salary");

        grid.add(new Label("Month:"), 0, 0); grid.add(tfMonth, 1, 0);
        grid.add(new Label("Year:"), 0, 1); grid.add(tfYear, 1, 1);
        grid.add(new Label("New Basic:"), 0, 2); grid.add(tfAmount, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {

                try {
                    int year = Integer.parseInt(tfYear.getText());
                    double amt = Double.parseDouble(tfAmount.getText());

                    e.setMonthlySalary(tfMonth.getText(), year, amt);

                    showAlert("Success", "Salary updated successfully!");
                } catch (Exception ex) {
                    showAlert("Error", "Invalid input.");
                }
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
