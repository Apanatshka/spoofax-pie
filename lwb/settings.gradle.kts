rootProject.name = "spoofax3.lwb.root"

pluginManagement {
  repositories {
    maven("https://artifacts.metaborg.org/content/groups/public/")
  }
}

if(org.gradle.util.VersionNumber.parse(gradle.gradleVersion).major < 6) {
  enableFeaturePreview("GRADLE_METADATA")
}

// Only include composite builds when this is the root project (it has no parent), for example when running Gradle tasks
// from the command-line. Otherwise, the parent project (spoofax.root) will include these composite builds.
if(gradle.parent == null) {
  includeBuildWithName("../core", "spoofax3.core.root")
}

fun includeBuildWithName(dir: String, name: String) {
  includeBuild(dir) {
    try {
      ConfigurableIncludedBuild::class.java
        .getDeclaredMethod("setName", String::class.java)
        .invoke(this, name)
    } catch(e: NoSuchMethodException) {
      // Running Gradle < 6, no need to set the name, ignore.
    }
  }
}

fun String.includeProject(id: String, path: String = "$this/$id") {
  include(id)
  project(":$id").projectDir = file(path)
}

"metalang/cfg".run {
  includeProject("cfg")
  includeProject("cfg.cli")
  includeProject("cfg.eclipse")
  includeProject("cfg.intellij")
  includeProject("cfg.spoofax2")
}
"metalang/sdf3".run {
  includeProject("sdf3")
  includeProject("sdf3.cli")
  includeProject("sdf3.eclipse")
  includeProject("sdf3.intellij")
}
"metalang/stratego".run {
  includeProject("stratego")
  includeProject("stratego.cli")
  includeProject("stratego.eclipse")
  includeProject("stratego.intellij")
}
"metalang/esv".run {
  includeProject("esv")
  includeProject("esv.cli")
  includeProject("esv.eclipse")
  includeProject("esv.intellij")
}
"metalang/statix".run {
  includeProject("statix")
  includeProject("statix.cli")
  includeProject("statix.eclipse")
  includeProject("statix.intellij")
}
"metalang/sdf3_ext_statix".run {
  includeProject("sdf3_ext_statix")
  includeProject("sdf3_ext_statix.eclipse")
}
"metalang/spt".run {
  includeProject("spt")
  includeProject("spt.dynamicloading")
  includeProject("spt.cli")
  includeProject("spt.eclipse")
  includeProject("spt.intellij")
}
"metalib/libspoofax2".run {
  includeProject("libspoofax2")
  includeProject("libspoofax2.eclipse")
}
"metalib/libstatix".run {
  includeProject("libstatix")
  includeProject("libstatix.eclipse")
}
"metalib/strategolib".run {
  includeProject("strategolib")
  includeProject("strategolib.eclipse")
}

include("spoofax.lwb.compiler")
include("spoofax.lwb.compiler.dagger")
include("spoofax.lwb.compiler.gradle")

include("spoofax.lwb.dynamicloading")
