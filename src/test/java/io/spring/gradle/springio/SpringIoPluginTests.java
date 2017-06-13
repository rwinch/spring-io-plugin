package io.spring.gradle.springio;

import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import io.spring.gradle.propdeps.PropDepsPlugin;
import org.assertj.core.api.Condition;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.project.DefaultProject;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringIoPlugin}.
 *
 * @author Rob Winch
 * @author Andy Wilkinson
 */
public class SpringIoPluginTests {

	private Project project;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private File jdk7Home;

	private File jdk8Home;

	private File jdk9Home;

	private File java7;

	private File java8;

	private File java9;

	@Before
	public void setup() throws IOException {
		this.jdk7Home = this.tempFolder.newFolder();
		this.java7 = new File(jdk7Home, "bin/java");
		this.java7.getParentFile().mkdirs();
		this.java7.createNewFile();

		this.jdk8Home = this.tempFolder.newFolder();
		this.java8 = new File(jdk8Home, "bin/java");
		this.java8.getParentFile().mkdirs();
		this.java8.createNewFile();

		this.jdk9Home = this.tempFolder.newFolder();
		this.java9 = new File(jdk9Home, "bin/java");
		this.java9.getParentFile().mkdirs();
		this.java9.createNewFile();

		this.project = ProjectBuilder.builder().withProjectDir(this.tempFolder.newFolder()).build();
	}

	@Test
	public void pluginCanBeAppliedToNonJavaProject() {
		applyPlugin(SpringIoPlugin.class);
		assertThat(project.getConfigurations().findByName("springIoTestRuntime")).isNull();
	}

	@Test
	public void pluginCanBeAppliedToGroovyProject() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(GroovyPlugin.class);
		project.getConfigurations().getByName("springIoTestRuntime");
	}

	@Test
	public void pluginSetsUpSpringIoTestRuntimeConfiguration() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		Configuration configuration = project.getConfigurations().getByName("springIoTestRuntime");
		assertThat(configuration.getExtendsFrom()).contains(project.getConfigurations().getByName("testRuntime"));
	}

	@Test
	public void pluginAppliesTheDependencyManagementPlugin() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		assertThat(project.getPlugins().findPlugin(DependencyManagementPlugin.class)).isNotNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void pluginCreatesTheSpringIoCheckTask() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		Task task = project.getTasks().findByName("springIoCheck");
		assertThat(task).isNotNull();
		assertThat((Set<Task>)task.getTaskDependencies().getDependencies(task)).containsExactlyInAnyOrder(
				project.getTasks().findByName("springIoTest"),
				project.getTasks().findByName("springIoAlternativeDependenciesCheck"),
				project.getTasks().findByName("springIoDependencyVersionMappingCheck"),
				project.getTasks().findByName("springIoIncompleteExcludesCheck"));
	}

	@Test
	public void pluginCreatesSpringIoJdk7TestTaskWhenJdk7IsAvailable() {
		ExtraPropertiesExtension ext = (ExtraPropertiesExtension) this.project.getProperties().get("ext");
		ext.set("JDK7_HOME", this.jdk7Home);
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		((DefaultProject)project).evaluate();
		org.gradle.api.tasks.testing.Test springIoJdk7Test =
				(org.gradle.api.tasks.testing.Test) this.project.getTasks().findByName("springIoJdk7Test");
		assertThat(springIoJdk7Test).isNotNull();
		assertThat(springIoJdk7Test.getExecutable()).isEqualTo(this.java7.getAbsolutePath());
		assertThat(springIoJdk7Test).has(correctClasspath());
		assertThat(springIoJdk7Test.getReports().getJunitXml().getDestination())
				.isEqualTo(new File(this.project.getBuildDir(), "/spring-io-jdk7-test-results"));
		assertThat(springIoJdk7Test.getReports().getHtml().getDestination())
				.isEqualTo(new File(this.project.getBuildDir(), "reports/spring-io-jdk7-tests"));
		assertThat(this.project.getTasks().findByName("springIoJdk8Test")).isNull();
		assertThat(this.project.getTasks().findByName("springIoJdk9Test")).isNull();
	}

	@Test
	public void pluginCreatesSpringIoJdk9TestTaskWhenJdk9IsAvailable() {
		ExtraPropertiesExtension ext = (ExtraPropertiesExtension) project.getProperties().get("ext");
		ext.set("JDK9_HOME", this.jdk9Home);
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		((DefaultProject)project).evaluate();
		org.gradle.api.tasks.testing.Test springIoJdk9Test =
				(org.gradle.api.tasks.testing.Test) this.project.getTasks().findByName("springIoJdk9Test");
		assertThat(springIoJdk9Test).isNotNull();
		assertThat(springIoJdk9Test.getExecutable()).isEqualTo(this.java9.getAbsolutePath());
		assertThat(springIoJdk9Test).has(correctClasspath());
		assertThat(springIoJdk9Test.getReports().getJunitXml().getDestination())
				.isEqualTo(new File(this.project.getBuildDir(), "/spring-io-jdk9-test-results"));
		assertThat(springIoJdk9Test.getReports().getHtml().getDestination())
				.isEqualTo(new File(this.project.getBuildDir(), "reports/spring-io-jdk9-tests"));
		assertThat(this.project.getTasks().findByName("springIoJdk7Test")).isNull();
		assertThat(this.project.getTasks().findByName("springIoJdk8Test")).isNull();
	}

	@Test
	public void pluginCreatesSpringIoJdk8TestTaskWhenJdk8IsAvailable() {
		ExtraPropertiesExtension ext = (ExtraPropertiesExtension) project.getProperties().get("ext");
		ext.set("JDK8_HOME", this.jdk8Home);
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		((DefaultProject)project).evaluate();
		org.gradle.api.tasks.testing.Test springIoJdk8Test =
				(org.gradle.api.tasks.testing.Test) this.project.getTasks().findByName("springIoJdk8Test");
		assertThat(springIoJdk8Test).isNotNull();
		assertThat(springIoJdk8Test.getExecutable()).isEqualTo(this.java8.getAbsolutePath());
		assertThat(springIoJdk8Test).has(correctClasspath());
		assertThat(springIoJdk8Test.getReports().getJunitXml().getDestination())
				.isEqualTo(new File(this.project.getBuildDir(), "/spring-io-jdk8-test-results"));
		assertThat(springIoJdk8Test.getReports().getHtml().getDestination())
				.isEqualTo(new File(this.project.getBuildDir(), "reports/spring-io-jdk8-tests"));
		assertThat(this.project.getTasks().findByName("springIoJdk7Test")).isNull();
		assertThat(this.project.getTasks().findByName("springIoJdk9Test")).isNull();
	}

	@Test
	public void pluginCreatesSpringIoJdk7And8TestTasksWhenJdk7And8AreAvailable() {
		ExtraPropertiesExtension ext = (ExtraPropertiesExtension) project.getProperties().get("ext");
		ext.set("JDK7_HOME", this.jdk7Home);
		ext.set("JDK8_HOME", this.jdk8Home);
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		((DefaultProject)project).evaluate();
		org.gradle.api.tasks.testing.Test springIoJdk7Test =
				(org.gradle.api.tasks.testing.Test) this.project.getTasks().findByName("springIoJdk7Test");
		assertThat(springIoJdk7Test).isNotNull();
		assertThat(springIoJdk7Test.getExecutable()).isEqualTo(this.java7.getAbsolutePath());
		assertThat(springIoJdk7Test).has(correctClasspath());
		assertThat(springIoJdk7Test.getReports().getJunitXml().getDestination())
				.isEqualTo(new File(this.project.getBuildDir(), "/spring-io-jdk7-test-results"));
		org.gradle.api.tasks.testing.Test springIoJdk8Test =
				(org.gradle.api.tasks.testing.Test) this.project.getTasks().findByName("springIoJdk8Test");
		assertThat(springIoJdk8Test).isNotNull();
		assertThat(springIoJdk8Test.getExecutable()).isEqualTo(this.java8.getAbsolutePath());
		assertThat(springIoJdk8Test).has(correctClasspath());
		assertThat(springIoJdk8Test.getReports().getJunitXml().getDestination())
				.isEqualTo(new File(this.project.getBuildDir(), "/spring-io-jdk8-test-results"));
		assertThat(springIoJdk8Test.getReports().getHtml().getDestination())
				.isEqualTo(new File(this.project.getBuildDir(), "reports/spring-io-jdk8-tests"));
		assertThat(this.project.getTasks().findByName("springIoJdk9Test")).isNull();

	}

	@Test
	public void pluginCreatesSpringIoJdk7And8And9TestTasksWhenJdk7And8And9AreAvailable() {
		ExtraPropertiesExtension ext = (ExtraPropertiesExtension) project.getProperties().get("ext");
		ext.set("JDK7_HOME", this.jdk7Home);
		ext.set("JDK8_HOME", this.jdk8Home);
		ext.set("JDK9_HOME", this.jdk9Home);
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		((DefaultProject)project).evaluate();
		org.gradle.api.tasks.testing.Test springIoJdk7Test =
				(org.gradle.api.tasks.testing.Test) this.project.getTasks().findByName("springIoJdk7Test");
		assertThat(springIoJdk7Test).isNotNull();
		assertThat(springIoJdk7Test.getExecutable()).isEqualTo(this.java7.getAbsolutePath());
		assertThat(springIoJdk7Test).has(correctClasspath());
		assertThat(springIoJdk7Test.getReports().getJunitXml().getDestination())
				.isEqualTo(new File(this.project.getBuildDir(), "/spring-io-jdk7-test-results"));
		org.gradle.api.tasks.testing.Test springIoJdk8Test =
				(org.gradle.api.tasks.testing.Test) this.project.getTasks().findByName("springIoJdk8Test");
		assertThat(springIoJdk8Test).isNotNull();
		assertThat(springIoJdk8Test.getExecutable()).isEqualTo(this.java8.getAbsolutePath());
		assertThat(springIoJdk8Test).has(correctClasspath());
		assertThat(springIoJdk8Test.getReports().getJunitXml().getDestination())
				.isEqualTo(new File(this.project.getBuildDir(), "/spring-io-jdk8-test-results"));
		assertThat(springIoJdk8Test.getReports().getHtml().getDestination())
				.isEqualTo(new File(this.project.getBuildDir(), "reports/spring-io-jdk8-tests"));
		org.gradle.api.tasks.testing.Test springIoJdk9Test =
				(org.gradle.api.tasks.testing.Test) this.project.getTasks().findByName("springIoJdk9Test");
		assertThat(springIoJdk9Test).isNotNull();
		assertThat(springIoJdk9Test.getExecutable()).isEqualTo(this.java9.getAbsolutePath());
		assertThat(springIoJdk9Test).has(correctClasspath());
		assertThat(springIoJdk9Test.getReports().getJunitXml().getDestination())
				.isEqualTo(new File(this.project.getBuildDir(), "/spring-io-jdk9-test-results"));
		assertThat(springIoJdk9Test.getReports().getHtml().getDestination())
				.isEqualTo(new File(this.project.getBuildDir(), "reports/spring-io-jdk9-tests"));

	}

	@Test
	@SuppressWarnings("unchecked")
	public void pluginCreatesSpringIoTestTask() {
		ExtraPropertiesExtension ext = (ExtraPropertiesExtension) project.getProperties().get("ext");
		ext.set("JDK7_HOME", this.jdk7Home);
		ext.set("JDK8_HOME", this.jdk8Home);
		ext.set("JDK9_HOME", this.jdk9Home);
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		Task springIoTest = project.getTasks().findByName("springIoTest");
		assertThat(springIoTest.getTaskDependencies().getDependencies(springIoTest))
				.containsExactlyInAnyOrder(this.project.getTasks().findByName("springIoJdk7Test"),
						this.project.getTasks().findByName("springIoJdk8Test"),
						this.project.getTasks().findByName("springIoJdk9Test"));
	}

	@Test
	public void pluginCreatesSpringIoIncompleteExcludesCheckTask() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		assertThat(this.project.getTasks().findByName("springIoIncompleteExcludesCheck")).isNotNull();
	}

	@Test
	public void pluginCreatesSpringIoAlternativeDependenciesCheckTask() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		assertThat(this.project.getTasks().findByName("springIoAlternativeDependenciesCheck")).isNotNull();
	}

	@Test
	public void pluginCreatesSpringIoDependencyVersionMappingCheckTask() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		assertThat(this.project.getTasks().findByName("springIoDependencyVersionMappingCheck")).isNotNull();
	}

	@Test
	public void usesCorrectPathForJavaExecutableOnWindows() {
		assertThat(new SpringIoPlugin().createRelativeJavaExec(true)).isEqualTo("/bin/java.exe");
	}

	@Test
	public void pluginCreatesSpringIoTestSourceSet() {
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(JavaPlugin.class);
		((DefaultProject)project).evaluate();
		SourceSet springIoTestSourceSet = this.project.getConvention().getPlugin(JavaPluginConvention.class)
				.getSourceSets().getByName("springIoTest");
		assertThat(springIoTestSourceSet).isNotNull();
		assertThat(springIoTestSourceSet.getJava().getSrcDirs()).containsExactly(
				new File(project.getProjectDir(), "src/test/java"));
		assertThat(springIoTestSourceSet.getResources().getSrcDirs()).containsExactly(
				new File(project.getProjectDir(), "src/test/resources"));
		assertThat(springIoTestSourceSet.getCompileClasspath().getFiles()).containsExactly(
				new File(project.getBuildDir(), "classes/main"), new File(project.getBuildDir(), "resources/main"));
		assertThat(springIoTestSourceSet.getRuntimeClasspath().getFiles()).containsExactly(
				new File(project.getBuildDir(), "classes/main"), new File(project.getBuildDir(), "resources/main"),
				new File(project.getBuildDir(), "classes/springIoTest"),
				new File(project.getBuildDir(), "resources/springIoTest"));
	}

	@Test
	public void testSourceSetsSrcDirsAreUsedForSpringIoTestSourceSet() {
		applyPlugin(JavaPlugin.class);
		SourceSetContainer sourceSets = this.project.getConvention().getPlugin(JavaPluginConvention.class)
				.getSourceSets();
		sourceSets.getByName("test").getJava().srcDir("custom/java");
		sourceSets.getByName("test").getResources().srcDir("custom/resources");
		applyPlugin(SpringIoPlugin.class);
		((DefaultProject)project).evaluate();
		SourceSet springIoTestSourceSet = sourceSets.getByName("springIoTest");
		assertThat(springIoTestSourceSet).isNotNull();
		assertThat(springIoTestSourceSet.getJava().getSrcDirs()).containsExactly(
				new File(project.getProjectDir(), "src/test/java"),
				new File(project.getProjectDir(), "custom/java"));
		assertThat(springIoTestSourceSet.getResources().getSrcDirs()).containsExactly(
				new File(project.getProjectDir(), "src/test/resources"),
				new File(project.getProjectDir(), "custom/resources"));
		assertThat(springIoTestSourceSet.getCompileClasspath().getFiles()).containsExactly(
				new File(project.getBuildDir(), "classes/main"), new File(project.getBuildDir(), "resources/main"));
		assertThat(springIoTestSourceSet.getRuntimeClasspath().getFiles()).containsExactly(
				new File(project.getBuildDir(), "classes/main"), new File(project.getBuildDir(), "resources/main"),
				new File(project.getBuildDir(), "classes/springIoTest"),
				new File(project.getBuildDir(), "resources/springIoTest"));
	}

	@Test
	public void whenPropdepsPluginIsAppliedSpringIoTestRuntimeExtendsProvidedConfiguration() {
		applyPlugin(JavaPlugin.class);
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(PropDepsPlugin.class);
		assertThat(project.getConfigurations().getByName("springIoTestRuntime").getExtendsFrom())
				.contains(project.getConfigurations().getByName("provided"));
	}

	@Test
	public void whenPropdepsPluginIsAppliedSpringIoTestRuntimeExtendsOptionalConfiguration() {
		applyPlugin(JavaPlugin.class);
		applyPlugin(SpringIoPlugin.class);
		applyPlugin(PropDepsPlugin.class);
		assertThat(project.getConfigurations().getByName("springIoTestRuntime").getExtendsFrom())
				.contains(project.getConfigurations().getByName("optional"));
	}

	private void applyPlugin(Class<?> pluginClass) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("plugin", pluginClass);
		this.project.apply(arguments);
	}

	private Condition<org.gradle.api.tasks.testing.Test> correctClasspath() {
		return new Condition<org.gradle.api.tasks.testing.Test>() {
			@Override
			public boolean matches(org.gradle.api.tasks.testing.Test value) {
				return value.getClasspath().getFiles().containsAll(Arrays.asList(
						new File(project.getBuildDir(), "classes/main"),
						new File(project.getBuildDir(), "resources/main"),
						new File(project.getBuildDir(), "classes/springIoTest"),
						new File(project.getBuildDir(), "resources/springIoTest")));
			}
		};
	}

}
