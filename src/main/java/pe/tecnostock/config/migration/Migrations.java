package pe.tecnostock.config.migration;

import pe.tecnostock.config.AuditInitializer;
import pe.tecnostock.config.DatabaseInitializer;
import pe.tecnostock.config.PasswordPolicyInitializer;
import pe.tecnostock.config.SessionVersionInitializer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Lista ordenada de todas las migraciones de TecnoStock.
 *
 * Para cambiar el esquema se agrega una migración al final con la versión
 * siguiente. Nunca se edita ni se reordena una migración que ya fue compartida
 * con el equipo.
 */
public final class Migrations {

    private Migrations() {
        // Impide crear instancias de esta clase.
    }

    public static List<Migration> all() {
        return List.of(
                // V1 a V4 son la estructura que ya existía.
                // Son idempotentes, así que las bases actuales
                // las registran sin perder datos.
                new Migration(1,
                        "Tablas base de empleados y usuarios",
                        DatabaseInitializer::initialize),
                new Migration(2,
                        "Auditoría de cuentas",
                        AuditInitializer::initialize),
                new Migration(3,
                        "Contraseñas temporales",
                        PasswordPolicyInitializer::initialize),
                new Migration(4,
                        "Versión de sesión",
                        SessionVersionInitializer::initialize),
                new Migration(5,
                        "Tabla de clientes",
                        Migrations::createCustomers),
                new Migration(6,
                        "Categorías, productos y servicios",
                        Migrations::createCatalog),
                new Migration(7,
                        "Movimientos de inventario",
                        Migrations::createInventoryMovements),
                new Migration(8,
                        "Ventas y detalle de venta",
                        Migrations::createSales)
        );
    }

    private static void createSales(Connection connection)
            throws SQLException {

        try (Statement statement = connection.createStatement()) {

            statement.executeUpdate("""
            CREATE TABLE IF NOT EXISTS ventas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                numero_boleta TEXT NOT NULL UNIQUE,
                cliente_id INTEGER NOT NULL
                    REFERENCES clientes (id),
                usuario_id INTEGER NOT NULL
                    REFERENCES usuarios (id),
                subtotal_centimos INTEGER NOT NULL
                    CHECK (subtotal_centimos >= 0),
                descuento_centimos INTEGER NOT NULL DEFAULT 0
                    CHECK (descuento_centimos >= 0),
                total_centimos INTEGER NOT NULL
                    CHECK (total_centimos >= 0),
                estado TEXT NOT NULL DEFAULT 'EMITIDA'
                    CHECK (estado IN ('EMITIDA', 'ANULADA')),
                fecha TEXT NOT NULL
                    DEFAULT (datetime('now', 'localtime'))
            )
            """);

            statement.executeUpdate("""
            CREATE TABLE IF NOT EXISTS detalle_venta (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                venta_id INTEGER NOT NULL
                    REFERENCES ventas (id),
                tipo_item TEXT NOT NULL
                    CHECK (tipo_item IN ('PRODUCTO', 'SERVICIO')),
                item_id INTEGER NOT NULL,
                codigo_item TEXT NOT NULL,
                nombre_item TEXT NOT NULL,
                cantidad INTEGER NOT NULL
                    CHECK (cantidad > 0),
                precio_unitario_centimos INTEGER NOT NULL
                    CHECK (precio_unitario_centimos > 0),
                subtotal_centimos INTEGER NOT NULL
                    CHECK (subtotal_centimos > 0)
            )
            """);

            statement.executeUpdate("""
            CREATE INDEX IF NOT EXISTS idx_ventas_fecha
            ON ventas (fecha)
            """);

            statement.executeUpdate("""
            CREATE INDEX IF NOT EXISTS idx_ventas_usuario
            ON ventas (usuario_id)
            """);

            statement.executeUpdate("""
            CREATE INDEX IF NOT EXISTS idx_detalle_venta
            ON detalle_venta (venta_id)
            """);
        }
    }

    private static void createInventoryMovements(Connection connection)
            throws SQLException {

        try (Statement statement = connection.createStatement()) {

            // Cada fila explica un cambio de stock: qué pasó, cuánto,
            // cómo estaba antes, cómo quedó, quién y por qué. La base
            // exige que esas cifras sean coherentes entre sí.
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS movimientos_inventario (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    producto_id INTEGER NOT NULL
                        REFERENCES productos (id),
                    tipo TEXT NOT NULL
                        CHECK (tipo IN ('ENTRADA', 'SALIDA', 'AJUSTE')),
                    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
                    stock_anterior INTEGER NOT NULL
                        CHECK (stock_anterior >= 0),
                    stock_resultante INTEGER NOT NULL
                        CHECK (stock_resultante >= 0),
                    usuario_id INTEGER NOT NULL
                        REFERENCES usuarios (id),
                    motivo TEXT NOT NULL
                        CHECK (length(trim(motivo)) > 0),
                    fecha TEXT NOT NULL
                        DEFAULT (datetime('now', 'localtime')),
                    CHECK (
                        (tipo = 'ENTRADA'
                            AND stock_resultante = stock_anterior + cantidad)
                        OR (tipo = 'SALIDA'
                            AND stock_resultante = stock_anterior - cantidad)
                        OR (tipo = 'AJUSTE'
                            AND abs(stock_resultante - stock_anterior)
                                = cantidad)
                    )
                )
                """);

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_movimientos_producto
                ON movimientos_inventario (producto_id, id)
                """);

            // El historial es una constancia: no se edita ni se borra.
            statement.executeUpdate("""
                CREATE TRIGGER IF NOT EXISTS trg_movimientos_sin_editar
                BEFORE UPDATE ON movimientos_inventario
                BEGIN
                    SELECT RAISE(ABORT,
                        'Los movimientos de inventario no se pueden modificar.');
                END
                """);

            statement.executeUpdate("""
                CREATE TRIGGER IF NOT EXISTS trg_movimientos_sin_borrar
                BEFORE DELETE ON movimientos_inventario
                BEGIN
                    SELECT RAISE(ABORT,
                        'Los movimientos de inventario no se pueden borrar.');
                END
                """);
        }
    }

    private static void createCatalog(Connection connection)
            throws SQLException {

        try (Statement statement = connection.createStatement()) {

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS categorias (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL UNIQUE
                        CHECK (length(trim(nombre)) > 0)
                )
                """);

            statement.executeUpdate("""
                INSERT OR IGNORE INTO categorias (nombre) VALUES
                    ('Laptops'), ('Periféricos'),
                    ('Componentes'), ('Accesorios')
                """);

            // Los precios se guardan en céntimos (enteros) para que
            // no existan errores de redondeo. El stock solo cambiará
            // mediante movimientos de inventario.
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS productos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    codigo TEXT NOT NULL UNIQUE,
                    nombre TEXT NOT NULL
                        CHECK (length(trim(nombre)) > 0),
                    descripcion TEXT,
                    categoria_id INTEGER NOT NULL
                        REFERENCES categorias (id),
                    precio_compra_centimos INTEGER NOT NULL DEFAULT 0
                        CHECK (precio_compra_centimos >= 0),
                    precio_venta_centimos INTEGER NOT NULL
                        CHECK (precio_venta_centimos > 0),
                    stock_actual INTEGER NOT NULL DEFAULT 0
                        CHECK (stock_actual >= 0),
                    stock_minimo INTEGER NOT NULL DEFAULT 0
                        CHECK (stock_minimo >= 0),
                    activo INTEGER NOT NULL DEFAULT 1
                        CHECK (activo IN (0, 1)),
                    fecha_registro TEXT NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT
                )
                """);

            // Los servicios no tienen stock ni categoría.
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS servicios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    codigo TEXT NOT NULL UNIQUE,
                    nombre TEXT NOT NULL
                        CHECK (length(trim(nombre)) > 0),
                    descripcion TEXT,
                    precio_venta_centimos INTEGER NOT NULL
                        CHECK (precio_venta_centimos > 0),
                    activo INTEGER NOT NULL DEFAULT 1
                        CHECK (activo IN (0, 1)),
                    fecha_registro TEXT NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT
                )
                """);

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_productos_nombre
                ON productos (nombre)
                """);

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_servicios_nombre
                ON servicios (nombre)
                """);
        }
    }

    private static void createCustomers(Connection connection)
            throws SQLException {

        try (Statement statement = connection.createStatement()) {

            // El documento es un DNI (8 dígitos) o un RUC (11).
            // Los clientes se desactivan, no se borran, para
            // conservar el historial de sus ventas.
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS clientes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    documento TEXT NOT NULL UNIQUE
                        CHECK (length(documento) IN (8, 11)
                               AND documento NOT GLOB '*[^0-9]*'),
                    nombre TEXT NOT NULL
                        CHECK (length(trim(nombre)) > 0),
                    telefono TEXT,
                    correo TEXT,
                    direccion TEXT,
                    activo INTEGER NOT NULL DEFAULT 1
                        CHECK (activo IN (0, 1)),
                    fecha_registro TEXT NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT
                )
                """);

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_clientes_nombre
                ON clientes (nombre)
                """);
        }
    }
}
