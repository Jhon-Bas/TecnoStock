
package pe.tecnostock.modules.admin.view;

import pe.tecnostock.model.User;
import pe.tecnostock.modules.admin.repository.AuditRepository;
import pe.tecnostock.modules.admin.service.AuditService;
import pe.tecnostock.repository.UserRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.concurrent.ExecutionException;

/**
 * Panel integrado para consultar la auditoría.
 *
 * No abre una ventana independiente.
 * Se muestra dentro del área central de MainFrame.
 */
public class AuditPanel extends JPanel {

    private static final int PAGE_SIZE = 10;

    private final User currentUser;

    private final AuditService service = new AuditService(
            new AuditRepository(),
            new UserRepository()
    );

    private final JTextField searchField = new JTextField();

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new String[]{
                            "ID", "Fecha", "Responsable",
                            "Cuenta afectada", "Acción", "Detalle"
                    },
                    0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JLabel pageLabel =
            new JLabel("Página 1 de 1");

    private final JButton previousButton =
            new JButton("Anterior");

    private final JButton nextButton =
            new JButton("Siguiente");

    private final JButton searchButton =
            new JButton("Buscar");

    private int currentPage = 0;
    private boolean loading = false;

    /**
     * Construye el panel con la sesión autenticada.
     */
    public AuditPanel(User currentUser) {

        this.currentUser = currentUser;

        setLayout(new BorderLayout(12, 12));
        setOpaque(false);

        initializeComponents();
        loadEvents();
    }

    /**
     * Construye la búsqueda, tabla y controles de página.
     */
    private void initializeComponents() {

        JPanel searchPanel = new JPanel(
                new BorderLayout(10, 0)
        );

        searchPanel.setOpaque(false);

        searchField.putClientProperty(
                "JTextField.placeholderText",
                "Buscar responsable, cuenta o acción"
        );

        searchButton.addActionListener(event -> {
            currentPage = 0;
            loadEvents();
        });

        searchField.addActionListener(event -> {
            currentPage = 0;
            loadEvents();
        });

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        JTable table = new JTable(tableModel);
        table.setRowHeight(29);
        table.setAutoCreateRowSorter(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int[] widths = {55, 160, 150, 150, 160, 360};

        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(widths[i]);
        }

        JScrollPane scrollPane = new JScrollPane(table);

        previousButton.addActionListener(event -> {
            if (currentPage > 0) {
                currentPage--;
                loadEvents();
            }
        });

        nextButton.addActionListener(event -> {
            currentPage++;
            loadEvents();
        });

        JPanel pagination = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        pagination.setOpaque(false);
        pagination.add(previousButton);
        pagination.add(pageLabel);
        pagination.add(nextButton);

        JPanel tablePanel = new JPanel(
                new BorderLayout(10, 10)
        );

        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        tablePanel.add(searchPanel, BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(pagination, BorderLayout.SOUTH);

        add(tablePanel, BorderLayout.CENTER);
    }

    /**
     * Consulta la auditoría fuera del hilo visual de Swing.
     *
     * Evita que una consulta lenta congele el dashboard.
     */
    private void loadEvents() {

        if (loading) {
            return;
        }

        loading = true;

        final int requestedPage = currentPage;
        final String search = searchField.getText();

        previousButton.setEnabled(false);
        nextButton.setEnabled(false);
        searchButton.setEnabled(false);

        SwingWorker<AuditRepository.AuditPage, Void> worker =
                new SwingWorker<>() {

            @Override
            protected AuditRepository.AuditPage doInBackground()
                    throws Exception {

                return service.listEvents(
                        currentUser,
                        search,
                        requestedPage,
                        PAGE_SIZE
                );
            }

            @Override
            protected void done() {

                loading = false;
                searchButton.setEnabled(true);

                try {
                    AuditRepository.AuditPage result = get();

                    int totalPages = Math.max(
                            1,
                            (result.total() + PAGE_SIZE - 1)
                                    / PAGE_SIZE
                    );

                    if (currentPage >= totalPages) {
                        currentPage = totalPages - 1;
                        loadEvents();
                        return;
                    }

                    tableModel.setRowCount(0);

                    for (var event : result.events()) {
                        tableModel.addRow(new Object[]{
                                event.id(),
                                event.date(),
                                event.actor(),
                                event.target(),
                                event.action(),
                                event.details()
                        });
                    }

                    pageLabel.setText(
                            "Página " + (currentPage + 1)
                            + " de " + totalPages
                            + " | Total: " + result.total()
                    );

                    previousButton.setEnabled(
                            currentPage > 0
                    );

                    nextButton.setEnabled(
                            currentPage < totalPages - 1
                    );

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();
                    showError("Consulta interrumpida.");

                } catch (ExecutionException e) {

                    Throwable cause = e.getCause();

                    if (cause instanceof SecurityException) {
                        showError(
                                "Tu sesión ya no tiene permisos. "
                                + "Inicia sesión nuevamente."
                        );
                    } else {
                        showError(
                                "No se pudo consultar la auditoría."
                        );
                    }

                    if (cause != null) {
                        java.util.logging.Logger.getLogger(AuditPanel.class.getName()).log(java.util.logging.Level.SEVERE, "Error durante la operacion.", cause);
                    }
                }
            }
        };

        worker.execute();
    }

    /**
     * Muestra un mensaje de error sin modificar los datos.
     */
    private void showError(String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "Auditoría",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
