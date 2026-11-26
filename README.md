# TecnoStock

**Sistema de gestión de ventas e inventario** — Proyecto integrador de Diseño de Patrones, 2026-2.

TecnoStock es una aplicación de escritorio para un negocio de comercialización de tecnología. Integra administración de usuarios, atención al cliente, catálogo de productos y servicios, inventario, ventas y reportes en una interfaz gráfica con persistencia SQLite y control de acceso por roles.

## Objetivo

Facilitar las operaciones comerciales mediante una aplicación organizada, mantenible y extensible, aplicando programación orientada a objetos, principios SOLID y patrones de diseño.

## Tecnologías

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje y plataforma |
| Swing y FlatLaf 3.6 | Interfaz gráfica |
| SQLite JDBC 3.49.1.0 | Persistencia local |
| OpenPDF 2.0.3 | Exportación de documentos PDF |
| Maven | Dependencias, compilación y pruebas |
| JUnit 5.11.4 | Pruebas automatizadas |
| Git y GitHub | Control de versiones |
| PlantUML | Documentación de diseño |

## Instalación y ejecución

Requisitos: JDK 21, Maven, entorno gráfico compatible con Swing y permisos de escritura en el directorio del proyecto. La primera resolución de dependencias requiere conexión a Internet.

```bash
git clone https://github.com/Jhon-Bas/TecnoStock.git
cd TecnoStock
mvn clean verify
mvn exec:java
```

En el primer inicio, si no existen usuarios, el sistema presenta el formulario de configuración de la cuenta inicial. En los siguientes accesos presenta el formulario de inicio de sesión. Las migraciones preparan el esquema de SQLite al iniciar.

La base local `data/tecnostock.db` no se distribuye mediante Git. No eliminarla para actualizar el programa; conservar una copia de seguridad de los datos antes de cambios de versión.

Para construir el artefacto Maven: `mvn clean package`. Para iniciar con las dependencias gestionadas por Maven, utilizar `mvn exec:java`.

## Módulos

| Módulo | Funcionalidades |
|---|---|
| Administración | Empleados, cuentas, roles, estados, contraseñas temporales, auditoría e indicadores |
| Clientes | Registro, búsqueda y mantenimiento según permisos |
| Productos | Catálogo de productos y servicios, códigos comerciales, precios y estados |
| Inventario | Entradas, salidas, ajustes, existencias e historial de movimientos |
| Ventas | Carrito, cantidades, precios, descuentos autorizados, registro transaccional y boletas PDF |
| Reportes | Consultas comerciales, filtros por fechas y exportación PDF/CSV |

La ventana principal incluye indicadores y accesos rápidos diferenciados por rol. Los indicadores del empleado corresponden a operaciones propias recientes, no a totales históricos ilimitados.

## Roles y seguridad

| Operación | ADMIN | GERENTE | EMPLEADO |
|---|---|---|---|
| Administración de cuentas y auditoría | Sí | No | No |
| Consultar y registrar clientes | Sí | Sí | Sí |
| Gestionar clientes existentes | Sí | Sí | No |
| Consultar catálogo | Sí | Sí | Sí |
| Gestionar catálogo e inventario | Sí | Sí | No |
| Registrar ventas y consultar ventas propias | Sí | Sí | Sí |
| Consultar todas las ventas y aplicar descuentos | Sí | Sí | No |
| Consultar reportes | Sí | Sí | No |

Los permisos se definen en `PermissionPolicy` y se verifican en los servicios mediante `AuthorizationService`. El control visual no sustituye la autorización. Las contraseñas se protegen mediante PBKDF2 con salt; las sesiones se verifican y pueden invalidarse ante cambios sensibles en las cuentas. Los identificadores internos se generan en SQLite y su presentación se restringe al administrador; los códigos comerciales se conservan.

## Arquitectura

Organización modular por capas, con separación de interfaz gráfica, reglas de negocio y persistencia. No se afirma que sea una implementación de MVC puro.

```text
src/main/java/pe/tecnostock/
├── App.java
├── config/             # Conexión, inicialización, migraciones y transacciones
├── model/              # Entidades y enumeraciones
├── modules/
│   ├── admin/
│   ├── customers/
│   ├── inventory/
│   ├── products/
│   ├── reports/
│   └── sales/
├── repository/         # Persistencia compartida
├── security/           # Permisos
├── service/            # Autenticación, autorización y sesiones
├── util/               # Utilidades
└── view/               # Ventanas principales y estilos
src/test/java/          # Pruebas JUnit
```

Los módulos agrupan sus servicios, repositorios, vistas y otros componentes específicos. Los servicios coordinan las reglas del negocio; los repositorios encapsulan el acceso SQL. Las ventas y movimientos de inventario aprovechan transacciones para mantener consistencia. Los importes se representan en céntimos en la persistencia.

## Patrones de diseño

| Patrón | Categoría | Participantes principales | Beneficio |
|---|---|---|---|
| Factory Method | Creacional | `ItemCreator`, `ProductCreator`, `ServiceItemCreator`, `CommercialItemFactory` | Delega la construcción de los artículos en creadores concretos |
| Facade | Estructural | `SalesFacade`, servicios de clientes, catálogo y ventas | Simplifica la interacción de las vistas comerciales con los subsistemas |
| Protection Proxy | Estructural | `ReportAccess`, `ReportAccessProxy`, `ReportService` | Verifica autorización antes de delegar consultas |
| Command | Comportamiento | `CartCommand`, `CartCommandAction`, `CartCommandHistory` | Encapsula acciones y permite deshacer cambios del carrito antes de registrar la venta |

El diseño aplica encapsulamiento, abstracción, herencia y polimorfismo, además de separación de responsabilidades (SRP). Las interfaces de acceso a reportes y los comandos permiten desacoplar consumidores de implementaciones concretas. Las colecciones dinámicas se utilizan en consultas y en el historial de acciones.

## Pruebas y validación

```bash
mvn clean verify
```

Última ejecución reportada por el equipo (26 de noviembre de 2026): **112 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**. Se compilaron 81 archivos fuente de producción y 24 archivos fuente de pruebas.

Las pruebas cubren migraciones, transacciones, clientes, catálogo, inventario, ventas, comprobantes, reportes, seguridad y patrones de diseño. El equipo también reportó 25 comprobaciones manuales exitosas entre los tres roles y funcionalidades generales, antes de la última limpieza de nomenclatura. Estas evidencias no equivalen a una garantía de ausencia de defectos ni a cobertura total del código.

## Documentación y entrega

- [Documentación del módulo administrador](docs/modulo-01-administrador/README.md): referencia de la primera etapa, con arquitectura, seguridad, pruebas y diagramas PlantUML.
- Informe académico, UML integral y presentación: pendientes de consolidación para la entrega final.

## Control de versiones

Repositorio: <https://github.com/Jhon-Bas/TecnoStock>.

`main` es la rama principal. La integración de módulos y patrones se desarrolla en `feature/integracion-modulos`, con validaciones antes de fusionar los cambios mediante Pull Request.

**Estado:** implementación integrada y pruebas aprobadas; cierre de documentación académica pendiente.
