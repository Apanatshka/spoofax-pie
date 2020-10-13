package mb.spoofax.compiler.spoofax3.language;

import mb.common.message.KeyedMessages;
import mb.common.result.Result;
import mb.common.util.ListView;
import mb.esv.spoofax.task.EsvCheckMulti;
import mb.esv.spoofax.task.EsvCompile;
import mb.esv.spoofax.task.EsvParse;
import mb.esv.spoofax.util.EsvUtil;
import mb.libspoofax2.LibSpoofax2Exports;
import mb.libspoofax2.spoofax.LibSpoofax2Qualifier;
import mb.pie.api.ExecContext;
import mb.pie.api.Function;
import mb.pie.api.Supplier;
import mb.pie.api.TaskDef;
import mb.pie.api.ValueSupplier;
import mb.pie.api.stamp.resource.ResourceStampers;
import mb.pie.task.archive.UnarchiveFromJar;
import mb.resource.ReadableResource;
import mb.resource.WritableResource;
import mb.resource.classloader.ClassLoaderResource;
import mb.resource.classloader.ClassLoaderResourceLocations;
import mb.resource.classloader.JarFileWithPath;
import mb.resource.hierarchical.HierarchicalResource;
import mb.resource.hierarchical.ResourcePath;
import mb.spoofax.compiler.language.StylerLanguageCompiler;
import mb.str.spoofax.config.StrategoConfigurator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;
import org.spoofax.interpreter.terms.IStrategoTerm;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Value.Enclosing
public class Spoofax3StylerLanguageCompiler implements TaskDef<Spoofax3StylerLanguageCompiler.Input, Result<KeyedMessages, StylerCompilerException>> {
    private final EsvParse parse;
    private final EsvCheckMulti check;
    private final EsvCompile compile;

    private final UnarchiveFromJar unarchiveFromJar;
    private final ClassLoaderResource libSpoofax2DefinitionDir;

    @Inject public Spoofax3StylerLanguageCompiler(
        EsvParse parse,
        EsvCheckMulti check,
        EsvCompile compile,
        StrategoConfigurator configurator,
        UnarchiveFromJar unarchiveFromJar,
        @LibSpoofax2Qualifier("definition-dir") ClassLoaderResource libSpoofax2DefinitionDir
    ) {
        this.parse = parse;
        this.check = check;
        this.compile = compile;
        this.unarchiveFromJar = unarchiveFromJar;
        this.libSpoofax2DefinitionDir = libSpoofax2DefinitionDir;
    }


    @Override public String getId() {
        return getClass().getName();
    }

    @Override
    public Result<KeyedMessages, StylerCompilerException> exec(ExecContext context, Input input) throws Exception {
        // Check main file, root directory, and include directories.
        final HierarchicalResource mainFile = context.require(input.esvMainFile(), ResourceStampers.<HierarchicalResource>exists());
        if(!mainFile.exists() || !mainFile.isFile()) {
            return Result.ofErr(StylerCompilerException.mainFileFail(mainFile.getPath()));
        }
        final HierarchicalResource rootDirectory = context.require(input.esvRootDirectory(), ResourceStampers.<HierarchicalResource>exists());
        if(!rootDirectory.exists() || !rootDirectory.isDirectory()) {
            return Result.ofErr(StylerCompilerException.rootDirectoryFail(rootDirectory.getPath()));
        }
        for(ResourcePath includeDirectoryPath : input.esvIncludeDirs()) {
            final HierarchicalResource includeDirectory = context.require(includeDirectoryPath, ResourceStampers.<HierarchicalResource>exists());
            if(!includeDirectory.exists() || !includeDirectory.isDirectory()) {
                return Result.ofErr(StylerCompilerException.includeDirectoryFail(includeDirectoryPath));
            }
        }

        // Determine libspoofax2 definition directories.
        final LinkedHashSet<HierarchicalResource> libSpoofax2DefinitionDirs = new LinkedHashSet<>(); // LinkedHashSet to remove duplicates while keeping insertion order.
        final LinkedHashSet<Supplier<ResourcePath>> libSpoofax2UnarchiveDirSuppliers = new LinkedHashSet<>();
        if(input.spoofax3LanguageProject().includeLibSpoofax2Exports()) {
            final ClassLoaderResourceLocations locations = libSpoofax2DefinitionDir.getLocations();
            libSpoofax2DefinitionDirs.addAll(locations.directories);
            final ResourcePath libSpoofax2UnarchiveDirectory = input.spoofax3LanguageProject().unarchiveDirectory().appendRelativePath("libspoofax2");
            for(JarFileWithPath jarFileWithPath : locations.jarFiles) {
                final ResourcePath jarFilePath = jarFileWithPath.file.getPath();
                final ResourcePath unarchiveDirectory = libSpoofax2UnarchiveDirectory.appendRelativePath(jarFilePath.getLeaf()); // JAR files always have leaves.
                libSpoofax2UnarchiveDirSuppliers.add(unarchiveFromJar
                    .createSupplier(new UnarchiveFromJar.Input(jarFilePath, unarchiveDirectory, false, false))
                    .map((dir) -> dir.appendAsRelativePath(jarFileWithPath.path))
                );
            }
        }

        // Gather include directories.
        final LinkedHashSet<Supplier<ResourcePath>> includeDirSuppliers = new LinkedHashSet<>();
        includeDirSuppliers.add(new ValueSupplier<>(input.esvRootDirectory()));
        for(String export : LibSpoofax2Exports.getEsvExports()) {
            for(HierarchicalResource definitionDir : libSpoofax2DefinitionDirs) {
                final HierarchicalResource exportDirectory = definitionDir.appendAsRelativePath(export);
                if(exportDirectory.exists()) {
                    includeDirSuppliers.add(new ValueSupplier<>(exportDirectory.getPath()));
                }
            }
            for(Supplier<ResourcePath> unarchiveDirSupplier : libSpoofax2UnarchiveDirSuppliers) {
                includeDirSuppliers.add(unarchiveDirSupplier.map((unarchiveDir) -> unarchiveDir.appendAsRelativePath(export)));
            }
        }

        // Check ESV source files.
        // TODO: this does not check ESV files in include directories.
        final @Nullable KeyedMessages messages = context.require(check.createTask(
            new EsvCheckMulti.Input(rootDirectory.getPath(), EsvUtil.createResourceWalker(), EsvUtil.createResourceMatcher())
        ));
        if(messages.containsError()) {
            return Result.ofErr(StylerCompilerException.checkFail(messages));
        }

        // Compile ESV files to aterm format.
        final Result<IStrategoTerm, ?> result = context.require(compile, new EsvCompile.Input(parse.createAstSupplier(input.esvMainFile()), new ImportFunction(includeDirSuppliers), ListView.of()));
        if(result.isErr()) {
            return Result.ofErr(StylerCompilerException.compilerFail(result.unwrapErr()));
        }
        final IStrategoTerm atermFormat = result.unwrap();
        final WritableResource atermFormatFile = context.getWritableResource(input.esvAtermFormatOutputFile());
        atermFormatFile.writeString(atermFormat.toString());
        context.provide(atermFormatFile);

        return Result.ofOk(messages);
    }


    @Value.Immutable public interface Input extends Serializable {
        class Builder extends Spoofax3StylerLanguageCompilerData.Input.Builder {}

        static Builder builder() { return new Builder(); }


        @Value.Default default ResourcePath esvRootDirectory() {
            return spoofax3LanguageProject().languageProject().project().srcMainDirectory().appendRelativePath("esv");
        }

        @Value.Default default ResourcePath esvMainFile() {
            return esvRootDirectory().appendRelativePath("main.esv");
        }

        List<ResourcePath> esvIncludeDirs();

        List<Supplier<Result<IStrategoTerm, ?>>> esvAdditionalAstSuppliers();


        @Value.Default default String esvAtermFormatFileRelativePath() {
            return "editor.esv.af";
        }

        default ResourcePath esvAtermFormatOutputFile() {
            return spoofax3LanguageProject().generatedResourcesDirectory() // Generated resources directory, so that Gradle includes the aterm format file in the JAR file.
                .appendRelativePath(spoofax3LanguageProject().languageProject().packagePath()) // Append package path to make location unique, enabling JAR files to be merged.
                .appendRelativePath(esvAtermFormatFileRelativePath()) // Append the relative path to the aterm format file.
                ;
        }


        /// Automatically provided sub-inputs

        Spoofax3LanguageProject spoofax3LanguageProject();


        default void syncTo(StylerLanguageCompiler.Input.Builder builder) {
            builder.packedEsvRelativePath(esvAtermFormatFileRelativePath());
        }
    }


    private class ImportFunction implements Function<String, Result<IStrategoTerm, ?>> {
        private final HashSet<Supplier<ResourcePath>> includeDirSuppliers;

        public ImportFunction(
            HashSet<Supplier<ResourcePath>> includeDirSuppliers
        ) {
            this.includeDirSuppliers = includeDirSuppliers;
        }

        @Override public Result<IStrategoTerm, ?> apply(ExecContext context, String input) {
            final ArrayList<IOException> suppressedIoExceptions = new ArrayList<>();
            for(Supplier<ResourcePath> includeDirSupplier : includeDirSuppliers) {
                final Supplier<ResourcePath> pathSupplier = includeDirSupplier.map((includeDir) -> includeDir.appendRelativePath(input).ensureLeafExtension("esv").getNormalized());
                try {
                    final ResourcePath path = context.require(pathSupplier);
                    final ReadableResource resource = context.require(path, ResourceStampers.<ReadableResource>exists());
                    if(!resource.exists()) continue;
                    return context.require(parse.createAstSupplier(pathSupplier.map((ctx, p) -> {
                        try {
                            return ctx.require(p).readString();
                        } catch(IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })));
                } catch(IOException e) {
                    suppressedIoExceptions.add(e);
                } catch(UncheckedIOException e) {
                    suppressedIoExceptions.add(e.getCause());
                }
            }
            final Exception exception = new Exception("Could not import '" + input + "', no ESV module found with that name in any import directory");
            suppressedIoExceptions.forEach(exception::addSuppressed);
            return Result.ofErr(exception);
        }

        @Override public boolean equals(@Nullable Object o) {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;
            final ImportFunction that = (ImportFunction)o;
            return includeDirSuppliers.equals(that.includeDirSuppliers);
        }

        @Override public int hashCode() {
            return Objects.hash(includeDirSuppliers);
        }
    }
}
