package com.pedidos360.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// Este test simplemente confirma que la aplicacion completa (contexto de Spring,
// seguridad, JPA, controladores) levanta sin errores de configuracion.
@SpringBootTest
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.microsoftonline.com/00000000-0000-0000-0000-000000000000/v2.0"
})
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // si este test pasa, significa que toda la configuracion (BD, seguridad, beans)
        // esta correctamente definida y la aplicacion es capaz de arrancar.
    }
}
