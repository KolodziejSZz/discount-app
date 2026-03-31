package pl.discountapp;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static final String BASE_PACKAGE = "pl.discountapp";
    private static final String DOMAIN_PACKAGE = "pl.discountapp.domain..";
    private static final String APPLICATION_PACKAGE = "pl.discountapp.application..";
    private static final String INFRASTRUCTURE_PACKAGE = "pl.discountapp.infrastructure..";
    private static final String PORT_INPUT_PACKAGE = "pl.discountapp.application.port.input..";
    private static final String PORT_OUTPUT_PACKAGE = "pl.discountapp.application.port.output..";
    private static final String ADAPTER_PACKAGE = "pl.discountapp.infrastructure.adapter..";
    private static final String ADAPTER_WEB_PACKAGE = "pl.discountapp.infrastructure.adapter.web..";
    private static final String ADAPTER_PERSISTENCE_PACKAGE = "pl.discountapp.infrastructure.adapter.persistence..";
    private static final String ADAPTER_GEOLOCATION_PACKAGE = "pl.discountapp.infrastructure.adapter.geolocation..";

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Nested
    @DisplayName("Domain Layer Isolation")
    class DomainLayerIsolation {

        @Test
        void testDomainShouldNotDependOnSpring() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.."
                    );

            rule.check(importedClasses);
        }

        @Test
        void testDomainShouldNotDependOnApplication() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage(APPLICATION_PACKAGE);

            rule.check(importedClasses);
        }

        @Test
        void testDomainShouldNotDependOnInfrastructure() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE);

            rule.check(importedClasses);
        }

        @Test
        void testDomainShouldNotDependOnPersistenceFrameworks() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "jakarta.persistence..",
                            "org.hibernate..",
                            "javax.persistence.."
                    );

            rule.check(importedClasses);
        }

        @Test
        void testDomainShouldOnlyDependOnJavaAndLombok() {
            ArchRule rule = classes()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "java..",
                            "lombok..",
                            DOMAIN_PACKAGE
                    );

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Application Layer Rules")
    class ApplicationLayerRules {

        @Test
        void testApplicationShouldNotDependOnInfrastructure() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(APPLICATION_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE);

            rule.check(importedClasses);
        }

        @Test
        void testApplicationShouldNotDependOnSpring() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(APPLICATION_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.."
                    );

            rule.check(importedClasses);
        }

        @Test
        void testApplicationShouldOnlyDependOnDomainAndSelf() {
            ArchRule rule = classes()
                    .that().resideInAPackage(APPLICATION_PACKAGE)
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "java..",
                            "lombok..",
                            DOMAIN_PACKAGE,
                            APPLICATION_PACKAGE
                    );

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Infrastructure Layer Rules")
    class InfrastructureLayerRules {

        @Test
        void testAdaptersWebShouldNotDependOnPersistence() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(ADAPTER_WEB_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage(ADAPTER_PERSISTENCE_PACKAGE);

            rule.check(importedClasses);
        }

        @Test
        void testAdaptersPersistenceShouldNotDependOnWeb() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(ADAPTER_PERSISTENCE_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAPackage(ADAPTER_WEB_PACKAGE);

            rule.check(importedClasses);
        }

        @Test
        void testAdaptersGeolocationShouldNotDependOnOtherAdapters() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(ADAPTER_GEOLOCATION_PACKAGE)
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            ADAPTER_WEB_PACKAGE,
                            ADAPTER_PERSISTENCE_PACKAGE
                    );

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Ports Rules")
    class PortsRules {

        @Test
        void testInputPortsShouldBeInterfaces() {
            ArchRule rule = classes()
                    .that().resideInAPackage(PORT_INPUT_PACKAGE)
                    .should().beInterfaces();

            rule.check(importedClasses);
        }

        @Test
        void testOutputPortsShouldBeInterfaces() {
            ArchRule rule = classes()
                    .that().resideInAPackage(PORT_OUTPUT_PACKAGE)
                    .should().beInterfaces();

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("DDD Tactical Patterns")
    class DddTacticalPatterns {

        @Test
        void testDomainRepositoryPortsShouldBeInterfaces() {
            ArchRule rule = classes()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .and().haveSimpleNameEndingWith("Repository")
                    .should().beInterfaces();

            rule.check(importedClasses);
        }

        @Test
        void testValueObjectsShouldBeRecords() {
            ArchRule rule = classes()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .and().haveSimpleNameEndingWith("Id")
                    .should().beRecords();

            rule.check(importedClasses);
        }

        @Test
        void testValueObjectCodeShouldBeRecord() {
            ArchRule rule = classes()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .and().haveSimpleName("CouponCode")
                    .should().beRecords();

            rule.check(importedClasses);
        }

        @Test
        void testValueObjectCountryShouldBeRecord() {
            ArchRule rule = classes()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .and().haveSimpleName("Country")
                    .should().beRecords();

            rule.check(importedClasses);
        }

        @Test
        void testDomainExceptionsShouldExtendRuntimeException() {
            ArchRule rule = classes()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .and().haveSimpleNameEndingWith("Exception")
                    .should().beAssignableTo(RuntimeException.class);

            rule.check(importedClasses);
        }

        @Test
        void testAggregateRootShouldNotBeRecord() {
            ArchRule rule = classes()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .and().haveSimpleName("Coupon")
                    .should().notBeRecords();

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Dependency Direction")
    class DependencyDirection {

        @Test
        void testInfrastructureShouldNotBeAccessedByDomain() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(DOMAIN_PACKAGE)
                    .should().accessClassesThat()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE);

            rule.check(importedClasses);
        }

        @Test
        void testInfrastructureShouldNotBeAccessedByApplication() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage(APPLICATION_PACKAGE)
                    .should().accessClassesThat()
                    .resideInAPackage(INFRASTRUCTURE_PACKAGE);

            rule.check(importedClasses);
        }
    }
}
