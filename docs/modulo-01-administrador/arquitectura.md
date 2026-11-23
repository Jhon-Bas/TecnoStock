# Arquitectura del módulo Administrador

## Enfoque arquitectónico

TecnoStock utiliza una arquitectura modular por capas,
organizada para separar presentación, lógica de negocio,
persistencia y configuración.

La organización está inspirada en MVC, pero algunas vistas
Swing coordinan directamente eventos y llamadas a servicios.
Por lo tanto, no se describe como MVC puro.

## Capas

### Presentación

Las clases Swing proporcionan formularios, navegación y
visualización de datos.

MainFrame conserva la ventana principal, mientras que
EmployeesPanel, PasswordResetPanel y AuditPanel presentan
las secciones administrativas dentro de ella.

### Servicios

Los servicios coordinan reglas de negocio y seguridad.

Componentes principales:

- AuthService
- AuthorizationService
- BootstrapService
- SessionService
- PasswordChangeService
- EmployeeService
- PasswordResetService
- AuditService
- AdminDashboardService

### Persistencia

Los repositorios encapsulan el acceso a datos.

Componentes principales:

- UserRepository
- EmployeeRepository
- AuditRepository
- AdminDashboardRepository

### Configuración y utilidades

La capa de configuración prepara SQLite y sus estructuras.
PasswordUtil concentra las operaciones criptográficas
relacionadas con las contraseñas.

## Organización del código

Paquetes principales:

- pe.tecnostock.config
- pe.tecnostock.model
- pe.tecnostock.repository
- pe.tecnostock.service
- pe.tecnostock.util
- pe.tecnostock.view
- pe.tecnostock.modules.admin.repository
- pe.tecnostock.modules.admin.service
- pe.tecnostock.modules.admin.view

## Flujo general

1. El usuario interactúa con una vista Swing.
2. La vista solicita una operación al servicio correspondiente.
3. El servicio valida las condiciones de negocio y los permisos.
4. El repositorio ejecuta las operaciones necesarias en SQLite.
5. El resultado regresa a la interfaz para su presentación.

## Decisiones de diseño

### Separación de responsabilidades

Las consultas SQL se concentran en repositorios y las
reglas administrativas se coordinan mediante servicios.

### Reutilización

Los componentes de autenticación y autorización son
compartidos por los módulos del sistema.

### Mantenibilidad

La organización modular facilita agregar funcionalidades
de inventario, productos, ventas y reportes sin concentrar
todo el sistema en una sola clase.

### Interfaz integrada

Se utilizan paneles Swing para evitar ventanas
administrativas independientes y mantener la navegación
dentro de MainFrame.

## Patrones previstos para siguientes etapas

Factory: creación de productos y servicios.

Facade: coordinación de operaciones de venta.

Proxy: control de acceso a reportes.

Command: operaciones del carrito.

Estos cuatro patrones no se consideran implementados
solo por existir servicios, repositorios o clases Swing.

## Nota sobre UML

Los diagramas adjuntos son representaciones
arquitectónicas de alto nivel.

Antes de incluirlos en el informe final, se revisarán
contra las firmas y dependencias reales del código fuente.