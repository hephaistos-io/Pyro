package io.hephaistos.flagforge.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import io.hephaistos.flagforge.data.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture tests to enforce proper usage of Hibernate-filtered repository methods.
 * <p>
 * The standard JpaRepository methods findById() and existsById() bypass Hibernate @Filter
 * annotations because they use EntityManager.find() internally. Services must use the explicitly
 * filtered methods (findByIdFiltered, existsByIdFiltered) to ensure proper multi-tenancy
 * isolation.
 */
@Tag("architecture")
class RepositoryFilterArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter().withImportOption(
                        ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("io.hephaistos.flagforge");
    }

    @Test
    @DisplayName("Services should not use unfiltered findById on ApplicationRepository")
    void servicesShouldNotUseUnfilteredFindByIdOnApplicationRepository() {
        ArchRule rule = noClasses().that()
                .resideInAPackage("..service..")
                .should()
                .callMethod(ApplicationRepository.class, "findById", UUID.class)
                .because(
                        "findById() bypasses Hibernate filters. Use findByIdFiltered() instead " + "to ensure proper company/application filtering for multi-tenancy isolation.");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Services should not use unfiltered existsById on ApplicationRepository")
    void servicesShouldNotUseUnfilteredExistsByIdOnApplicationRepository() {
        ArchRule rule = noClasses().that()
                .resideInAPackage("..service..")
                .should()
                .callMethod(ApplicationRepository.class, "existsById", UUID.class)
                .because(
                        "existsById() bypasses Hibernate filters. Use existsByIdFiltered() instead " + "to ensure proper company/application filtering for multi-tenancy isolation.");

        rule.check(importedClasses);
    }
}
