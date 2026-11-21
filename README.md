# TecnoStock

**Sistema de Gestión de Ventas e Inventario**

Proyecto Integrador del curso de Diseño de Patrones, desarrollado en Java mediante programación orientada a objetos y patrones de diseño.

## Objetivo

Desarrollar una aplicación de escritorio que permita administrar clientes, productos, servicios, ventas e inventario, aplicando principios SOLID y patrones de diseño para conseguir una arquitectura organizada, mantenible y extensible.

## Tecnologías

- Java 21
- Visual Studio Code
- Apache Maven
- Java Swing
- SQLite (planificado)
- FlatLaf (planificado)
- JUnit (planificado)

## Arquitectura

Se utilizará una arquitectura MVC con separación de responsabilidades en capas:

- `model`: entidades del negocio.
- `view`: formularios e interfaces gráficas.
- `controller`: interacción entre vistas y servicios.
- `service`: lógica de negocio.
- `repository`: persistencia de datos.
- `patterns`: implementación de patrones de diseño.
- `config`: configuración de la aplicación.

## Patrones de diseño previstos

| Patrón | Categoría | Aplicación |
|---|---|---|
| Factory | Creacional | Creación de productos y servicios |
| Facade | Estructural | Gestión simplificada de operaciones de venta |
| Proxy | Estructural | Control de acceso a reportes |
| Command | Comportamiento | Operaciones del carrito de compras |

## Funcionalidades planificadas

- Gestión de clientes.
- Gestión de productos y servicios.
- Control de inventario.
- Registro de ventas.
- Cálculo de importes y descuentos.
- Generación de boletas.
- Consultas y reportes.

## Estado del proyecto

**En desarrollo — configuración inicial.**

Actualmente se ha generado la estructura base de Maven. Las funcionalidades y patrones descritos están pendientes de implementación.

## Ejecución

Requisitos:

- JDK 21
- Apache Maven

Para compilar el proyecto:

```bash
mvn clean install
```

## Equipo de desarrollo

- Integrantes: pendientes de agregar.
- Curso: Diseño de Patrones.
- Ciclo académico: 2026-2.

## Entrega

El proyecto final incluirá el código fuente Java, el informe PDF y la presentación de exposición.