# Seguridad del módulo Administrador

## Autenticación

TecnoStock permite iniciar sesión mediante credenciales
de usuario y contraseña.

AuthService coordina la autenticación con los datos
consultados en UserRepository.

## Almacenamiento de contraseñas

Las contraseñas se procesan mediante PBKDF2 con salt,
utilizando PasswordUtil.

Las contraseñas no se almacenan como texto plano.

## Roles

El sistema utiliza los roles:

- ADMIN
- GERENTE
- EMPLEADO

El administrador dispone de operaciones de gestión de
usuarios y empleados.

Los accesos de gerente y empleado se completarán
con las funcionalidades de sus módulos correspondientes.

## Autorización

AuthorizationService aplica comprobaciones de acceso
relacionadas con la sesión y las operaciones protegidas.

No se considera suficiente ocultar botones de la interfaz:
los servicios también deben verificar permisos.

## Estado de las cuentas

La gestión administrativa contempla activación
y desactivación lógica de cuentas.

Una cuenta desactivada no debe conservar acceso válido
a las operaciones protegidas.

## Sesiones

SessionService comprueba la vigencia de las sesiones.

La versión de sesión permite invalidar sesiones
anteriores tras modificaciones relevantes de la cuenta,
como cambios de contraseña, rol o estado.

## Contraseñas temporales

PasswordResetService participa en el restablecimiento
administrativo de contraseñas.

Después de un restablecimiento, el usuario debe cambiar
su contraseña temporal según la política implementada.

PasswordChangeService coordina el proceso de cambio.

## Auditoría

Las operaciones administrativas relevantes se registran
para proporcionar trazabilidad.

La consulta de estos eventos se realiza desde AuditPanel,
mediante AuditService y AuditRepository.

## Protección de datos locales

El archivo data/tecnostock.db está excluido de Git.

No se deben subir al repositorio:

- Bases de datos con usuarios reales.
- Contraseñas o credenciales.
- Archivos temporales y respaldos con datos sensibles.

## Límites de seguridad

Las pruebas implementadas verifican reglas concretas
del módulo. No constituyen una auditoría integral de
ciberseguridad ni pruebas de penetración.