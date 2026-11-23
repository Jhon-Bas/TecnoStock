# Pruebas del módulo Administrador

## Herramientas

- JUnit 5
- Apache Maven
- Maven Surefire

## Clases de prueba

| Clase | Cantidad | Resultado |
|---|---:|---|
| EmployeeITTest | 5 | Aprobadas |
| PasswordUtilTest | 3 | Aprobadas |
| SessionServiceTest | 5 | Aprobadas |
| **Total** | **13** | **Aprobadas** |

## Última verificación registrada

- Pruebas ejecutadas: 13.
- Fallos: 0.
- Errores: 0.
- Omitidas: 0.
- Resultado: BUILD SUCCESS.
- Fecha: 22 de noviembre de 2026.

## Ejecución

Desde la raíz del proyecto:

```powershell
mvn clean verify
```

## Alcance

Las pruebas verifican los comportamientos contenidos
en las tres clases de prueba mencionadas.

También se realizó comprobación manual de la
navegación y funcionamiento del módulo Administrador.

## Verificaciones visuales

- Inicio de sesión.
- Apertura del dashboard.
- Navegación hacia Empleados.
- Navegación hacia Auditoría.
- Navegación hacia Restablecer contraseñas.
- Permanencia del menú lateral.
- Retorno a Inicio y cierre de sesión.

## Evidencias

Los reportes XML se generan mediante Maven en:

target/surefire-reports/

La carpeta target es generada y no necesita versionarse.

## Pendiente para el proyecto completo

- Pruebas de clientes.
- Pruebas de productos.
- Pruebas de inventario.
- Pruebas de ventas.
- Pruebas de reportes.
- Pruebas de los patrones de diseño seleccionados.