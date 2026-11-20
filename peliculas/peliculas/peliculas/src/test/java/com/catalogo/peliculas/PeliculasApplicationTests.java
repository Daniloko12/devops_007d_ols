package com.catalogo.peliculas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Verifica que el contexto completo de Spring arranca (con H2 del perfil "test")
@SpringBootTest
@ActiveProfiles("test")
class PeliculasApplicationTests {

	@Test
	void contextLoads() {
		// Si algún bean o configuración falla, esta prueba falla al iniciar el contexto
	}

}
