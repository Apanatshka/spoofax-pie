package mb.spoofax.compiler.spoofaxcore;

import mb.pie.api.MixedSession;
import mb.resource.fs.FSPath;
import mb.spoofax.compiler.spoofaxcore.tiger.TigerInputs;
import org.junit.jupiter.api.Test;

class ClassloaderResourcesCompilerTest extends TestBase {
    @Test void testCompilerDefaults() throws Exception {
        final FSPath baseDirectory = new FSPath(fileSystem.getPath("repo"));
        final Shared shared = TigerInputs.shared(baseDirectory).build();
        final LanguageProject languageProject = TigerInputs.languageProject(shared).build();

        final ClassloaderResourcesCompiler.Input input = TigerInputs.classloaderResourcesProjectInput(shared, languageProject).build();
        try(MixedSession session = pie.newSession()) {
            session.require(component.getClassloaderResourcesCompiler().createTask(input));
            fileAssertions.scopedExists(input.classesGenDirectory(), (s) -> {
                s.assertPublicJavaClass(input.classloaderResources(), "TigerClassloaderResources");
            });
        }
    }
}
