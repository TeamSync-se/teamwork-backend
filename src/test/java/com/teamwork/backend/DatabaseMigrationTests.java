package com.teamwork.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DatabaseMigrationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Flyway flyway;

	@Test
	void usesSamePostgresVersionAsComposeFile() {
		String version = jdbcTemplate.queryForObject("SHOW server_version", String.class);

		assertThat(version).startsWith("17.");
	}

	@Test
	void appliesAllFlywayMigrations() {
		assertThat(flyway.info().applied()).isNotEmpty();
		assertThat(flyway.info().pending()).isEmpty();

		Boolean usersTableExists = jdbcTemplate.queryForObject(
				"SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users')",
				Boolean.class);
		assertThat(usersTableExists).isTrue();
	}

}
