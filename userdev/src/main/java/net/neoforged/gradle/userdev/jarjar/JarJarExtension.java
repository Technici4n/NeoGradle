package net.neoforged.gradle.userdev.jarjar;

import net.neoforged.gradle.dsl.userdev.dependency.DependencyFilter;
import net.neoforged.gradle.dsl.userdev.dependency.DependencyVersionInformationHandler;
import net.neoforged.gradle.userdev.tasks.JarJar;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.provider.Property;
import org.gradle.api.publish.maven.MavenPublication;

import javax.inject.Inject;
import java.util.Optional;

public abstract class JarJarExtension implements net.neoforged.gradle.dsl.userdev.extension.JarJar {

    private final Attribute<String> fixedJarJarVersionAttribute = Attribute.of("fixedJarJarVersion", String.class);
    private final Attribute<String> jarJarRangeAttribute = Attribute.of("jarJarRange", String.class);

    private final Project project;
    private boolean disableDefaultSources;

    @Inject
    public JarJarExtension(final Project project) {
        this.project = project;
        getEnabled().convention(false);
    }

    @Override
    public abstract Property<Boolean> getEnabled();

    @Override
    public void enable() {
        disable(false);
    }

    @Override
    public void disable() {
        disable(true);
    }

    @Override
    public void disable(boolean disable) {
        getEnabled().set(!disable);
    }

    @Override
    public boolean getDefaultSourcesDisabled() {
        return this.disableDefaultSources;
    }

    @Override
    public void disableDefaultSources() {
        disableDefaultSources(true);
    }

    @Override
    public void disableDefaultSources(boolean value) {
        this.disableDefaultSources = value;
    }

    @Override
    public void fromRuntimeConfiguration() {
        enable();
        project.getTasks().withType(JarJar.class).configureEach(JarJar::fromRuntimeConfiguration);
    }

    @Override
    public void pin(Dependency dependency, String version) {
        enable();
        if (dependency instanceof ModuleDependency) {
            final ModuleDependency moduleDependency = (ModuleDependency) dependency;
            moduleDependency.attributes(attributeContainer -> attributeContainer.attribute(fixedJarJarVersionAttribute, version));
        }
    }

    @Override
    public Optional<String> getPin(Dependency dependency) {
        if (dependency instanceof ModuleDependency) {
            final ModuleDependency moduleDependency = (ModuleDependency) dependency;
            return Optional.ofNullable(moduleDependency.getAttributes().getAttribute(fixedJarJarVersionAttribute));
        }
        return Optional.empty();
    }

    @Override
    public void ranged(Dependency dependency, String range) {
        enable();
        if (dependency instanceof ModuleDependency) {
            final ModuleDependency moduleDependency = (ModuleDependency) dependency;
            moduleDependency.attributes(attributeContainer -> attributeContainer.attribute(jarJarRangeAttribute, range));
        }
    }

    @Override
    public Optional<String> getRange(Dependency dependency) {
        if (dependency instanceof ModuleDependency) {
            final ModuleDependency moduleDependency = (ModuleDependency) dependency;
            return Optional.ofNullable(moduleDependency.getAttributes().getAttribute(jarJarRangeAttribute));
        }
        return Optional.empty();
    }

    @Override
    public JarJarExtension dependencies(Action<DependencyFilter> c) {
        enable();
        project.getTasks().withType(JarJar.class).configureEach(jarJar -> jarJar.dependencies(c));
        return this;
    }

    @Override
    public JarJarExtension versionInformation(Action<DependencyVersionInformationHandler> c) {
        enable();
        project.getTasks().withType(JarJar.class).configureEach(jarJar -> jarJar.versionInformation(c));
        return this;
    }

    @Override
    public MavenPublication component(MavenPublication mavenPublication) {
        return component(mavenPublication, true);
    }

    public MavenPublication component(MavenPublication mavenPublication, boolean handleDependencies) {
        enable();
        project.getTasks().withType(JarJar.class).configureEach(task -> component(mavenPublication, task, false, handleDependencies));

        return mavenPublication;
    }

    public MavenPublication component(MavenPublication mavenPublication, JarJar task) {
        enable();
        return component(mavenPublication, task, true, true);
    }

    public MavenPublication cleanedComponent(MavenPublication mavenPublication, JarJar task, boolean handleDependencies) {
        enable();
        return component(mavenPublication, task, true, handleDependencies);
    }

    private MavenPublication component(MavenPublication mavenPublication, JarJar task, boolean handleCleaning) {
        return component(mavenPublication, task, handleCleaning, true);
    }

    private MavenPublication component(MavenPublication mavenPublication, JarJar task, boolean handleCleaning, boolean handleDependencies) {
        if (!task.isEnabled()) {
            return mavenPublication;
        }

        if (handleCleaning) {
            //TODO: Handle this gracefully somehow?
        }

        mavenPublication.artifact(task, mavenArtifact -> {
            mavenArtifact.setClassifier(task.getArchiveClassifier().get());
            mavenArtifact.setExtension(task.getArchiveExtension().get());
        });

        if (handleDependencies) {
            //TODO: Handle this gracefully.
        }

        return mavenPublication;
    }
}
