package pe.tecnostock.security;

import pe.tecnostock.config.Database;
import pe.tecnostock.config.migration.SchemaMigrator;
import pe.tecnostock.model.Role;
import pe.tecnostock.model.User;
import pe.tecnostock.modules.admin.repository.EmployeeRepository;
import pe.tecnostock.modules.admin.service.EmployeeService;
import pe.tecnostock.repository.UserRepository;
import pe.tecnostock.service.BootstrapService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Apoyo para pruebas: migra la base temporal activa y crea un
 * administrador, un gerente y un empleado listos para operar
 * (con su contraseña temporal ya cambiada).
 *
 * Debe llamarse después de BaseTemporal.usar(...).
 */
public final class TestUsers {

    public final User admin;
    public final User manager;
    public final User employee;

    private TestUsers(User admin, User manager, User employee) {
        this.admin = admin;
        this.manager = manager;
        this.employee = employee;
    }

    public static TestUsers create() throws SQLException {

        new SchemaMigrator().migrate();

        UserRepository users = new UserRepository();
        EmployeeService employees = new EmployeeService(
                new EmployeeRepository(), users
        );

        new BootstrapService().createInitialAdmin(
                "Administrador", "Prueba", "12345678",
                "admin_test", "AdminPrueba2026!".toCharArray()
        );
        User admin = users.findByUsername("admin_test")
                .orElseThrow().user();

        for (Object[] account : new Object[][] {
                {"gerente01", "11111111", Role.GERENTE},
                {"empleado01", "22222222", Role.EMPLEADO}}) {

            employees.createEmployee(
                    admin, "Empleado", "Prueba",
                    (String) account[1], (String) account[0],
                    (Role) account[2], "Temporal2026!".toCharArray()
            );
            confirmPasswordChange((String) account[0]);
        }

        return new TestUsers(
                admin,
                users.findByUsername("gerente01").orElseThrow().user(),
                users.findByUsername("empleado01").orElseThrow().user()
        );
    }

    private static void confirmPasswordChange(String currentUser)
            throws SQLException {

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE usuarios SET must_change_password = 0 "
                             + "WHERE username = ?"
             )) {
            statement.setString(1, currentUser);
            statement.executeUpdate();
        }
    }
}