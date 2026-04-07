package hp.soft;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("hp.soft");
    }

    @Test
    void services_should_have_at_most_4_dependencies() {
        classes().that().areAnnotatedWith(Service.class)
                .should(haveAtMostInjectedDependencies(4))
                .because("services with more than 4 dependencies should be split")
                .check(classes);
    }

    @Test
    void dslContext_only_in_repository_packages() {
        noClasses().that().resideOutsideOfPackage("..repository..")
                .and().resideOutsideOfPackage("..config..")
                .should().dependOnClassesThat().areAssignableTo(DSLContext.class)
                .because("DSLContext should only be used in repository or config classes")
                .check(classes);
    }

    @Test
    void dslContext_only_in_classes_named_repository() {
        noClasses().that().haveSimpleNameNotEndingWith("Repository")
                .and().haveSimpleNameNotEndingWith("Config")
                .should().dependOnClassesThat().areAssignableTo(DSLContext.class)
                .because("only classes named *Repository or *Config should use DSLContext")
                .check(classes);
    }

    private static ArchCondition<JavaClass> haveAtMostInjectedDependencies(int max) {
        return new ArchCondition<>("have at most " + max + " injected dependencies") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                long count = javaClass.getFields().stream()
                        .filter(this::isInjectedDependency)
                        .count();
                if (count > max) {
                    events.add(SimpleConditionEvent.violated(javaClass,
                            "%s has %d injected dependencies (max %d)".formatted(
                                    javaClass.getSimpleName(), count, max)));
                }
            }

            private boolean isInjectedDependency(JavaField field) {
                return !field.getModifiers().contains(JavaModifier.STATIC)
                        && field.getModifiers().contains(JavaModifier.FINAL)
                        && !field.getRawType().isPrimitive()
                        && !field.getRawType().getPackageName().startsWith("java.");
            }
        };
    }
}
