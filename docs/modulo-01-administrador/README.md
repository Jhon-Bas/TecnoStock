# Módulo 01 — Administración de usuarios

## Descripción

Este módulo forma parte del sistema TecnoStock y proporciona las
funcionalidades de autenticación, autorización, gestión administrativa
de empleados y usuarios, auditoría y restablecimiento de contraseñas.

El módulo se desarrolla con Java 21, Swing, FlatLaf, Maven y SQLite.

## Funcionalidades implementadas

- Inicio y cierre de sesión.
- Acceso condicionado por roles.
- Configuración inicial del sistema.
- Registro y edición de empleados.
- Búsqueda y paginación de registros.
- Modificación de roles.
- Activación y desactivación lógica de cuentas.
- Restablecimiento administrativo de contraseñas.
- Cambio obligatorio de contraseña temporal.
- Registro y consulta de auditoría.
- Control de vigencia de sesiones.
- Dashboard con indicadores administrativos.
- Navegación integrada en una ventana principal.

## Componentes visuales

- MainFrame: ventana principal y navegación.
- LoginFrame: inicio de sesión.
- SetupFrame: configuración inicial.
- ChangePasswordFrame: cambio de contraseña.
- EmployeesPanel: administración de empleados.
- PasswordResetPanel: restablecimiento de contraseñas.
- AuditPanel: consulta de auditoría.

## Organización del módulo

Los componentes administrativos se agrupan bajo:

src/main/java/pe/tecnostock/modules/admin/

El módulo utiliza componentes compartidos de los paquetes
model, repository, service, config y util.

## Documentos relacionados

- arquitectura.md
- seguridad.md
- pruebas.md
- uml/diagrama-clases.puml
- uml/secuencia-login.puml

## Estado

Funcionalidades administrativas implementadas y verificadas
mediante pruebas automatizadas y ejecución manual.

Los módulos de clientes, productos, inventario, ventas
y reportes todavía forman parte de futuras etapas.