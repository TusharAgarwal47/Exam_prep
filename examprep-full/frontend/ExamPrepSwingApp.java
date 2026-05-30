import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.PrintWriter;
import java.net.URLEncoder;
import org.json.*;

public class ExamPrepSwingApp {

    private static final String BASE_URL = "http://localhost:8080";

    private static Long   currentUserId;
    private static String currentUsername;
    private static String currentDisplayName;
    private static String currentRole;

    private static final Color PRIMARY   = new Color(37, 99, 235);
    private static final Color LIGHT_BG  = new Color(241, 245, 249);
    private static final Color WHITE     = Color.WHITE;
    private static final Color DANGER    = new Color(220, 38, 38);
    private static final Color SUCCESS   = new Color(22, 163, 74);
    private static final Color TEXT_DARK = new Color(30, 41, 59);

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> showLoginFrame());
    }

    private static void showLoginFrame() {
        JFrame frame = new JFrame("ExamPrep — Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(420, 480);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(LIGHT_BG);

        JPanel header = new JPanel();
        header.setBackground(PRIMARY);
        header.setPreferredSize(new Dimension(420, 90));
        JLabel title = new JLabel("📚 ExamPrep");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(WHITE);
        header.add(title);
        root.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setBackground(WHITE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel subtitle = new JLabel("Sign in to your account");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(Color.GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField usernameField = makeTextField();
        JPasswordField passwordField = new JPasswordField();
        styleTextField(passwordField);

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(DANGER);
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton loginBtn = makePrimaryButton("Login");
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JLabel hint = new JLabel("Admin: admin / admin123  |  Student: student / student123");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        hint.setForeground(Color.GRAY);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        form.add(subtitle);
        form.add(Box.createVerticalStrut(24));
        form.add(makeLabel("Username"));
        form.add(Box.createVerticalStrut(4));
        form.add(usernameField);
        form.add(Box.createVerticalStrut(14));
        form.add(makeLabel("Password"));
        form.add(Box.createVerticalStrut(4));
        form.add(passwordField);
        form.add(Box.createVerticalStrut(8));
        form.add(errorLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(loginBtn);
        form.add(Box.createVerticalStrut(20));
        form.add(hint);

        root.add(form, BorderLayout.CENTER);
        frame.setContentPane(root);
        frame.setVisible(true);

        ActionListener doLogin = e -> {
            String uname = usernameField.getText().trim();
            String pwd   = new String(passwordField.getPassword());
            if (uname.isEmpty() || pwd.isEmpty()) { errorLabel.setText("Please enter username and password."); return; }
            loginBtn.setEnabled(false); loginBtn.setText("Logging in…");
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() {
                    return postJson(BASE_URL + "/api/auth/login",
                            "{\"username\":\"" + escape(uname) + "\",\"password\":\"" + escape(pwd) + "\"}");
                }
                @Override protected void done() {
                    loginBtn.setEnabled(true); loginBtn.setText("Login");
                    try {
                        String resp = get();
                        if (resp == null) { errorLabel.setText("Cannot reach server. Is backend running?"); return; }
                        JSONObject json = new JSONObject(resp);
                        if (json.optBoolean("success", false)) {
                            JSONObject data = json.getJSONObject("data");
                            currentUserId      = data.getLong("id");
                            currentUsername    = data.getString("username");
                            currentDisplayName = data.optString("displayName", currentUsername);
                            currentRole        = data.getString("role");
                            frame.dispose();
                            if ("ADMIN".equals(currentRole)) showAdminDashboard();
                            else showStudentDashboard();
                        } else { errorLabel.setText(json.optString("message", "Login failed.")); }
                    } catch (Exception ex) { errorLabel.setText("Error: " + ex.getMessage()); }
                }
            }.execute();
        };
        loginBtn.addActionListener(doLogin);
        passwordField.addActionListener(doLogin);
        usernameField.addActionListener(e -> passwordField.requestFocus());
    }

    // ════════════════════════════════════════════════════════════
    //  STUDENT DASHBOARD
    // ════════════════════════════════════════════════════════════
    private static void showStudentDashboard() {
        JFrame frame = new JFrame("ExamPrep — Student Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 680);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(LIGHT_BG);
        root.add(makeTopBar("Student Dashboard", frame), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // ── Tab 1: Exam Papers ──
        JPanel papersPanel = new JPanel(new BorderLayout(0, 10));
        papersPanel.setBackground(LIGHT_BG);
        papersPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] paperCols = {"ID", "Subject", "Paper Code", "Year", "Semester", "Exam Type", "Total Marks"};
        DefaultTableModel papersModel = new DefaultTableModel(paperCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable papersTable = new JTable(papersModel);
        papersTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        papersTable.setRowHeight(28);
        papersTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        papersTable.setSelectionBackground(new Color(219, 234, 254));
        papersTable.setGridColor(new Color(226, 232, 240));
        papersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        papersTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        papersTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        papersTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        papersTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        papersTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        papersTable.getColumnModel().getColumn(5).setPreferredWidth(200);
        papersTable.getColumnModel().getColumn(6).setPreferredWidth(80);

        JLabel papersStatus = new JLabel("Loading papers...");
        papersStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        papersStatus.setForeground(Color.GRAY);

        JButton refreshPapersBtn = makePrimaryButton("🔄 Refresh");
        refreshPapersBtn.setBackground(new Color(37, 99, 235));
        refreshPapersBtn.setForeground(Color.WHITE);
        refreshPapersBtn.setOpaque(true);
        refreshPapersBtn.setBorderPainted(false);

        JButton viewQuestionsBtn = makePrimaryButton("📋 View Questions");
        viewQuestionsBtn.setBackground(new Color(22, 163, 74));
        viewQuestionsBtn.setForeground(Color.WHITE);
        viewQuestionsBtn.setOpaque(true);
        viewQuestionsBtn.setBorderPainted(false);

        JPanel papersBtnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        papersBtnBar.setOpaque(false);
        papersBtnBar.add(refreshPapersBtn);
        papersBtnBar.add(viewQuestionsBtn);

        papersPanel.add(papersBtnBar, BorderLayout.NORTH);
        papersPanel.add(new JScrollPane(papersTable), BorderLayout.CENTER);
        papersPanel.add(papersStatus, BorderLayout.SOUTH);

        // ── Tab 2: Questions ──
        JPanel questionsPanel = new JPanel(new BorderLayout(0, 10));
        questionsPanel.setBackground(LIGHT_BG);
        questionsPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        String[] qCols = {"#", "Question", "Marks"};
        DefaultTableModel questionsModel = new DefaultTableModel(qCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable questionsTable = new JTable(questionsModel);
        questionsTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        questionsTable.setRowHeight(36);
        questionsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        questionsTable.setSelectionBackground(new Color(219, 234, 254));
        questionsTable.setGridColor(new Color(226, 232, 240));
        questionsTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        questionsTable.getColumnModel().getColumn(1).setPreferredWidth(750);
        questionsTable.getColumnModel().getColumn(2).setPreferredWidth(60);

        // Wrap long question text
        questionsTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JTextArea area = new JTextArea(v == null ? "" : v.toString());
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                area.setOpaque(true);
                area.setBorder(new EmptyBorder(6, 6, 6, 6));
                area.setBackground(sel ? new Color(219, 234, 254) : Color.WHITE);
                int height = area.getPreferredSize().height + 10;
                if (t.getRowHeight(r) != Math.max(36, height)) t.setRowHeight(r, Math.max(36, height));
                return area;
            }
        });

        JLabel questionsStatus = new JLabel("Select a paper from 'Exam Papers' tab and click 'View Questions'");
        questionsStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        questionsStatus.setForeground(Color.GRAY);

        questionsPanel.add(new JScrollPane(questionsTable), BorderLayout.CENTER);
        questionsPanel.add(questionsStatus, BorderLayout.SOUTH);

        // ── Tab 3: Feedback ──
        JPanel feedbackPanel = new JPanel(new BorderLayout());
        feedbackPanel.setBackground(LIGHT_BG);
        feedbackPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel card = new JPanel();
        card.setBackground(WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(24, 24, 24, 24)));

        JLabel heading = new JLabel("Submit Feedback");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 18));
        heading.setForeground(TEXT_DARK);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField subCodeField = makeTextField();
        subCodeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JTextArea msgArea = new JTextArea(6, 20);
        msgArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msgArea.setLineWrap(true);
        msgArea.setWrapStyleWord(true);
        msgArea.setBorder(new CompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        JScrollPane msgScroll = new JScrollPane(msgArea);
        msgScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        msgScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        JLabel fbStatus = new JLabel(" ");
        fbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fbStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton submitBtn = makePrimaryButton("Submit Feedback");
        submitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        card.add(heading);
        card.add(Box.createVerticalStrut(20));
        card.add(makeLabel("Subject Code (optional)"));
        card.add(Box.createVerticalStrut(4));
        card.add(subCodeField);
        card.add(Box.createVerticalStrut(14));
        card.add(makeLabel("Your Feedback *"));
        card.add(Box.createVerticalStrut(4));
        card.add(msgScroll);
        card.add(Box.createVerticalStrut(10));
        card.add(fbStatus);
        card.add(Box.createVerticalStrut(10));
        card.add(submitBtn);
        feedbackPanel.add(card, BorderLayout.CENTER);

        tabs.addTab("📄 Exam Papers", papersPanel);
        tabs.addTab("❓ Questions", questionsPanel);
        tabs.addTab("💬 Feedback", feedbackPanel);

        // ── Tab 4: Top / Frequent Questions ──
        JPanel freqPanel = new JPanel(new BorderLayout(0, 10));
        freqPanel.setBackground(LIGHT_BG);
        freqPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Top control bar
        JPanel freqTopBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        freqTopBar.setOpaque(false);

        JComboBox<String> subjectCombo = new JComboBox<>();
        subjectCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subjectCombo.setPreferredSize(new Dimension(280, 36));
        subjectCombo.addItem("— Select Subject —");

        JButton loadFreqBtn = makePrimaryButton("🔥 Show Top Questions");
        loadFreqBtn.setBackground(new Color(234, 88, 12));  // orange

        JLabel freqStatus = new JLabel("Select a subject to view most repeated questions.");
        freqStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        freqStatus.setForeground(Color.GRAY);

        freqTopBar.add(new JLabel("Subject:"));
        freqTopBar.add(subjectCombo);
        freqTopBar.add(loadFreqBtn);

        // Frequency table
        String[] freqCols = {"Rank", "Question", "Times Asked"};
        DefaultTableModel freqModel = new DefaultTableModel(freqCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable freqTable = new JTable(freqModel);
        freqTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        freqTable.setRowHeight(36);
        freqTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        freqTable.setSelectionBackground(new Color(254, 215, 170));
        freqTable.setGridColor(new Color(226, 232, 240));
        freqTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        freqTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        freqTable.getColumnModel().getColumn(1).setPreferredWidth(760);
        freqTable.getColumnModel().getColumn(2).setPreferredWidth(90);

        // Rank column: medal emoji for top 3
        freqTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel lbl = new JLabel();
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
                lbl.setOpaque(true);
                lbl.setBackground(sel ? new Color(254, 215, 170) : Color.WHITE);
                String rank = v == null ? "" : v.toString();
                if ("1".equals(rank))      lbl.setText("🥇");
                else if ("2".equals(rank)) lbl.setText("🥈");
                else if ("3".equals(rank)) lbl.setText("🥉");
                else                        lbl.setText(rank);
                return lbl;
            }
        });

        // Question text column: wrap long text
        freqTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JTextArea area = new JTextArea(v == null ? "" : v.toString());
                area.setLineWrap(true); area.setWrapStyleWord(true);
                area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                area.setOpaque(true);
                area.setBorder(new EmptyBorder(6, 8, 6, 8));
                area.setBackground(sel ? new Color(254, 215, 170) : Color.WHITE);
                int h = area.getPreferredSize().height + 10;
                if (t.getRowHeight(r) != Math.max(36, h)) t.setRowHeight(r, Math.max(36, h));
                return area;
            }
        });

        // Times asked column: badge style
        freqTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel lbl = new JLabel(v == null ? "" : v + "×");
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                lbl.setOpaque(true);
                int freq = 0;
                try { freq = Integer.parseInt(v.toString()); } catch (Exception ignored) {}
                lbl.setForeground(freq >= 3 ? new Color(185, 28, 28) : freq == 2 ? new Color(180, 83, 9) : TEXT_DARK);
                lbl.setBackground(sel ? new Color(254, 215, 170) : Color.WHITE);
                return lbl;
            }
        });

        freqPanel.add(freqTopBar, BorderLayout.NORTH);
        freqPanel.add(new JScrollPane(freqTable), BorderLayout.CENTER);
        freqPanel.add(freqStatus, BorderLayout.SOUTH);

        tabs.addTab("🔥 Top Questions", freqPanel);

        root.add(tabs, BorderLayout.CENTER);
        frame.setContentPane(root);
        frame.setVisible(true);

        // ── Load papers ──
        Runnable loadPapers = () -> new SwingWorker<String, Void>() {
            @Override protected String doInBackground() { return httpGet(BASE_URL + "/api/questions/papers"); }
            @Override protected void done() {
                try {
                    String resp = get();
                    papersModel.setRowCount(0);
                    if (resp == null) { papersStatus.setForeground(DANGER); papersStatus.setText("Could not load papers."); return; }
                    JSONArray arr = new JSONObject(resp).optJSONArray("data");
                    if (arr == null) { papersStatus.setForeground(DANGER); papersStatus.setText("No data found."); return; }
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject p = arr.getJSONObject(i);
                        papersModel.addRow(new Object[]{
                                p.getLong("id"),
                                p.optString("subject", "—"),
                                p.optString("paperCode", "—"),
                                p.optInt("year", 0),
                                p.optString("semester", "—"),
                                p.optString("examType", "—"),
                                p.optInt("totalMarks", 0)
                        });
                    }
                    papersStatus.setForeground(Color.GRAY);
                    papersStatus.setText("Loaded " + arr.length() + " paper(s). Select one and click 'View Questions'.");
                } catch (Exception ex) {
                    papersStatus.setForeground(DANGER);
                    papersStatus.setText("Error: " + ex.getMessage());
                }
            }
        }.execute();

        loadPapers.run();
        refreshPapersBtn.addActionListener(e -> loadPapers.run());

        // ── Load subjects into combo ──
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() { return httpGet(BASE_URL + "/api/questions/subjects"); }
            @Override protected void done() {
                try {
                    String resp = get();
                    if (resp == null) return;
                    JSONArray arr = new JSONObject(resp).optJSONArray("data");
                    if (arr == null) return;
                    SwingUtilities.invokeLater(() -> {
                        subjectCombo.removeAllItems();
                        subjectCombo.addItem("— Select Subject —");
                        for (int i = 0; i < arr.length(); i++) subjectCombo.addItem(arr.getString(i));
                    });
                } catch (Exception ignored) {}
            }
        }.execute();

        // ── Load top questions for selected subject ──
        loadFreqBtn.addActionListener(e -> {
            String selected = (String) subjectCombo.getSelectedItem();
            if (selected == null || selected.startsWith("—")) {
                freqStatus.setForeground(DANGER); freqStatus.setText("Please select a subject first."); return;
            }
            freqStatus.setForeground(Color.GRAY); freqStatus.setText("Loading top questions for: " + selected + "…");
            loadFreqBtn.setEnabled(false);
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() {
                    try { return httpGet(BASE_URL + "/api/questions/frequent/by-subject/" + URLEncoder.encode(selected, "UTF-8")); }
                    catch (Exception ex) { return null; }
                }
                @Override protected void done() {
                    loadFreqBtn.setEnabled(true);
                    try {
                        String resp = get();
                        freqModel.setRowCount(0);
                        if (resp == null) { freqStatus.setForeground(DANGER); freqStatus.setText("Server unreachable."); return; }
                        JSONObject json = new JSONObject(resp);
                        if (!json.optBoolean("success", false)) {
                            freqStatus.setForeground(DANGER); freqStatus.setText(json.optString("message", "No data.")); return;
                        }
                        JSONArray arr = json.optJSONArray("data");
                        if (arr == null || arr.length() == 0) {
                            freqStatus.setForeground(Color.GRAY); freqStatus.setText("No frequency data yet for: " + selected); return;
                        }
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject row = arr.getJSONObject(i);
                            freqModel.addRow(new Object[]{
                                    String.valueOf(i + 1),
                                    row.optString("questionText", "—"),
                                    row.optInt("frequency", 0)
                            });
                        }
                        freqStatus.setForeground(SUCCESS);
                        freqStatus.setText("✔ Showing top " + arr.length() + " questions for: " + selected);
                    } catch (Exception ex) { freqStatus.setForeground(DANGER); freqStatus.setText("Error: " + ex.getMessage()); }
                }
            }.execute();
        });

        // ── View Questions ──
        viewQuestionsBtn.addActionListener(e -> {
            int row = papersTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(frame, "Please select a paper first.", "No Selection", JOptionPane.WARNING_MESSAGE); return; }
            String paperCode = (String) papersModel.getValueAt(row, 2);
            String subject   = (String) papersModel.getValueAt(row, 1);
            questionsStatus.setForeground(Color.GRAY);
            questionsStatus.setText("Loading questions for: " + subject + "...");
            tabs.setSelectedIndex(1);

            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() { return httpGet(BASE_URL + "/api/questions/by-paper/" + paperCode); }
                @Override protected void done() {
                    try {
                        String resp = get();
                        questionsModel.setRowCount(0);
                        if (resp == null) { questionsStatus.setForeground(DANGER); questionsStatus.setText("Could not load questions."); return; }
                        JSONArray arr = new JSONObject(resp).optJSONArray("data");
                        if (arr == null) { questionsStatus.setForeground(DANGER); questionsStatus.setText("No questions found."); return; }
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject q = arr.getJSONObject(i);
                            questionsModel.addRow(new Object[]{
                                    i + 1,
                                    q.optString("questionText", "—"),
                                    q.optInt("marks", 0)
                            });
                        }
                        questionsStatus.setForeground(SUCCESS);
                        questionsStatus.setText("✔ Loaded " + arr.length() + " question(s) for: " + subject);
                    } catch (Exception ex) {
                        questionsStatus.setForeground(DANGER);
                        questionsStatus.setText("Error: " + ex.getMessage());
                    }
                }
            }.execute();
        });

        // ── Submit Feedback ──
        submitBtn.addActionListener(e -> {
            String msg     = msgArea.getText().trim();
            String subCode = subCodeField.getText().trim();
            if (msg.isEmpty()) { fbStatus.setForeground(DANGER); fbStatus.setText("Feedback message cannot be empty."); return; }
            submitBtn.setEnabled(false); submitBtn.setText("Submitting…");
            String body = "{\"message\":\"" + escape(msg) + "\","
                    + "\"submittedBy\":\"" + escape(currentDisplayName) + "\","
                    + "\"subjectCode\":\"" + escape(subCode) + "\"}";
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() { return postJson(BASE_URL + "/api/feedback/submit", body); }
                @Override protected void done() {
                    submitBtn.setEnabled(true); submitBtn.setText("Submit Feedback");
                    try {
                        String resp = get();
                        if (resp == null) { fbStatus.setForeground(DANGER); fbStatus.setText("Server unreachable."); return; }
                        JSONObject json = new JSONObject(resp);
                        if (json.optBoolean("success", false)) {
                            fbStatus.setForeground(SUCCESS); fbStatus.setText("✔ Feedback submitted successfully!");
                            msgArea.setText(""); subCodeField.setText("");
                        } else { fbStatus.setForeground(DANGER); fbStatus.setText(json.optString("message", "Submission failed.")); }
                    } catch (Exception ex) { fbStatus.setForeground(DANGER); fbStatus.setText("Error: " + ex.getMessage()); }
                }
            }.execute();
        });
    }

    // ════════════════════════════════════════════════════════════
    //  ADMIN DASHBOARD
    // ════════════════════════════════════════════════════════════
    private static void showAdminDashboard() {
        JFrame frame = new JFrame("ExamPrep — Admin Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(960, 680);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(LIGHT_BG);
        root.add(makeTopBar("Admin Dashboard", frame), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // ── Tab 1: Feedback ──
        JPanel feedbackPanel = new JPanel(new BorderLayout());
        feedbackPanel.setBackground(LIGHT_BG);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        toolbar.setBackground(LIGHT_BG);
        JButton refreshBtn = makePrimaryButton("🔄 Refresh");
        JButton unreadBtn  = makeSecondaryButton("📬 Show Unread");
        JButton allBtn     = makeSecondaryButton("📋 Show All");
        JLabel countBadge  = new JLabel("Loading…");
        countBadge.setFont(new Font("Segoe UI", Font.BOLD, 13));
        countBadge.setForeground(DANGER);
        toolbar.add(refreshBtn); toolbar.add(unreadBtn); toolbar.add(allBtn);
        toolbar.add(Box.createHorizontalStrut(20));
        toolbar.add(new JLabel("Unread: ")); toolbar.add(countBadge);
        feedbackPanel.add(toolbar, BorderLayout.NORTH);

        String[] cols = {"ID", "Submitted By", "Subject Code", "Message", "Read?", "Submitted At"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setGridColor(new Color(226, 232, 240));
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(320);
        table.getColumnModel().getColumn(4).setPreferredWidth(55);
        table.getColumnModel().getColumn(5).setPreferredWidth(150);
        feedbackPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setBackground(LIGHT_BG);
        JButton markReadBtn = makePrimaryButton("✔ Mark Selected as Read");
        JLabel actionStatus = new JLabel(" ");
        actionStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bottomBar.add(actionStatus); bottomBar.add(markReadBtn);
        feedbackPanel.add(bottomBar, BorderLayout.SOUTH);

        // ── Tab 2: Upload Paper ──
        JPanel uploadPanel = new JPanel(new BorderLayout(0, 10));
        uploadPanel.setBackground(LIGHT_BG);
        uploadPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel formCard = new JPanel();
        formCard.setBackground(WHITE);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(24, 28, 24, 28)));

        JLabel uploadHeading = new JLabel("Upload Exam Paper");
        uploadHeading.setFont(new Font("Segoe UI", Font.BOLD, 18));
        uploadHeading.setForeground(TEXT_DARK);
        uploadHeading.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Paper detail fields
        JTextField subjectField   = makeTextField(); subjectField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JTextField paperCodeField = makeTextField(); paperCodeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JTextField yearField      = makeTextField(); yearField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JTextField semesterField  = makeTextField(); semesterField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JTextField examTypeField  = makeTextField(); examTypeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JTextField totalMarksField= makeTextField(); totalMarksField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel uploadStatus = new JLabel(" ");
        uploadStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        uploadStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton uploadPaperBtn = makePrimaryButton("📤 Upload Paper");
        uploadPaperBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        formCard.add(uploadHeading);
        formCard.add(Box.createVerticalStrut(18));
        formCard.add(makeLabel("Subject Name *")); formCard.add(Box.createVerticalStrut(4)); formCard.add(subjectField);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(makeLabel("Paper Code *"));   formCard.add(Box.createVerticalStrut(4)); formCard.add(paperCodeField);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(makeLabel("Year *"));         formCard.add(Box.createVerticalStrut(4)); formCard.add(yearField);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(makeLabel("Semester"));       formCard.add(Box.createVerticalStrut(4)); formCard.add(semesterField);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(makeLabel("Exam Type"));      formCard.add(Box.createVerticalStrut(4)); formCard.add(examTypeField);
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(makeLabel("Total Marks"));    formCard.add(Box.createVerticalStrut(4)); formCard.add(totalMarksField);
        formCard.add(Box.createVerticalStrut(16));
        formCard.add(uploadStatus);
        formCard.add(Box.createVerticalStrut(8));
        formCard.add(uploadPaperBtn);

        // Questions section below the form
        JPanel questionsCard = new JPanel(new BorderLayout(0, 8));
        questionsCard.setBackground(WHITE);
        questionsCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(16, 20, 16, 20)));

        JLabel qHeading = new JLabel("Add Questions to Uploaded Paper");
        qHeading.setFont(new Font("Segoe UI", Font.BOLD, 15));
        qHeading.setForeground(TEXT_DARK);

        JPanel qForm = new JPanel();
        qForm.setOpaque(false);
        qForm.setLayout(new BoxLayout(qForm, BoxLayout.Y_AXIS));

        JTextField qPaperCodeField = makeTextField(); qPaperCodeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JTextArea  qTextArea       = new JTextArea(4, 20);
        qTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        qTextArea.setLineWrap(true); qTextArea.setWrapStyleWord(true);
        qTextArea.setBorder(new CompoundBorder(new LineBorder(new Color(203,213,225),1,true), new EmptyBorder(8,10,8,10)));
        JScrollPane qTextScroll = new JScrollPane(qTextArea);
        qTextScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        qTextScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JTextField qMarksField = makeTextField(); qMarksField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel qStatus = new JLabel(" ");
        qStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        qStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addQuestionBtn = makePrimaryButton("➕ Add Question");
        addQuestionBtn.setBackground(SUCCESS);
        addQuestionBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // PDF upload button
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel orLabel = new JLabel("── OR upload a PDF to auto-extract questions ──");
        orLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        orLabel.setForeground(Color.GRAY);
        orLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton uploadPdfBtn = makePrimaryButton("📎 Choose PDF & Upload");
        uploadPdfBtn.setBackground(new Color(99, 102, 241));  // indigo
        uploadPdfBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel pdfFileLabel = new JLabel("No file chosen");
        pdfFileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pdfFileLabel.setForeground(Color.GRAY);
        pdfFileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        qForm.add(makeLabel("Paper Code (of uploaded paper) *")); qForm.add(Box.createVerticalStrut(4)); qForm.add(qPaperCodeField);
        qForm.add(Box.createVerticalStrut(10));
        qForm.add(makeLabel("Question Text *")); qForm.add(Box.createVerticalStrut(4)); qForm.add(qTextScroll);
        qForm.add(Box.createVerticalStrut(10));
        qForm.add(makeLabel("Marks *")); qForm.add(Box.createVerticalStrut(4)); qForm.add(qMarksField);
        qForm.add(Box.createVerticalStrut(10));
        qForm.add(qStatus);
        qForm.add(Box.createVerticalStrut(6));
        qForm.add(addQuestionBtn);
        qForm.add(Box.createVerticalStrut(16));
        qForm.add(orLabel);
        qForm.add(Box.createVerticalStrut(8));
        qForm.add(pdfFileLabel);
        qForm.add(Box.createVerticalStrut(6));
        qForm.add(uploadPdfBtn);

        questionsCard.add(qHeading, BorderLayout.NORTH);
        questionsCard.add(Box.createVerticalStrut(10));
        questionsCard.add(qForm, BorderLayout.CENTER);

        // ── PDF upload logic ──
        final java.io.File[] chosenPdf = {null};
        uploadPdfBtn.addActionListener(e -> {
            String pdfPaperCode = qPaperCodeField.getText().trim();
            if (pdfPaperCode.isEmpty()) {
                qStatus.setForeground(DANGER);
                qStatus.setText("Enter the Paper Code first, then choose a PDF.");
                return;
            }
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Select Exam Paper PDF");
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));
            int result = fc.showOpenDialog(null);
            if (result != JFileChooser.APPROVE_OPTION) return;
            chosenPdf[0] = fc.getSelectedFile();
            pdfFileLabel.setText("Selected: " + chosenPdf[0].getName());
            qStatus.setForeground(Color.GRAY);
            qStatus.setText("Uploading PDF…");
            uploadPdfBtn.setEnabled(false);
            uploadPdfBtn.setText("Uploading…");

            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() {
                    return uploadMultipart(BASE_URL + "/api/questions/upload-pdf",
                            chosenPdf[0], pdfPaperCode);
                }
                @Override protected void done() {
                    uploadPdfBtn.setEnabled(true);
                    uploadPdfBtn.setText("📎 Choose PDF & Upload");
                    try {
                        String resp = get();
                        if (resp == null) { qStatus.setForeground(DANGER); qStatus.setText("Server unreachable."); return; }
                        JSONObject json = new JSONObject(resp);
                        if (json.optBoolean("success", false)) {
                            int count = json.optJSONArray("data") != null ? json.getJSONArray("data").length() : 0;
                            qStatus.setForeground(SUCCESS);
                            qStatus.setText("✔ " + json.optString("message", count + " question(s) extracted!"));
                        } else {
                            qStatus.setForeground(DANGER);
                            qStatus.setText(json.optString("message", "PDF upload failed."));
                        }
                    } catch (Exception ex) { qStatus.setForeground(DANGER); qStatus.setText("Error: " + ex.getMessage()); }
                }
            }.execute();
        });

        JPanel twoCol = new JPanel(new GridLayout(1, 2, 12, 0));
        twoCol.setOpaque(false);
        twoCol.add(formCard);
        twoCol.add(questionsCard);

        JScrollPane uploadScroll = new JScrollPane(twoCol);
        uploadScroll.setBorder(null);
        uploadScroll.getVerticalScrollBar().setUnitIncrement(16);
        uploadPanel.add(uploadScroll, BorderLayout.CENTER);

        // ── Wire up tabs ──
        tabs.addTab("💬 Feedback", feedbackPanel);
        tabs.addTab("📤 Upload Paper", uploadPanel);
        root.add(tabs, BorderLayout.CENTER);

        frame.setContentPane(root);
        frame.setVisible(true);

        // ── Feedback logic ──
        Runnable updateCount = () -> {
            String r = httpGet(BASE_URL + "/api/feedback/unread-count");
            if (r != null) {
                try {
                    long cnt = new JSONObject(r).optLong("data", 0);
                    SwingUtilities.invokeLater(() -> countBadge.setText(cnt + " unread"));
                } catch (Exception ignored) {}
            }
        };
        Runnable loadAll    = () -> { populateTable(model, httpGet(BASE_URL + "/api/feedback/all"),    actionStatus); updateCount.run(); };
        Runnable loadUnread = () -> { populateTable(model, httpGet(BASE_URL + "/api/feedback/unread"), actionStatus); updateCount.run(); };

        new SwingWorker<Void,Void>() { @Override protected Void doInBackground() { loadAll.run(); return null; } }.execute();

        refreshBtn.addActionListener(e -> { refreshBtn.setEnabled(false);
            new SwingWorker<Void,Void>() {
                @Override protected Void doInBackground() { loadAll.run(); return null; }
                @Override protected void done() { refreshBtn.setEnabled(true); }
            }.execute();
        });
        allBtn.addActionListener(e ->    new SwingWorker<Void,Void>() { @Override protected Void doInBackground() { loadAll.run();    return null; } }.execute());
        unreadBtn.addActionListener(e -> new SwingWorker<Void,Void>() { @Override protected Void doInBackground() { loadUnread.run(); return null; } }.execute());

        markReadBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { actionStatus.setForeground(DANGER); actionStatus.setText("Please select a row first."); return; }
            long id = (Long) model.getValueAt(row, 0);
            markReadBtn.setEnabled(false);
            new SwingWorker<String,Void>() {
                @Override protected String doInBackground() { return httpPut(BASE_URL + "/api/feedback/" + id + "/read"); }
                @Override protected void done() {
                    markReadBtn.setEnabled(true);
                    try {
                        String r = get();
                        if (r != null && new JSONObject(r).optBoolean("success", false)) {
                            actionStatus.setForeground(SUCCESS); actionStatus.setText("✔ Marked as read.");
                            new SwingWorker<Void,Void>() { @Override protected Void doInBackground() { loadAll.run(); return null; } }.execute();
                        } else { actionStatus.setForeground(DANGER); actionStatus.setText("Failed."); }
                    } catch (Exception ex) { actionStatus.setForeground(DANGER); actionStatus.setText("Error: " + ex.getMessage()); }
                }
            }.execute();
        });

        // ── Upload Paper logic ──
        uploadPaperBtn.addActionListener(e -> {
            String subject    = subjectField.getText().trim();
            String paperCode  = paperCodeField.getText().trim();
            String yearStr    = yearField.getText().trim();
            String semester   = semesterField.getText().trim();
            String examType   = examTypeField.getText().trim();
            String marksStr   = totalMarksField.getText().trim();
            if (subject.isEmpty() || paperCode.isEmpty() || yearStr.isEmpty()) {
                uploadStatus.setForeground(DANGER); uploadStatus.setText("Subject, Paper Code and Year are required."); return;
            }
            int year = 0; int totalMarks = 0;
            try { year = Integer.parseInt(yearStr); } catch (NumberFormatException ex) { uploadStatus.setForeground(DANGER); uploadStatus.setText("Year must be a number."); return; }
            try { if (!marksStr.isEmpty()) totalMarks = Integer.parseInt(marksStr); } catch (NumberFormatException ex) { uploadStatus.setForeground(DANGER); uploadStatus.setText("Total Marks must be a number."); return; }
            uploadPaperBtn.setEnabled(false); uploadPaperBtn.setText("Uploading…");
            final int finalYear = year; final int finalMarks = totalMarks;
            String body = "{\"subject\":\"" + escape(subject) + "\","
                    + "\"paperCode\":\"" + escape(paperCode) + "\","
                    + "\"year\":" + finalYear + ","
                    + "\"semester\":\"" + escape(semester) + "\","
                    + "\"examType\":\"" + escape(examType) + "\","
                    + "\"totalMarks\":" + finalMarks + "}";
            new SwingWorker<String,Void>() {
                @Override protected String doInBackground() { return postJson(BASE_URL + "/api/questions/papers", body); }
                @Override protected void done() {
                    uploadPaperBtn.setEnabled(true); uploadPaperBtn.setText("📤 Upload Paper");
                    try {
                        String resp = get();
                        if (resp == null) { uploadStatus.setForeground(DANGER); uploadStatus.setText("Server unreachable."); return; }
                        JSONObject json = new JSONObject(resp);
                        if (json.optBoolean("success", false)) {
                            uploadStatus.setForeground(SUCCESS); uploadStatus.setText("✔ Paper uploaded! Paper Code: " + paperCode);
                            subjectField.setText(""); paperCodeField.setText(""); yearField.setText("");
                            semesterField.setText(""); examTypeField.setText(""); totalMarksField.setText("");
                            qPaperCodeField.setText(paperCode);
                        } else { uploadStatus.setForeground(DANGER); uploadStatus.setText(json.optString("message", "Upload failed.")); }
                    } catch (Exception ex) { uploadStatus.setForeground(DANGER); uploadStatus.setText("Error: " + ex.getMessage()); }
                }
            }.execute();
        });

        // ── Add Question logic ──
        addQuestionBtn.addActionListener(e -> {
            String qPaperCode = qPaperCodeField.getText().trim();
            String qText      = qTextArea.getText().trim();
            String qMarksStr  = qMarksField.getText().trim();
            if (qPaperCode.isEmpty() || qText.isEmpty() || qMarksStr.isEmpty()) {
                qStatus.setForeground(DANGER); qStatus.setText("All fields are required."); return;
            }
            int marks = 0;
            try { marks = Integer.parseInt(qMarksStr); } catch (NumberFormatException ex) { qStatus.setForeground(DANGER); qStatus.setText("Marks must be a number."); return; }
            addQuestionBtn.setEnabled(false); addQuestionBtn.setText("Adding…");
            final int finalMarks = marks;
            String body = "{\"paperCode\":\"" + escape(qPaperCode) + "\","
                    + "\"questionText\":\"" + escape(qText) + "\","
                    + "\"marks\":" + finalMarks + "}";
            new SwingWorker<String,Void>() {
                @Override protected String doInBackground() { return postJson(BASE_URL + "/api/questions", body); }
                @Override protected void done() {
                    addQuestionBtn.setEnabled(true); addQuestionBtn.setText("➕ Add Question");
                    try {
                        String resp = get();
                        if (resp == null) { qStatus.setForeground(DANGER); qStatus.setText("Server unreachable."); return; }
                        JSONObject json = new JSONObject(resp);
                        if (json.optBoolean("success", false)) {
                            qStatus.setForeground(SUCCESS); qStatus.setText("✔ Question added successfully!");
                            qTextArea.setText(""); qMarksField.setText("");
                        } else { qStatus.setForeground(DANGER); qStatus.setText(json.optString("message", "Failed to add question.")); }
                    } catch (Exception ex) { qStatus.setForeground(DANGER); qStatus.setText("Error: " + ex.getMessage()); }
                }
            }.execute();
        });
    }

    private static void populateTable(DefaultTableModel model, String resp, JLabel statusLabel) {
        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            if (resp == null) { statusLabel.setForeground(DANGER); statusLabel.setText("Failed to load data."); return; }
            try {
                JSONArray arr = new JSONObject(resp).getJSONArray("data");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject f = arr.getJSONObject(i);
                    model.addRow(new Object[]{
                            f.getLong("id"),
                            f.optString("submittedBy", "—"),
                            f.optString("subjectCode", "—"),
                            f.optString("message", ""),
                            f.optBoolean("isRead") ? "✔" : "✗",
                            f.optString("submittedAt", "").replace("T", " ").replaceAll("\\..*", "")
                    });
                }
                statusLabel.setForeground(Color.GRAY);
                statusLabel.setText("Loaded " + arr.length() + " record(s).");
            } catch (Exception ex) { statusLabel.setForeground(DANGER); statusLabel.setText("Parse error: " + ex.getMessage()); }
        });
    }

    private static JPanel makeTopBar(String title, JFrame frame) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(PRIMARY);
        bar.setPreferredSize(new Dimension(0, 56));
        bar.setBorder(new EmptyBorder(0, 16, 0, 16));
        JLabel lbl = new JLabel("📚 ExamPrep  —  " + title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(WHITE);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 14));
        right.setOpaque(false);
        JLabel user = new JLabel("👤 " + currentDisplayName + " (" + currentRole + ")");
        user.setForeground(new Color(186, 230, 253));
        user.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutBtn.setForeground(WHITE);
        logoutBtn.setBackground(new Color(59, 130, 246));
        logoutBtn.setBorder(new EmptyBorder(4, 12, 4, 12));
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> { frame.dispose(); showLoginFrame(); });
        right.add(user); right.add(logoutBtn);
        bar.add(lbl, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private static String postJson(String url, String json) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("POST"); c.setDoOutput(true);
            c.setConnectTimeout(5000); c.setReadTimeout(8000);
            c.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = c.getOutputStream()) { os.write(json.getBytes(StandardCharsets.UTF_8)); }
            return readResponse(c);
        } catch (Exception e) { return null; }
    }

    private static String httpGet(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(5000); c.setReadTimeout(8000);
            return readResponse(c);
        } catch (Exception e) { return null; }
    }

    private static String httpPut(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("PUT");
            c.setConnectTimeout(5000); c.setReadTimeout(8000);
            c.setRequestProperty("Content-Length", "0");
            return readResponse(c);
        } catch (Exception e) { return null; }
    }

    private static String readResponse(HttpURLConnection c) throws IOException {
        InputStream is = c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream();
        if (is == null) return "{}";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(TEXT_DARK);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
    private static JTextField makeTextField() {
        JTextField f = new JTextField(); styleTextField(f); return f;
    }
    private static void styleTextField(JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225), 1, true), new EmptyBorder(8, 10, 8, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
    }
    private static JButton makePrimaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBackground(PRIMARY); b.setForeground(WHITE);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 20, 10, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private static JButton makeSecondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(new Color(226, 232, 240)); b.setForeground(TEXT_DARK);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private static String uploadMultipart(String url, java.io.File file, String paperCode) {
        try {
            String boundary = "----ExamPrepBoundary" + System.currentTimeMillis();
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(10000);
            c.setReadTimeout(30000);
            c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream os = c.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

                // paperCode field
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"paperCode\"").append("\r\n\r\n");
                writer.append(paperCode).append("\r\n").flush();

                // file field
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                      .append(file.getName()).append("\"").append("\r\n");
                writer.append("Content-Type: application/pdf").append("\r\n\r\n").flush();
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buf = new byte[4096]; int n;
                    while ((n = fis.read(buf)) != -1) os.write(buf, 0, n);
                }
                os.flush();
                writer.append("\r\n").flush();
                writer.append("--").append(boundary).append("--").append("\r\n").flush();
            }
            return readResponse(c);
        } catch (Exception e) { return null; }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");
    }
}
