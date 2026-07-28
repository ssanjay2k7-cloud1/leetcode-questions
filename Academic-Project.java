import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class TaskManagementSystem {

    // ========================================================================
    //  CONSTANTS
    // ========================================================================
    private static final String DB_NAME = "task_manager.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_NAME;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy");

    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String RED = "\033[31m";
    private static final String GREEN = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String BLUE = "\033[34m";
    private static final String MAGENTA = "\033[35m";
    private static final String CYAN = "\033[36m";
    private static final String WHITE = "\033[37m";
    private static final String BG_BLUE = "\033[44m";
    private static final String BG_RED = "\033[41m";
    private static final String BG_GREEN = "\033[42m";
    private static final String BG_YELLOW = "\033[43m";

    private static Connection connection = null;
    private static Scanner scanner = new Scanner(System.in);

    // ========================================================================
    //  TASK MODEL CLASS
    // ========================================================================
    static class Task {
        int id;
        String title;
        String description;
        String category;
        String priority;    // HIGH, MEDIUM, LOW
        String status;      // PENDING, IN_PROGRESS, COMPLETED
        String dueDate;
        String createdDate;
        String completedDate;

        public Task() {}

        public Task(int id, String title, String description, String category,
                     String priority, String status, String dueDate,
                     String createdDate, String completedDate) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.category = category;
            this.priority = priority;
            this.status = status;
            this.dueDate = dueDate;
            this.createdDate = createdDate;
            this.completedDate = completedDate;
        }
    }

    // ========================================================================
    //  MAIN METHOD
    // ========================================================================
    public static void main(String[] args) {
        initializeDatabase();
        showWelcomeBanner();

        boolean running = true;
        while (running) {
            showMainMenu();
            int choice = getIntInput("Select Choice (1-9): ", 1, 9);

            switch (choice) {
                case 1: addTask(); break;
                case 2: viewAllTasks(); break;
                case 3: editTask(); break;
                case 4: deleteTask(); break;
                case 5: updateTaskStatus(); break;
                case 6: searchTasks(); break;
                case 7: filterTasks(); break;
                case 8: showStatistics(); break;
                case 9:
                    running = false;
                    showExitBanner();
                    break;
            }
        }
        closeDatabase();
        scanner.close();
    }

    // ========================================================================
    //  DATABASE INITIALIZATION
    // ========================================================================
    private static void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            Statement stmt = connection.createStatement();

            String createTable = "CREATE TABLE IF NOT EXISTS tasks ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "title TEXT NOT NULL, "
                    + "description TEXT, "
                    + "category TEXT DEFAULT 'General', "
                    + "priority TEXT DEFAULT 'MEDIUM', "
                    + "status TEXT DEFAULT 'PENDING', "
                    + "due_date TEXT, "
                    + "created_date TEXT NOT NULL, "
                    + "completed_date TEXT"
                    + ")";
            stmt.execute(createTable);
            stmt.close();
        } catch (ClassNotFoundException e) {
            printError("SQLite JDBC Driver not found! Please add sqlite-jdbc.jar to classpath.");
            printInfo("Download from: https://github.com/xerial/sqlite-jdbc/releases");
            System.exit(1);
        } catch (SQLException e) {
            printError("Database initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void closeDatabase() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            printError("Error closing database: " + e.getMessage());
        }
    }

    // ========================================================================
    //  UI - BANNERS & MENUS
    // ========================================================================
    private static void showWelcomeBanner() {
        System.out.println();
        System.out.println(CYAN + "╔══════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(CYAN + "║" + BOLD + WHITE + "            ✦  TASK MANAGEMENT SYSTEM  ✦                     " + RESET + CYAN + "║" + RESET);
        System.out.println(CYAN + "║" + WHITE + "        Organize  ·  Prioritize  ·  Accomplish               " + RESET + CYAN + "║" + RESET);
        System.out.println(CYAN + "╠══════════════════════════════════════════════════════════════╣" + RESET);
        System.out.println(CYAN + "║" + WHITE + "  Manage your tasks efficiently with powerful features:      " + RESET + CYAN + "║" + RESET);
        System.out.println(CYAN + "║" + WHITE + "  • Create, Edit & Delete Tasks                              " + RESET + CYAN + "║" + RESET);
        System.out.println(CYAN + "║" + WHITE + "  • Set Priorities & Categories                              " + RESET + CYAN + "║" + RESET);
        System.out.println(CYAN + "║" + WHITE + "  • Track Progress & Completion                              " + RESET + CYAN + "║" + RESET);
        System.out.println(CYAN + "║" + WHITE + "  • Search, Filter & Sort Tasks                              " + RESET + CYAN + "║" + RESET);
        System.out.println(CYAN + "║" + WHITE + "  • View Statistics Dashboard                                " + RESET + CYAN + "║" + RESET);
        System.out.println(CYAN + "╚══════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
    }

    private static void showMainMenu() {
        int[] counts = getTaskCounts();
        System.out.println();
        System.out.println(BLUE + "┌──────────────────────────────────────────┐" + RESET);
        System.out.println(BLUE + "│" + BOLD + WHITE + "           ★ MAIN MENU ★                 " + RESET + BLUE + "│" + RESET);
        System.out.println(BLUE + "├──────────────────────────────────────────┤" + RESET);
        System.out.println(BLUE + "│" + YELLOW + "  Tasks: " + WHITE + "Total=" + counts[0]
                + " │ Pending=" + counts[1]
                + " │ Done=" + counts[2] + "   " + RESET + BLUE + "│" + RESET);
        System.out.println(BLUE + "├──────────────────────────────────────────┤" + RESET);
        System.out.println(BLUE + "│" + GREEN + "  1." + WHITE + " Add New Task                        " + RESET + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + GREEN + "  2." + WHITE + " View All Tasks                      " + RESET + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + GREEN + "  3." + WHITE + " Edit Task                           " + RESET + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + GREEN + "  4." + WHITE + " Delete Task                         " + RESET + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + GREEN + "  5." + WHITE + " Update Task Status                  " + RESET + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + GREEN + "  6." + WHITE + " Search Tasks                        " + RESET + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + GREEN + "  7." + WHITE + " Filter & Sort Tasks                 " + RESET + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + GREEN + "  8." + WHITE + " Task Statistics Dashboard           " + RESET + BLUE + "│" + RESET);
        System.out.println(BLUE + "│" + RED + "  9." + WHITE + " Exit                                " + RESET + BLUE + "│" + RESET);
        System.out.println(BLUE + "└──────────────────────────────────────────┘" + RESET);
    }

    private static void showExitBanner() {
        System.out.println();
        System.out.println(CYAN + "╔══════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(CYAN + "║" + BOLD + WHITE + "       Thank you for using Task Management System!          " + RESET + CYAN + "║" + RESET);
        System.out.println(CYAN + "║" + WHITE + "              Your tasks have been saved.                     " + RESET + CYAN + "║" + RESET);
        System.out.println(CYAN + "║" + YELLOW + "                   Goodbye! ✦                                " + RESET + CYAN + "║" + RESET);
        System.out.println(CYAN + "╚══════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
    }

    private static void printSectionHeader(String title) {
        System.out.println();
        System.out.println(MAGENTA + "┌──────────────────────────────────────────┐" + RESET);
        System.out.println(MAGENTA + "│" + BOLD + WHITE + centerText(title, 42) + RESET + MAGENTA + "│" + RESET);
        System.out.println(MAGENTA + "└──────────────────────────────────────────┘" + RESET);
    }

    // ========================================================================
    //  FEATURE 1: ADD TASK
    // ========================================================================
    private static void addTask() {
        printSectionHeader("✚ ADD NEW TASK");

        System.out.print(CYAN + "  Enter Task Title: " + RESET);
        String title = scanner.nextLine().trim();
        if (title.isEmpty()) {
            printError("Task title cannot be empty!");
            return;
        }

        System.out.print(CYAN + "  Enter Description (or press Enter to skip): " + RESET);
        String description = scanner.nextLine().trim();
        if (description.isEmpty()) description = "No description";

        System.out.print(CYAN + "  Enter Category (default: General): " + RESET);
        String category = scanner.nextLine().trim();
        if (category.isEmpty()) category = "General";

        System.out.println(YELLOW + "  Priority Levels: " + RED + "[1] HIGH  " + YELLOW + "[2] MEDIUM  " + GREEN + "[3] LOW" + RESET);
        int priorityChoice = getIntInput("  Select Priority (1-3): ", 1, 3);
        String priority;
        switch (priorityChoice) {
            case 1: priority = "HIGH"; break;
            case 3: priority = "LOW"; break;
            default: priority = "MEDIUM"; break;
        }

        System.out.print(CYAN + "  Enter Due Date (yyyy-MM-dd) or press Enter to skip: " + RESET);
        String dueDate = scanner.nextLine().trim();
        if (!dueDate.isEmpty() && !isValidDate(dueDate)) {
            printWarning("Invalid date format. Due date will not be set.");
            dueDate = null;
        }
        if (dueDate != null && dueDate.isEmpty()) dueDate = null;

        String createdDate = DATE_FORMAT.format(new Date());

        try {
            String sql = "INSERT INTO tasks (title, description, category, priority, status, due_date, created_date) "
                    + "VALUES (?, ?, ?, ?, 'PENDING', ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, title);
            pstmt.setString(2, description);
            pstmt.setString(3, category);
            pstmt.setString(4, priority);
            pstmt.setString(5, dueDate);
            pstmt.setString(6, createdDate);
            pstmt.executeUpdate();
            pstmt.close();

            printSuccess("Task \"" + title + "\" added successfully!");
        } catch (SQLException e) {
            printError("Failed to add task: " + e.getMessage());
        }
    }

    // ========================================================================
    //  FEATURE 2: VIEW ALL TASKS
    // ========================================================================
    private static void viewAllTasks() {
        printSectionHeader("📋 ALL TASKS");
        List<Task> tasks = getAllTasks("SELECT * FROM tasks ORDER BY "
                + "CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 END, "
                + "due_date ASC");

        if (tasks.isEmpty()) {
            printInfo("No tasks found. Add a new task to get started!");
            return;
        }
        displayTaskTable(tasks);
    }

    // ========================================================================
    //  FEATURE 3: EDIT TASK
    // ========================================================================
    private static void editTask() {
        printSectionHeader("✎ EDIT TASK");

        List<Task> tasks = getAllTasks("SELECT * FROM tasks ORDER BY id ASC");
        if (tasks.isEmpty()) {
            printInfo("No tasks available to edit.");
            return;
        }
        displayTaskTable(tasks);

        int taskId = getIntInput("\n  Enter Task ID to edit: ", 1, Integer.MAX_VALUE);
        Task task = getTaskById(taskId);
        if (task == null) {
            printError("Task with ID " + taskId + " not found!");
            return;
        }

        System.out.println();
        System.out.println(YELLOW + "  Current values are shown in [brackets]. Press Enter to keep current value." + RESET);
        System.out.println();

        // Edit Title
        System.out.print(CYAN + "  Title [" + task.title + "]: " + RESET);
        String newTitle = scanner.nextLine().trim();
        if (newTitle.isEmpty()) newTitle = task.title;

        // Edit Description
        System.out.print(CYAN + "  Description [" + task.description + "]: " + RESET);
        String newDesc = scanner.nextLine().trim();
        if (newDesc.isEmpty()) newDesc = task.description;

        // Edit Category
        System.out.print(CYAN + "  Category [" + task.category + "]: " + RESET);
        String newCategory = scanner.nextLine().trim();
        if (newCategory.isEmpty()) newCategory = task.category;

        // Edit Priority
        System.out.println(YELLOW + "  Current Priority: " + colorPriority(task.priority) + RESET);
        System.out.println(YELLOW + "  Priority: " + RED + "[1] HIGH  " + YELLOW + "[2] MEDIUM  " + GREEN + "[3] LOW  " + WHITE + "[4] Keep Current" + RESET);
        int priorityChoice = getIntInput("  Select Priority (1-4): ", 1, 4);
        String newPriority;
        switch (priorityChoice) {
            case 1: newPriority = "HIGH"; break;
            case 2: newPriority = "MEDIUM"; break;
            case 3: newPriority = "LOW"; break;
            default: newPriority = task.priority; break;
        }

        // Edit Due Date
        System.out.print(CYAN + "  Due Date [" + (task.dueDate != null ? task.dueDate : "Not Set") + "] (yyyy-MM-dd): " + RESET);
        String newDueDate = scanner.nextLine().trim();
        if (newDueDate.isEmpty()) {
            newDueDate = task.dueDate;
        } else if (!isValidDate(newDueDate)) {
            printWarning("Invalid date format. Keeping current due date.");
            newDueDate = task.dueDate;
        }

        try {
            String sql = "UPDATE tasks SET title=?, description=?, category=?, priority=?, due_date=? WHERE id=?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, newTitle);
            pstmt.setString(2, newDesc);
            pstmt.setString(3, newCategory);
            pstmt.setString(4, newPriority);
            pstmt.setString(5, newDueDate);
            pstmt.setInt(6, taskId);
            pstmt.executeUpdate();
            pstmt.close();

            printSuccess("Task #" + taskId + " updated successfully!");
        } catch (SQLException e) {
            printError("Failed to update task: " + e.getMessage());
        }
    }

    // ========================================================================
    //  FEATURE 4: DELETE TASK
    // ========================================================================
    private static void deleteTask() {
        printSectionHeader("✖ DELETE TASK");

        List<Task> tasks = getAllTasks("SELECT * FROM tasks ORDER BY id ASC");
        if (tasks.isEmpty()) {
            printInfo("No tasks available to delete.");
            return;
        }
        displayTaskTable(tasks);

        int taskId = getIntInput("\n  Enter Task ID to delete: ", 1, Integer.MAX_VALUE);
        Task task = getTaskById(taskId);
        if (task == null) {
            printError("Task with ID " + taskId + " not found!");
            return;
        }

        System.out.println(YELLOW + "\n  Are you sure you want to delete: \"" + task.title + "\"?" + RESET);
        System.out.print(RED + "  Type 'YES' to confirm deletion: " + RESET);
        String confirm = scanner.nextLine().trim();

        if (confirm.equalsIgnoreCase("YES")) {
            try {
                String sql = "DELETE FROM tasks WHERE id = ?";
                PreparedStatement pstmt = connection.prepareStatement(sql);
                pstmt.setInt(1, taskId);
                pstmt.executeUpdate();
                pstmt.close();

                printSuccess("Task #" + taskId + " deleted successfully!");
            } catch (SQLException e) {
                printError("Failed to delete task: " + e.getMessage());
            }
        } else {
            printInfo("Deletion cancelled.");
        }
    }

    // ========================================================================
    //  FEATURE 5: UPDATE TASK STATUS
    // ========================================================================
    private static void updateTaskStatus() {
        printSectionHeader("⟳ UPDATE STATUS");

        List<Task> tasks = getAllTasks("SELECT * FROM tasks WHERE status != 'COMPLETED' ORDER BY id ASC");
        if (tasks.isEmpty()) {
            printInfo("No pending/in-progress tasks found.");
            return;
        }
        displayTaskTable(tasks);

        int taskId = getIntInput("\n  Enter Task ID to update status: ", 1, Integer.MAX_VALUE);
        Task task = getTaskById(taskId);
        if (task == null) {
            printError("Task with ID " + taskId + " not found!");
            return;
        }

        System.out.println(YELLOW + "\n  Current Status: " + colorStatus(task.status) + RESET);
        System.out.println(CYAN + "  Select New Status:" + RESET);
        System.out.println(YELLOW + "    [1] PENDING" + RESET);
        System.out.println(BLUE + "    [2] IN PROGRESS" + RESET);
        System.out.println(GREEN + "    [3] COMPLETED" + RESET);

        int statusChoice = getIntInput("  Select Status (1-3): ", 1, 3);
        String newStatus;
        String completedDate = null;

        switch (statusChoice) {
            case 1: newStatus = "PENDING"; break;
            case 2: newStatus = "IN_PROGRESS"; break;
            case 3:
                newStatus = "COMPLETED";
                completedDate = DATE_FORMAT.format(new Date());
                break;
            default: newStatus = task.status; break;
        }

        try {
            String sql = "UPDATE tasks SET status=?, completed_date=? WHERE id=?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, newStatus);
            pstmt.setString(2, completedDate);
            pstmt.setInt(3, taskId);
            pstmt.executeUpdate();
            pstmt.close();

            printSuccess("Task #" + taskId + " status updated to " + colorStatus(newStatus) + RESET);
        } catch (SQLException e) {
            printError("Failed to update status: " + e.getMessage());
        }
    }

    // ========================================================================
    //  FEATURE 6: SEARCH TASKS
    // ========================================================================
    private static void searchTasks() {
        printSectionHeader("🔍 SEARCH TASKS");

        System.out.print(CYAN + "  Enter search keyword: " + RESET);
        String keyword = scanner.nextLine().trim();
        if (keyword.isEmpty()) {
            printError("Search keyword cannot be empty!");
            return;
        }

        try {
            String sql = "SELECT * FROM tasks WHERE "
                    + "title LIKE ? OR description LIKE ? OR category LIKE ? "
                    + "ORDER BY id ASC";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            ResultSet rs = pstmt.executeQuery();

            List<Task> tasks = new ArrayList<>();
            while (rs.next()) {
                tasks.add(extractTask(rs));
            }
            rs.close();
            pstmt.close();

            if (tasks.isEmpty()) {
                printInfo("No tasks found matching \"" + keyword + "\".");
            } else {
                printSuccess("Found " + tasks.size() + " task(s) matching \"" + keyword + "\":");
                displayTaskTable(tasks);
            }
        } catch (SQLException e) {
            printError("Search failed: " + e.getMessage());
        }
    }

    // ========================================================================
    //  FEATURE 7: FILTER & SORT TASKS
    // ========================================================================
    private static void filterTasks() {
        printSectionHeader("⚙ FILTER & SORT");

        System.out.println(CYAN + "  Filter By:" + RESET);
        System.out.println(WHITE + "    [1] Status" + RESET);
        System.out.println(WHITE + "    [2] Priority" + RESET);
        System.out.println(WHITE + "    [3] Category" + RESET);
        System.out.println(WHITE + "    [4] Due Date (Overdue)" + RESET);
        System.out.println(WHITE + "    [5] Sort By Due Date" + RESET);
        System.out.println(WHITE + "    [6] Sort By Priority" + RESET);
        System.out.println(WHITE + "    [7] Sort By Created Date" + RESET);

        int choice = getIntInput("  Select Option (1-7): ", 1, 7);
        String sql = "";

        switch (choice) {
            case 1:
                System.out.println(CYAN + "  Status:" + RESET);
                System.out.println("    [1] PENDING  [2] IN PROGRESS  [3] COMPLETED");
                int statusChoice = getIntInput("  Select (1-3): ", 1, 3);
                String statusFilter;
                switch (statusChoice) {
                    case 1: statusFilter = "PENDING"; break;
                    case 2: statusFilter = "IN_PROGRESS"; break;
                    case 3: statusFilter = "COMPLETED"; break;
                    default: statusFilter = "PENDING"; break;
                }
                sql = "SELECT * FROM tasks WHERE status = '" + statusFilter + "' ORDER BY id ASC";
                break;

            case 2:
                System.out.println(CYAN + "  Priority:" + RESET);
                System.out.println("    [1] HIGH  [2] MEDIUM  [3] LOW");
                int prioChoice = getIntInput("  Select (1-3): ", 1, 3);
                String prioFilter;
                switch (prioChoice) {
                    case 1: prioFilter = "HIGH"; break;
                    case 2: prioFilter = "MEDIUM"; break;
                    case 3: prioFilter = "LOW"; break;
                    default: prioFilter = "MEDIUM"; break;
                }
                sql = "SELECT * FROM tasks WHERE priority = '" + prioFilter + "' ORDER BY id ASC";
                break;

            case 3:
                System.out.print(CYAN + "  Enter Category name: " + RESET);
                String catFilter = scanner.nextLine().trim();
                if (catFilter.isEmpty()) {
                    printError("Category cannot be empty!");
                    return;
                }
                sql = "SELECT * FROM tasks WHERE category LIKE '%" + catFilter + "%' ORDER BY id ASC";
                break;

            case 4:
                String today = DATE_FORMAT.format(new Date());
                sql = "SELECT * FROM tasks WHERE due_date < '" + today + "' AND status != 'COMPLETED' ORDER BY due_date ASC";
                break;

            case 5:
                sql = "SELECT * FROM tasks ORDER BY due_date ASC";
                break;

            case 6:
                sql = "SELECT * FROM tasks ORDER BY "
                        + "CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 END ASC";
                break;

            case 7:
                sql = "SELECT * FROM tasks ORDER BY created_date DESC";
                break;
        }

        List<Task> tasks = getAllTasks(sql);
        if (tasks.isEmpty()) {
            printInfo("No tasks match the selected filter.");
        } else {
            displayTaskTable(tasks);
        }
    }

    // ========================================================================
    //  FEATURE 8: STATISTICS DASHBOARD
    // ========================================================================
    private static void showStatistics() {
        printSectionHeader("📊 TASK STATISTICS");

        try {
            int total = getCount("SELECT COUNT(*) FROM tasks");
            int pending = getCount("SELECT COUNT(*) FROM tasks WHERE status='PENDING'");
            int inProgress = getCount("SELECT COUNT(*) FROM tasks WHERE status='IN_PROGRESS'");
            int completed = getCount("SELECT COUNT(*) FROM tasks WHERE status='COMPLETED'");
            int highPrio = getCount("SELECT COUNT(*) FROM tasks WHERE priority='HIGH'");
            int medPrio = getCount("SELECT COUNT(*) FROM tasks WHERE priority='MEDIUM'");
            int lowPrio = getCount("SELECT COUNT(*) FROM tasks WHERE priority='LOW'");
            String today = DATE_FORMAT.format(new Date());
            int overdue = getCount("SELECT COUNT(*) FROM tasks WHERE due_date < '" + today + "' AND status != 'COMPLETED'");

            System.out.println();
            System.out.println(CYAN + "  ╔════════════════════════════════════════════════╗" + RESET);
            System.out.println(CYAN + "  ║" + BOLD + WHITE + "          TASK STATISTICS DASHBOARD              " + RESET + CYAN + "║" + RESET);
            System.out.println(CYAN + "  ╠════════════════════════════════════════════════╣" + RESET);

            // Total
            System.out.println(CYAN + "  ║" + WHITE + "  Total Tasks:          " + BOLD + padRight(String.valueOf(total), 24) + RESET + CYAN + "║" + RESET);
            System.out.println(CYAN + "  ╠════════════════════════════════════════════════╣" + RESET);

            // Status Breakdown
            System.out.println(CYAN + "  ║" + BOLD + YELLOW + "  STATUS BREAKDOWN                              " + RESET + CYAN + "║" + RESET);
            System.out.println(CYAN + "  ║" + YELLOW + "  ● Pending:            " + padRight(String.valueOf(pending), 24) + RESET + CYAN + "║" + RESET);
            System.out.println(CYAN + "  ║" + BLUE + "  ● In Progress:        " + padRight(String.valueOf(inProgress), 24) + RESET + CYAN + "║" + RESET);
            System.out.println(CYAN + "  ║" + GREEN + "  ● Completed:          " + padRight(String.valueOf(completed), 24) + RESET + CYAN + "║" + RESET);
            System.out.println(CYAN + "  ╠════════════════════════════════════════════════╣" + RESET);

            // Priority Breakdown
            System.out.println(CYAN + "  ║" + BOLD + MAGENTA + "  PRIORITY BREAKDOWN                            " + RESET + CYAN + "║" + RESET);
            System.out.println(CYAN + "  ║" + RED + "  ▲ High Priority:      " + padRight(String.valueOf(highPrio), 24) + RESET + CYAN + "║" + RESET);
            System.out.println(CYAN + "  ║" + YELLOW + "  ■ Medium Priority:    " + padRight(String.valueOf(medPrio), 24) + RESET + CYAN + "║" + RESET);
            System.out.println(CYAN + "  ║" + GREEN + "  ▼ Low Priority:       " + padRight(String.valueOf(lowPrio), 24) + RESET + CYAN + "║" + RESET);
            System.out.println(CYAN + "  ╠════════════════════════════════════════════════╣" + RESET);

            // Overdue
            if (overdue > 0) {
                System.out.println(CYAN + "  ║" + RED + BOLD + "  ⚠ OVERDUE TASKS:      " + padRight(String.valueOf(overdue), 24) + RESET + CYAN + "║" + RESET);
            } else {
                System.out.println(CYAN + "  ║" + GREEN + "  ✓ No Overdue Tasks!   " + padRight("", 24) + RESET + CYAN + "║" + RESET);
            }

            // Completion Rate
            if (total > 0) {
                double rate = (completed * 100.0) / total;
                String rateStr = String.format("%.1f%%", rate);
                System.out.println(CYAN + "  ╠════════════════════════════════════════════════╣" + RESET);
                System.out.println(CYAN + "  ║" + WHITE + "  Completion Rate:      " + BOLD + padRight(rateStr, 24) + RESET + CYAN + "║" + RESET);

                // Progress bar
                int barLength = 30;
                int filled = (int) (rate * barLength / 100);
                StringBuilder bar = new StringBuilder("  [");
                for (int i = 0; i < barLength; i++) {
                    if (i < filled) bar.append("█");
                    else bar.append("░");
                }
                bar.append("]");
                String barStr = padRight(bar.toString(), 46);
                System.out.println(CYAN + "  ║" + GREEN + barStr + RESET + CYAN + "║" + RESET);
            }

            System.out.println(CYAN + "  ╚════════════════════════════════════════════════╝" + RESET);

            // Show categories
            System.out.println();
            System.out.println(CYAN + "  ── Categories ──" + RESET);
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT category, COUNT(*) as cnt FROM tasks GROUP BY category ORDER BY cnt DESC");
            while (rs.next()) {
                System.out.println(WHITE + "    • " + rs.getString("category") + ": " + rs.getInt("cnt") + " task(s)" + RESET);
            }
            rs.close();
            stmt.close();

        } catch (SQLException e) {
            printError("Failed to load statistics: " + e.getMessage());
        }
    }

    // ========================================================================
    //  DATABASE HELPER METHODS
    // ========================================================================
    private static List<Task> getAllTasks(String sql) {
        List<Task> tasks = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                tasks.add(extractTask(rs));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            printError("Failed to retrieve tasks: " + e.getMessage());
        }
        return tasks;
    }

    private static Task getTaskById(int id) {
        try {
            String sql = "SELECT * FROM tasks WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Task task = extractTask(rs);
                rs.close();
                pstmt.close();
                return task;
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            printError("Failed to find task: " + e.getMessage());
        }
        return null;
    }

    private static Task extractTask(ResultSet rs) throws SQLException {
        return new Task(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getString("priority"),
                rs.getString("status"),
                rs.getString("due_date"),
                rs.getString("created_date"),
                rs.getString("completed_date")
        );
    }

    private static int getCount(String sql) {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                int count = rs.getInt(1);
                rs.close();
                stmt.close();
                return count;
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            printError("Count query failed: " + e.getMessage());
        }
        return 0;
    }

    private static int[] getTaskCounts() {
        int total = getCount("SELECT COUNT(*) FROM tasks");
        int pending = getCount("SELECT COUNT(*) FROM tasks WHERE status='PENDING'");
        int completed = getCount("SELECT COUNT(*) FROM tasks WHERE status='COMPLETED'");
        return new int[]{total, pending, completed};
    }

    // ========================================================================
    //  DISPLAY HELPER: TASK TABLE
    // ========================================================================
    private static void displayTaskTable(List<Task> tasks) {
        System.out.println();
        // Header
        System.out.println(WHITE + "  " + padRight("ID", 5)
                + padRight("Title", 22)
                + padRight("Category", 14)
                + padRight("Priority", 10)
                + padRight("Status", 14)
                + padRight("Due Date", 12) + RESET);
        System.out.println(WHITE + "  " + repeat("─", 5)
                + repeat("─", 22)
                + repeat("─", 14)
                + repeat("─", 10)
                + repeat("─", 14)
                + repeat("─", 12) + RESET);

        for (Task t : tasks) {
            String title = t.title.length() > 20 ? t.title.substring(0, 17) + "..." : t.title;
            String category = t.category.length() > 12 ? t.category.substring(0, 9) + "..." : t.category;
            String dueDate = t.dueDate != null ? formatDisplayDate(t.dueDate) : "   ---";
            String overdue = "";
            if (t.dueDate != null && !t.status.equals("COMPLETED")) {
                try {
                    Date due = DATE_FORMAT.parse(t.dueDate);
                    if (due.before(new Date())) {
                        overdue = RED + " !" + RESET;
                    }
                } catch (ParseException ignored) {}
            }

            System.out.println("  " + CYAN + padRight(String.valueOf(t.id), 5) + RESET
                    + WHITE + padRight(title, 22) + RESET
                    + MAGENTA + padRight(category, 14) + RESET
                    + colorPriority(t.priority) + padRight(t.priority, 10) + RESET
                    + colorStatus(t.status) + padRight(formatStatus(t.status), 14) + RESET
                    + WHITE + padRight(dueDate, 12) + RESET
                    + overdue);
        }

        System.out.println(WHITE + "\n  Total: " + tasks.size() + " task(s)" + RESET);
    }

    // ========================================================================
    //  INPUT HELPER METHODS
    // ========================================================================
    private static int getIntInput(String prompt, int min, int max) {
        while (true) {
            System.out.print(YELLOW + prompt + RESET);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                printError("Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                printError("Invalid input. Please enter a valid number.");
            }
        }
    }

    // ========================================================================
    //  FORMATTING & UTILITY METHODS
    // ========================================================================
    private static String colorPriority(String priority) {
        switch (priority) {
            case "HIGH": return RED + BOLD;
            case "MEDIUM": return YELLOW;
            case "LOW": return GREEN;
            default: return WHITE;
        }
    }

    private static String colorStatus(String status) {
        switch (status) {
            case "PENDING": return YELLOW;
            case "IN_PROGRESS": return BLUE;
            case "COMPLETED": return GREEN;
            default: return WHITE;
        }
    }

    private static String formatStatus(String status) {
        switch (status) {
            case "PENDING": return "Pending";
            case "IN_PROGRESS": return "In Progress";
            case "COMPLETED": return "Completed";
            default: return status;
        }
    }

    private static String formatDisplayDate(String dateStr) {
        try {
            Date date = DATE_FORMAT.parse(dateStr);
            return DISPLAY_DATE_FORMAT.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    private static boolean isValidDate(String dateStr) {
        try {
            DATE_FORMAT.setLenient(false);
            DATE_FORMAT.parse(dateStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private static String padRight(String text, int width) {
        if (text == null) text = "";
        if (text.length() >= width) return text.substring(0, width);
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    private static String centerText(String text, int width) {
        if (text.length() >= width) return text.substring(0, width);
        int padding = (width - text.length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < padding; i++) sb.append(' ');
        sb.append(text);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(str);
        return sb.toString();
    }

    // ========================================================================
    //  MESSAGE DISPLAY METHODS
    // ========================================================================
    private static void printSuccess(String message) {
        System.out.println(GREEN + "\n  ✓ " + message + RESET);
    }

    private static void printError(String message) {
        System.out.println(RED + "\n  ✗ " + message + RESET);
    }

    private static void printWarning(String message) {
        System.out.println(YELLOW + "\n  ⚠ " + message + RESET);
    }

    private static void printInfo(String message) {
        System.out.println(CYAN + "\n  ℹ " + message + RESET);
    }
}
