package net.neoforged.gradle.neoform


import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class FunctionalTests extends BuilderBasedTestSpecification {

    private static final String NEOFORM_VERSION = "1.20.2-20230921.152923"

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.neoform";
        injectIntoAllProject = true;
    }

    def "a mod with neoform as dependency can run the apply official mappings task"() {
        given:
        def project = create "neoform-has-runnable-patch-task", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            dependencies {
                implementation 'net.minecraft:neoform_client:${NEOFORM_VERSION}'
            }
            """)
            it.settings("""
            plugins {
                id 'org.gradle.toolchains.foojay-resolver-convention' version '0.4.0'
            }
            """)
        }

        when:
        def run = project.run { it.tasks(':neoFormApplyOfficialMappings') }

        then:
        run.task(':neoFormApplyOfficialMappings').outcome == TaskOutcome.SUCCESS
    }

    def "neoform applies user ATs and allows remapped compiling"() {
        given:
        def project = create "neoform-compile-with-ats", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            minecraft {
                accessTransformers {
                    entry "public net.minecraft.client.Minecraft LOGGER # searchRegistry"
                }
            }
            
            dependencies {
                implementation 'net.minecraft:neoform_client:${NEOFORM_VERSION}'
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/neoform/FunctionalTests.java", """
            package net.neoforged.gradle.neoform;
            
            import net.minecraft.client.Minecraft;
            
            public class FunctionalTests {
                public static void main(String[] args) {
                    System.out.println(Minecraft.LOGGER.getClass().toString());
                }
            }
            """)
            it.settings("""
            plugins {
                id 'org.gradle.toolchains.foojay-resolver-convention' version '0.4.0'
            }
            """)
        }

        when:
        def run = project.run { it.tasks('build') }

        then:
        run.task(':build').outcome == TaskOutcome.SUCCESS
    }

    def "neoform re-setup uses a build-cache" () {
        given:
        def project = create "neoform-compile-with-ats", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            dependencies {
                implementation 'net.minecraft:neoform_client:${NEOFORM_VERSION}'
            }
            """)

            it.file("src/main/java/net/neoforged/gradle/neoform/FunctionalTests.java", """
            package net.neoforged.gradle.neoform;
            
            import net.minecraft.client.Minecraft;
            
            public class FunctionalTests {
                public static void main(String[] args) {
                    System.out.println(Minecraft.getInstance().getClass().toString());
                }
            }
            """)
            it.settings("""
            plugins {
                id 'org.gradle.toolchains.foojay-resolver-convention' version '0.4.0'
            }
            """)

            it.enableLocalBuildCache()
        }

        when:
        def run = project.run { it.tasks('build').arguments('--build-cache') }

        then:
        run.task(':build').outcome == TaskOutcome.SUCCESS

        when:
        new File(project.getProjectDir(), 'build').deleteDir()
        def secondRun = project.run {it.tasks('build').arguments('--build-cache') }

        then:
        secondRun.task(':build').outcome == TaskOutcome.SUCCESS
        secondRun.task(':neoFormRecompile').outcome == TaskOutcome.FROM_CACHE
    }


}
