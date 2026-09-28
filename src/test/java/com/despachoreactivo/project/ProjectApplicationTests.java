package com.despachoreactivo.project;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integración: requiere PostgreSQL (p. ej. {@code docker compose up -d}).
 * Ejecutar con {@code ./gradlew integrationTest}.
 */
@Tag("integration")
@SpringBootTest
class ProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
