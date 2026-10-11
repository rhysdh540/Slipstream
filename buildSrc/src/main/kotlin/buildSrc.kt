import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.named

fun Project.propMaybe(name: String): String? {
    var current: Project? = this
    while (current != null) {
        val value = current.properties[name] as? String
        if (value != null) return value
        current = current.parent
    }
    return null
}

fun Project.prop(name: String): String =
    propMaybe(name) ?: error("Property $name not found for project ${project.name}")

val Project.unobfuscated: Boolean get() = !prop("minecraft_version").startsWith("1.")

val Project.jarOutputTask: TaskProvider<out AbstractArchiveTask> get() = tasks.named<AbstractArchiveTask>(
    if (prop("platform") == "neoforge" || prop("platform") == "fabric" && unobfuscated) "jar" else "remapJar"
)

fun <T : Any> NamedDomainObjectContainer<T>.maybeRegister(name: String, config: T.() -> Unit = {}): NamedDomainObjectProvider<T> {
    val provider = if (names.contains(name)) named(name) else register(name)
    provider.configure(config)
    return provider
}
