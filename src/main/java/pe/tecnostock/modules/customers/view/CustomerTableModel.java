package pe.tecnostock.modules.customers.view;

import pe.tecnostock.model.Customer;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Entrega a la tabla los clientes que se están mostrando.
 */
class CustomerTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "Documento", "Tipo", "Nombre / Razón social",
            "Teléfono", "Correo", "Estado", "ID"
    };

    private List<Customer> customers = new ArrayList<>();

    /** Actualiza los registros visibles en la tabla. */
    void setCustomers(List<Customer> customers) {
        this.customers = new ArrayList<>(customers);
        fireTableDataChanged();
    }

    /** Devuelve el cliente de la fila solicitada. */
    Customer getCustomer(int row) {
        return customers.get(row);
    }

    @Override
    public int getRowCount() {
        return customers.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int row, int column) {

        Customer customer = customers.get(row);

        return switch (column) {
            case 0 -> customer.getDocument();
            case 1 -> customer.getDocumentType();
            case 2 -> customer.getName();
            case 3 -> customer.getPhone() == null
                    ? "" : customer.getPhone();
            case 4 -> customer.getEmail() == null
                    ? "" : customer.getEmail();
            case 5 -> customer.isActive() ? "Activo" : "Inactivo";
            case 6 -> customer.getId(); default -> "";
        };
    }
}