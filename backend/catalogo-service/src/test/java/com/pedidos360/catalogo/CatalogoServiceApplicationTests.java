package com.pedidos360.catalogo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.microsoftonline.com/00000000-0000-0000-0000-000000000000/v2.0"
})
class CatalogoServiceApplicationTests {

    @Test
    void contextLoads() {
        // confirma que la app arranca sin errores de configuracion
    }
}
