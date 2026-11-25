
package pe.tecnostock.view;

import pe.tecnostock.modules.reports.view.ReportsPanel;

import pe.tecnostock.modules.sales.view.SalesPanel;
import pe.tecnostock.modules.sales.view.ReceiptsPanel;

import pe.tecnostock.modules.inventory.view.InventoryPanel;

import pe.tecnostock.modules.products.view.ProductsPanel;

import pe.tecnostock.modules.customers.view.CustomersPanel;

import pe.tecnostock.modules.admin.view.EmployeesPanel;
import pe.tecnostock.modules.admin.view.PasswordResetPanel;

import pe.tecnostock.modules.admin.view.AuditPanel;


import pe.tecnostock.model.Role;
import pe.tecnostock.model.User;
import pe.tecnostock.modules.admin.repository.AdminDashboardRepository.DashboardStats;
import pe.tecnostock.repository.UserRepository;
import pe.tecnostock.modules.admin.service.AdminDashboardService;
import pe.tecnostock.service.SessionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

/**
 * Ventana principal de TecnoStock.
 *
 * Construye una interfaz diferenciada según el rol,
 * presenta indicadores reales al administrador y
 * verifica las sesiones al navegar.
 */
public class MainFrame extends JFrame {

    private static final Color NAVY =
            new Color(15, 23, 42);

    private static final Color NAVY_LIGHT =
            new Color(30, 41, 59);

    private static final Color ACCENT =
            new Color(37, 99, 235);

    private static final Color BACKGROUND =
            new Color(241, 245, 249);

    private static final Color TEXT =
            new Color(35, 49, 67);

    private static final Color MUTED =
            new Color(71, 85, 105);

    private final User currentUser;

    private final SessionService sessionService =
            new SessionService(new UserRepository());

    private final AdminDashboardService dashboardService =
            new AdminDashboardService();

    private final JPanel contentPanel =
            new JPanel(new BorderLayout(16, 16));

    private JLabel totalEmployeesValue;
    private JLabel activeEmployeesValue;
    private JLabel managersValue;
    private JLabel auditEventsValue;

    /**
     * Construye la ventana del usuario autenticado.
     */
    public MainFrame(User currentUser) {

        if (currentUser == null || !currentUser.isActive()) {
            throw new IllegalArgumentException(
                    "Se requiere un usuario activo."
            );
        }

        this.currentUser = currentUser;

        configureWindow();
        initializeComponents();
    }

    /**
     * Configura las propiedades de la ventana.
     */
    private void configureWindow() {

        setTitle(
                "TecnoStock - "
                + currentUser.getUsername()
                + " (" + currentUser.getRole() + ")"
        );

        setSize(1180, 760);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
     * Construye el menú lateral y el contenido.
     */
    private void initializeComponents() {

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);

        root.add(createSidebar(), BorderLayout.WEST);

        contentPanel.setBackground(BACKGROUND);
        contentPanel.setBorder(
                new EmptyBorder(22, 24, 22, 24)
        );

        root.add(contentPanel, BorderLayout.CENTER);

        setContentPane(root);

        showHome();
    }

    /**
     * Crea el menú lateral según los permisos.
     */
    private JPanel createSidebar() {

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(NAVY);
        sidebar.setPreferredSize(new Dimension(250, 0));

        JLabel brand = new JLabel("  TECNOSTOCK");
        brand.setForeground(Color.WHITE);
        brand.setFont(
                new Font("Segoe UI", Font.BOLD, 22)
        );
        brand.setBorder(
                new EmptyBorder(30, 15, 28, 15)
        );

        sidebar.add(brand, BorderLayout.NORTH);

        JPanel menu = new JPanel();
        menu.setLayout(
                new BoxLayout(menu, BoxLayout.Y_AXIS)
        );
        menu.setOpaque(false);
        menu.setBorder(
                new EmptyBorder(12, 14, 12, 14)
        );

        addSidebarButton(menu, "Inicio",
                () -> showHome());

        Role role = currentUser.getRole();

        // Los módulos comerciales serán compartidos
        // entre los roles según sus permisos.
        addSidebarButton(menu, "Clientes",
                () -> showCustomersPanel());

        addSidebarButton(menu, "Ventas",
                () -> showSalesPanel());

        addSidebarButton(menu, "Boletas",
                () -> showReceiptsPanel());

        if (role == Role.EMPLEADO) {

            addSidebarButton(
                    menu,
                    "Consultar productos",
                    () -> showProductsPanel()
            );

            addSidebarButton(
                    menu,
                    "Mis ventas",
                    () -> showSalesPanel()
            );
        }

        if (role == Role.GERENTE || role == Role.ADMIN) {

            addSidebarButton(
                    menu,
                    "Productos",
                    () -> showProductsPanel()
            );

            addSidebarButton(
                    menu,
                    "Inventario",
                    () -> showInventoryPanel()
            );

            addSidebarButton(
                    menu,
                    "Reportes",
                    () -> showReportsPanel()
            );
        }

        if (role == Role.ADMIN) {

            addSidebarButton(menu, "Empleados", () ->
                    showEmployeesPanel()
            );

            addSidebarButton(
                    menu,
                    "Restablecer contraseñas",
                    () -> showPasswordResetPanel()
            );

            addSidebarButton(menu, "Auditoría", () ->
                    showAuditPanel()
            );
        }

        JScrollPane menuScroll = new JScrollPane(menu);
        menuScroll.setBorder(BorderFactory.createEmptyBorder());
        menuScroll.setOpaque(false);
        menuScroll.getViewport().setBackground(NAVY);
        menuScroll.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        menuScroll.getVerticalScrollBar().setUnitIncrement(16);

        sidebar.add(menuScroll, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setLayout(
                new BoxLayout(footer, BoxLayout.Y_AXIS)
        );

        footer.setBackground(NAVY);
        footer.setBorder(
                new EmptyBorder(15, 14, 22, 14)
        );

        JLabel account = new JLabel(
                currentUser.getUsername()
                + "  |  " + currentUser.getRole()
        );

        account.setForeground(
                new Color(180, 201, 221)
        );
        account.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton logoutButton =
                createNavigationButton("Cerrar sesión");

        logoutButton.addActionListener(
                event -> logout()
        );

        footer.add(account);
        footer.add(Box.createVerticalStrut(12));
        footer.add(logoutButton);

        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    /**
     * Agrega un botón de navegación al menú.
     *
     * Antes de ejecutar cualquier opción se valida
     * la sesión contra SQLite.
     */
    private void addSidebarButton(
            JPanel menu,
            String text,
            Runnable action
    ) {

        JButton button = createNavigationButton(text);

        button.addActionListener(event ->
                openProtectedModule(action)
        );

        menu.add(button);
        menu.add(Box.createVerticalStrut(7));
    }

    /**
     * Crea botones uniformes para la barra lateral.
     */
    private JButton createNavigationButton(String text) {

        JButton button = new JButton(text);

        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 43)
        );

        button.setPreferredSize(
                new Dimension(208, 43)
        );

        button.setHorizontalAlignment(
                SwingConstants.LEFT
        );

        button.setFont(
                new Font("Segoe UI", Font.PLAIN, 14)
        );

        button.setForeground(Color.WHITE);
        button.setBackground(NAVY_LIGHT);
        button.setBorderPainted(false);
        button.setMargin(new Insets(10, 16, 10, 12));
        button.setFocusPainted(false);
        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        button.putClientProperty(
                "JButton.buttonType",
                "roundRect"
        );

        return button;
    }

    /**
     * Presenta la pantalla inicial del usuario.
     */
    private void showHome() {

        contentPanel.removeAll();

        String heading;

        if (currentUser.getRole() == Role.ADMIN) {
            heading = "Panel administrativo";
        } else if (currentUser.getRole() == Role.GERENTE) {
            heading = "Panel de gerencia";
        } else {
            heading = "Panel de ventas";
        }

        JPanel header = createHeader(
                heading,
                "Bienvenido, " + currentUser.getUsername()
        );

        contentPanel.add(header, BorderLayout.NORTH);

        if (currentUser.getRole() == Role.ADMIN) {

            contentPanel.add(
                    createAdminDashboard(),
                    BorderLayout.CENTER
            );

            loadDashboardStatistics();

        } else {
            // Los paneles del gerente y empleado
            // recibirán indicadores propios
            // al desarrollar sus módulos.
            contentPanel.add(
                    createWelcomePanel(),
                    BorderLayout.CENTER
            );
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Construye el título y subtítulo de cada pantalla.
     */
    private JPanel createHeader(
            String title,
            String subtitle
    ) {

        JPanel header = new JPanel();
        header.setLayout(
                new BoxLayout(header, BoxLayout.Y_AXIS)
        );
        header.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(
                new Font("Segoe UI", Font.BOLD, 32)
        );
        titleLabel.setForeground(TEXT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(MUTED);
        subtitleLabel.setFont(
                new Font("Segoe UI", Font.PLAIN, 14)
        );

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(7));
        header.add(subtitleLabel);

        return header;
    }

    /**
     * Construye las tarjetas y accesos rápidos
     * exclusivos del administrador.
     */
    private JPanel createAdminDashboard() {

        JPanel dashboard = new JPanel(
                new BorderLayout(18, 22)
        );
        dashboard.setOpaque(false);

        JPanel statsGrid = new JPanel(
                new GridLayout(2, 2, 18, 18)
        );
        statsGrid.setOpaque(false);

        totalEmployeesValue = createValueLabel();
        activeEmployeesValue = createValueLabel();
        managersValue = createValueLabel();
        auditEventsValue = createValueLabel();

        statsGrid.add(
                createStatCard(
                        "Empleados registrados",
                        totalEmployeesValue
                )
        );

        statsGrid.add(
                createStatCard(
                        "Cuentas activas",
                        activeEmployeesValue
                )
        );

        statsGrid.add(
                createStatCard(
                        "Gerentes activos",
                        managersValue
                )
        );

        statsGrid.add(
                createStatCard(
                        "Eventos de auditoría",
                        auditEventsValue
                )
        );

        dashboard.add(statsGrid, BorderLayout.CENTER);

        JPanel actions = new JPanel(
                new GridLayout(0, 1, 10, 10)
        );
        actions.setBackground(Color.WHITE);
        actions.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(3, 1, 1, 1, ACCENT),
                        new EmptyBorder(20, 20, 20, 20)
                )
        );

        JLabel actionsTitle = new JLabel(
                "Accesos rápidos"
        );
        actionsTitle.setFont(
                new Font("Segoe UI", Font.BOLD, 18)
        );
        actionsTitle.setForeground(TEXT);

        actions.add(actionsTitle);

        JButton employeesButton =
                createActionButton("Gestionar empleados");

        employeesButton.addActionListener(event ->
                openProtectedModule(() ->
                        showEmployeesPanel()
                )
        );

        JButton auditButton =
                createActionButton("Consultar auditoría");

        auditButton.addActionListener(event ->
                openProtectedModule(() ->
                        showAuditPanel()
                )
        );

        JButton refreshButton =
                createActionButton("Actualizar indicadores");

        refreshButton.addActionListener(event ->
                openProtectedModule(
                        () -> loadDashboardStatistics()
                )
        );

        actions.add(employeesButton);
        actions.add(auditButton);
        actions.add(refreshButton);

        dashboard.add(actions, BorderLayout.SOUTH);

        return dashboard;
    }

    /**
     * Crea una tarjeta visual para un indicador.
     */
    private JPanel createStatCard(
            String title,
            JLabel value
    ) {

        JPanel card = new JPanel(
                new BorderLayout(8, 8)
        );

        card.setBackground(Color.WHITE);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(3, 1, 1, 1, ACCENT),
                        new EmptyBorder(22, 24, 22, 24)
                )
        );

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(MUTED);
        titleLabel.setFont(
                new Font("Segoe UI", Font.PLAIN, 14)
        );

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);

        return card;
    }

    /**
     * Crea la etiqueta numérica de una tarjeta.
     */
    private JLabel createValueLabel() {

        JLabel label = new JLabel("—");

        label.setFont(
                new Font("Segoe UI", Font.BOLD, 44)
        );

        label.setForeground(ACCENT);

        return label;
    }

    /**
     * Crea botones para los accesos rápidos.
     */
    private JButton createActionButton(String text) {

        JButton button = new JButton(text);
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);

        button.setFont(
                new Font("Segoe UI", Font.BOLD, 14)
        );

        button.setPreferredSize(
                new Dimension(220, 43)
        );

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        button.putClientProperty(
                "JButton.buttonType",
                "roundRect"
        );

        return button;
    }

    /**
     * Consulta las estadísticas en segundo plano
     * para evitar bloquear la interfaz de Swing.
     */
    private void loadDashboardStatistics() {

        totalEmployeesValue.setText("...");
        activeEmployeesValue.setText("...");
        managersValue.setText("...");
        auditEventsValue.setText("...");

        SwingWorker<DashboardStats, Void> worker =
                new SwingWorker<>() {

            @Override
            protected DashboardStats doInBackground()
                    throws SQLException {

                return dashboardService.getStatistics(
                        currentUser
                );
            }

            @Override
            protected void done() {

                try {
                    DashboardStats stats = get();

                    totalEmployeesValue.setText(
                            String.valueOf(
                                    stats.totalEmployees()
                            )
                    );

                    activeEmployeesValue.setText(
                            String.valueOf(
                                    stats.activeEmployees()
                            )
                    );

                    managersValue.setText(
                            String.valueOf(
                                    stats.managers()
                            )
                    );

                    auditEventsValue.setText(
                            String.valueOf(
                                    stats.auditEvents()
                            )
                    );

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();
                    showStatisticsError();

                } catch (ExecutionException e) {

                    showStatisticsError();
                    e.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    /**
     * Informa que no fue posible cargar los indicadores.
     */
    private void showStatisticsError() {

        totalEmployeesValue.setText("—");
        activeEmployeesValue.setText("—");
        managersValue.setText("—");
        auditEventsValue.setText("—");

        JOptionPane.showMessageDialog(
                this,
                "No se pudieron cargar los indicadores.",
                "TecnoStock",
                JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * Presenta una bienvenida temporal para los roles
     * cuyos dashboards se desarrollarán después.
     */
    private JPanel createWelcomePanel() {

        JPanel panel = new JPanel(
                new BorderLayout()
        );

        panel.setBackground(Color.WHITE);
        panel.setBorder(
                new EmptyBorder(35, 35, 35, 35)
        );

        JLabel message = new JLabel(
                "<html><h2>Bienvenido a TecnoStock</h2>"
                + "<p>Tu espacio de trabajo está listo.</p>"
                + "<p>Utiliza el menú lateral para navegar.</p>"
                + "</html>"
        );

        message.setForeground(TEXT);
        panel.add(message, BorderLayout.NORTH);

        return panel;
    }

    /**
     * Muestra una pantalla informativa para módulos
     * que todavía no están implementados.
     */
    /**
 * Presenta la auditoría dentro del dashboard.
 *
 * Mantiene visible el menú lateral y reemplaza
 * solamente el contenido central de la ventana.
 */
private void showAuditPanel() {

    contentPanel.removeAll();

    contentPanel.add(
            createHeader(
                    "Auditoría administrativa",
                    "Historial de actividades del sistema"
            ),
            BorderLayout.NORTH
    );

    contentPanel.add(
            new AuditPanel(currentUser),
            BorderLayout.CENTER
    );

    contentPanel.revalidate();
    contentPanel.repaint();
}

    /**
     * Muestra el CRUD de empleados dentro de MainFrame.
     *
     * Conserva el menu lateral y permite utilizar
     * la busqueda, paginacion, edicion y desactivacion.
     */
    private void showEmployeesPanel() {

        contentPanel.removeAll();

        contentPanel.add(
                createHeader(
                        "Gestion de empleados",
                        "Administracion de personal y cuentas"
                ),
                BorderLayout.NORTH
        );

        contentPanel.add(
                new EmployeesPanel(currentUser),
                BorderLayout.CENTER
        );

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Muestra el restablecimiento de contrasenas
     * dentro del dashboard administrativo.
     */
    private void showPasswordResetPanel() {

        contentPanel.removeAll();

        contentPanel.add(
                createHeader(
                        "Restablecer contrasenas",
                        "Selecciona un gerente o empleado"
                ),
                BorderLayout.NORTH
        );

        contentPanel.add(
                new PasswordResetPanel(currentUser),
                BorderLayout.CENTER
        );

        contentPanel.revalidate();
        contentPanel.repaint();
    }
    /**
     * Presenta la gestión de clientes dentro de la ventana principal.
     * Conserva el menú lateral y la sesión del usuario autenticado.
     */
    private void showCustomersPanel() {

        contentPanel.removeAll();

        contentPanel.add(
                createHeader(
                        "Gestión de clientes",
                        "Registro, búsqueda y administración de clientes"
                ),
                BorderLayout.NORTH
        );

        contentPanel.add(
                new CustomersPanel(currentUser),
                BorderLayout.CENTER
        );

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Presenta el catálogo de productos y servicios.
     * Las operaciones disponibles dependen de los permisos
     * del usuario autenticado.
     */
    private void showProductsPanel() {

        contentPanel.removeAll();

        contentPanel.add(
                createHeader(
                        "Productos y servicios",
                        "Consulta y gestión del catálogo comercial"
                ),
                BorderLayout.NORTH
        );

        contentPanel.add(
                new ProductsPanel(currentUser),
                BorderLayout.CENTER
        );

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Presenta el control de inventario dentro de la ventana principal.
     * Permite gestionar existencias y consultar movimientos
     * según los permisos del usuario autenticado.
     */
    private void showInventoryPanel() {

        contentPanel.removeAll();

        contentPanel.add(
                createHeader(
                        "Gestión de inventario",
                        "Control de existencias y movimientos de stock"
                ),
                BorderLayout.NORTH
        );

        contentPanel.add(
                new InventoryPanel(currentUser),
                BorderLayout.CENTER
        );

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Presenta el registro de ventas dentro de la ventana principal.
     * Conserva la sesión y los permisos del usuario autenticado.
     */
    private void showSalesPanel() {

        contentPanel.removeAll();

        contentPanel.add(
                createHeader(
                        "Gestión de ventas",
                        "Registro de operaciones comerciales"
                ),
                BorderLayout.NORTH
        );

        contentPanel.add(
                new SalesPanel(currentUser),
                BorderLayout.CENTER
        );

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Presenta la consulta de comprobantes de venta.
     * Permite acceder a las operaciones disponibles según el rol.
     */
    private void showReceiptsPanel() {

        contentPanel.removeAll();

        contentPanel.add(
                createHeader(
                        "Boletas de venta",
                        "Consulta y exportación de comprobantes"
                ),
                BorderLayout.NORTH
        );

        contentPanel.add(
                new ReceiptsPanel(currentUser),
                BorderLayout.CENTER
        );

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /** Presenta los reportes comerciales en la ventana principal. */
    private void showReportsPanel() {
        contentPanel.removeAll();
        contentPanel.add(createHeader("Reportes de ventas", "Consulta de indicadores y exportación"), BorderLayout.NORTH);
        contentPanel.add(new ReportsPanel(currentUser), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    

    /**
     * Revisa la sesión antes de permitir la navegación.
     */
    private void openProtectedModule(Runnable action) {

        try {
            if (!sessionService.isSessionValid(currentUser)) {

                JOptionPane.showMessageDialog(
                        this,
                        "Tu sesión ya no es válida. "
                        + "Inicia sesión nuevamente.",
                        "Sesión finalizada",
                        JOptionPane.WARNING_MESSAGE
                );

                returnToLogin();
                return;
            }

            action.run();

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "No se pudo verificar la sesión.",
                    "Error de seguridad",
                    JOptionPane.ERROR_MESSAGE
            );

            returnToLogin();
        }
    }

    /**
     * Confirma el cierre de sesión.
     */
    private void logout() {

        int option = JOptionPane.showConfirmDialog(
                this,
                "¿Deseas cerrar sesión?",
                "Cerrar sesión",
                JOptionPane.YES_NO_OPTION
        );

        if (option == JOptionPane.YES_OPTION) {
            returnToLogin();
        }
    }

    /**
     * Cierra las ventanas de la sesión anterior
     * y muestra un nuevo formulario de acceso.
     */
    private void returnToLogin() {

        for (Window window : Window.getWindows()) {

            if (window.isDisplayable()) {
                window.dispose();
            }
        }

        new LoginFrame().setVisible(true);
    }
}
