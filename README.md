# TecnoStock

**Sistema de Gestión de Ventas e Inventario**

Proyecto Integrador del curso de **Diseño de Patrones**, desarrollado en Java mediante programación orientada a objetos, arquitectura por capas, principios SOLID y patrones de diseño.

## 1. Descripción del proyecto

TecnoStock es una aplicación de escritorio orientada a la gestión de ventas, productos, clientes e inventario.

El sistema busca facilitar las operaciones de un negocio mediante una interfaz gráfica, almacenamiento persistente de información, control de acceso por roles y organización modular del código.

Actualmente se encuentra en desarrollo. La primera etapa funcional corresponde al **módulo de autenticación, seguridad y administración de usuarios**.

## 2. Objetivo

Desarrollar una aplicación de escritorio que permita administrar clientes, productos, servicios, ventas e inventario, aplicando principios de programación orientada a objetos, SOLID y patrones de diseño para conseguir una arquitectura organizada, mantenible, segura y extensible.

### Objetivos específicos

- Implementar autenticación y autorización mediante roles.
- Gestionar usuarios y empleados desde un módulo administrativo.
- Administrar clientes, productos y servicios.
- Controlar existencias y movimientos de inventario.
- Registrar ventas y calcular importes.
- Generar comprobantes y reportes.
- Persistir la información en una base de datos SQLite.
- Aplicar patrones de diseño de manera funcional y justificable.
- Verificar las principales reglas del sistema mediante pruebas automatizadas.

## 3. Tecnologías utilizadas

| Tecnología | Aplicación | Estado |
|---|---|---|
| Java 21 | Lenguaje de programación | Implementado |
| Visual Studio Code | Entorno de desarrollo | En uso |
| Apache Maven | Dependencias, compilación y pruebas | Implementado |
| Java Swing | Interfaz gráfica de escritorio | Implementado |
| FlatLaf | Apariencia visual de los formularios | Implementado |
| SQLite | Persistencia de datos | Implementado |
| JUnit 5 | Pruebas automatizadas | Implementado |
| Git y GitHub | Control de versiones y colaboración | En uso |
| PlantUML | Diagramas UML de documentación | Previsto |

## 4. Arquitectura del sistema

TecnoStock utiliza una arquitectura modular por capas, con separación entre interfaz gráfica, lógica de negocio, persistencia y configuración.

Su organización sigue principios de MVC, aunque actualmente algunas clases Swing también coordinan eventos de interfaz. Por ello, no se considera todavía una implementación de MVC puro.

### Capas principales

- **model:** entidades y enumeraciones del dominio.
- **view:** ventanas principales, inicio de sesión y formularios compartidos.
- **modules:** funcionalidades agrupadas por módulo.
- **service:** reglas de negocio, autenticación, autorización y sesiones.
- **repository:** consultas y operaciones de persistencia.
- **config:** conexión e inicialización de SQLite.
- **util:** funciones auxiliares y operaciones criptográficas.

### Estructura actual simplificada

```text
TecnoStock/
├── data/
│   └── tecnostock.db
├── src/
│   ├── main/
│   │   └── java/
│   │       └── pe/
│   │           └── tecnostock/
│   │               ├── App.java
│   │               ├── config/
│   │               ├── model/
│   │               ├── modules/
│   │               │   └── admin/
│   │               │       ├── repository/
│   │               │       ├── service/
│   │               │       └── view/
│   │               ├── repository/
│   │               ├── service/
│   │               ├── util/
│   │               └── view/
│   └── test/
│       └── java/
│           └── pe/
│               └── tecnostock/
│                   └── security/
├── pom.xml
└── README.md
```

La base de datos local se excluye del repositorio mediante `.gitignore`.

## 5. Roles de usuario

El sistema contempla tres roles principales.

| Rol | Responsabilidades |
|---|---|
| ADMIN | Administración de usuarios, empleados, roles, estados, contraseñas, auditoría y acceso al panel administrativo |
| GERENTE | Gestión operativa, inventario y reportes, conforme se implementen los módulos |
| EMPLEADO | Operaciones de ventas y atención, conforme se implemente el punto de venta |

El acceso a las funcionalidades depende del rol autenticado. La autorización también se comprueba en los servicios de negocio, no únicamente mediante la visibilidad de botones.

## 6. Funcionalidades implementadas

### 6.1. Autenticación

- Inicio de sesión mediante usuario y contraseña.
- Configuración inicial del sistema.
- Identificación del rol del usuario autenticado.
- Navegación según el rol.
- Cierre de sesión.
- Cambio obligatorio de contraseña temporal.

### 6.2. Seguridad

- Almacenamiento de contraseñas mediante PBKDF2 con salt.
- Validación de políticas de contraseña.
- Verificación de permisos desde los servicios.
- Validación del estado activo de la cuenta.
- Control de versiones de sesión.
- Invalidación de sesiones ante modificaciones sensibles de la cuenta.

### 6.3. Administración de empleados y usuarios

- Registro de empleados.
- Consulta y búsqueda.
- Listado con paginación.
- Edición de información.
- Asignación y modificación de roles.
- Activación y desactivación lógica.
- Registro de información de creación y modificación.
- Restricciones de autorización para operaciones administrativas.

Los registros se desactivan lógicamente cuando corresponde, evitando eliminaciones físicas innecesarias.

### 6.4. Restablecimiento de contraseñas

- Búsqueda de cuentas.
- Selección de usuarios.
- Restablecimiento administrativo de contraseñas.
- Uso de contraseñas temporales.
- Cambio obligatorio de contraseña en el siguiente acceso.
- Invalidación de sesiones previas según las reglas de seguridad.

### 6.5. Auditoría

- Registro de eventos administrativos relevantes.
- Consulta del historial de acciones.
- Búsqueda de eventos.
- Paginación de resultados.
- Asociación de operaciones con usuarios responsables.

### 6.6. Dashboard administrativo

- Interfaz principal con menú lateral.
- Indicadores obtenidos de la base de datos.
- Acceso a la gestión de empleados.
- Acceso al restablecimiento de contraseñas.
- Acceso al historial de auditoría.
- Navegación integrada mediante paneles Swing, sin abrir ventanas administrativas independientes.

## 7. Persistencia de datos

TecnoStock utiliza **SQLite** para almacenar la información del sistema.

Se han implementado componentes para inicializar la base de datos y preparar las estructuras necesarias para autenticación, auditoría, políticas de contraseñas y control de sesiones.

El módulo administrativo utiliza repositorios para separar las operaciones SQL de la lógica de negocio.

El archivo local `data/tecnostock.db` no se incluye en el control de versiones.

## 8. Patrones de diseño previstos

Los siguientes patrones han sido seleccionados para su aplicación en las próximas etapas del sistema.

| Patrón | Categoría | Aplicación prevista |
|---|---|---|
| Factory | Creacional | Creación de productos y servicios |
| Facade | Estructural | Simplificación de operaciones relacionadas con ventas |
| Proxy | Estructural | Control de acceso a reportes |
| Command | Comportamiento | Representación de operaciones del carrito de compras |

**Estado:** seleccionados y pendientes de implementación funcional.

La arquitectura por capas y los servicios actuales no se presentan como sustitutos de estos patrones. Cada patrón deberá implementarse y justificarse mediante clases, funcionamiento y diagramas UML.

## 9. Funcionalidades pendientes

El desarrollo continuará con los siguientes componentes:

- Gestión de clientes.
- Catálogo de productos y servicios.
- Control de inventario y existencias.
- Movimientos de stock.
- Registro de ventas.
- Carrito y detalle de venta.
- Cálculo de subtotales, descuentos y totales.
- Generación de boletas o comprobantes.
- Consultas y reportes.
- Implementación de Factory, Facade, Proxy y Command.
- Pruebas de los nuevos módulos.
- Documentación UML integral.

## 10. Pruebas automatizadas

Se utiliza **JUnit 5** junto con Maven.

La revisión del módulo de autenticación y administración obtuvo los siguientes resultados:

| Clase de pruebas | Pruebas aprobadas |
|---|---:|
| EmployeeITTest | 5 |
| PasswordUtilTest | 3 |
| SessionServiceTest | 5 |
| **Total** | **13** |

**Resultado de la última verificación:** 13 pruebas ejecutadas, 0 fallos, 0 errores y `BUILD SUCCESS`.

Estas pruebas cubren aspectos administrativos y de seguridad. No representan todavía una validación completa de los módulos futuros de ventas, clientes e inventario.

## 11. Compilación y ejecución

### Requisitos

- JDK 21.
- Apache Maven.
- Sistema operativo compatible con Java Swing.

### Compilar y ejecutar pruebas

```bash
mvn clean verify
```

### Iniciar la aplicación

```bash
mvn exec:java
```

### Compilar el proyecto sin ejecutar la aplicación

```bash
mvn clean package
```

La aplicación prepara las estructuras de persistencia necesarias durante su inicialización.

## 12. Control de versiones

Repositorio:

https://github.com/Jhon-Bas/TecnoStock

Organización de trabajo:

- `main`: rama principal estable.
- `jhon-develop`: rama personal de desarrollo.
- `feature/autenticacion`: desarrollo del módulo de autenticación y administración.

Los cambios deben verificarse antes de integrarse a la rama principal.

## 13. Estado actual del proyecto

**En desarrollo — primera etapa funcional verificada.**

### Completado

- Configuración del proyecto Maven.
- Arquitectura inicial por capas.
- Persistencia SQLite.
- Autenticación y autorización por roles.
- Seguridad de contraseñas.
- Control de sesiones.
- Administración de empleados y usuarios.
- Restablecimiento administrativo de contraseñas.
- Auditoría.
- Dashboard administrativo con paneles integrados.
- Trece pruebas automatizadas aprobadas.

### En desarrollo o pendiente

- Módulos operativos para gerente y empleado.
- Productos, servicios y clientes.
- Inventario y ventas.
- Comprobantes y reportes.
- Patrones de diseño seleccionados.
- Diagramas UML y documentación final del sistema.

## 14. Equipo de desarrollo

- **Integrantes:** pendientes de completar.
- **Curso:** Diseño de Patrones.
- **Ciclo académico:** 2026-2.

## 15. Entregables del proyecto

La entrega final contempla:

- Código fuente Java.
- Proyecto Maven.
- Documentación técnica.
- Diagramas UML.
- Informe final en PDF.
- Presentación para exposición.
- Evidencias de ejecución y pruebas.

---

**TecnoStock — Proyecto Integrador de Diseño de Patrones, 2026-2.**