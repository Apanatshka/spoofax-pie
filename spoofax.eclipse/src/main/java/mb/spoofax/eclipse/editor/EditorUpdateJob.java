package mb.spoofax.eclipse.editor;

import mb.log.api.Logger;
import mb.log.api.LoggerFactory;
import mb.pie.api.ExecException;
import mb.spoofax.core.language.LanguageComponent;
import mb.spoofax.eclipse.pie.PieRunner;
import mb.spoofax.eclipse.util.StatusUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;

public class EditorUpdateJob extends Job {
    private final Logger logger;
    private final PieRunner pieRunner;
    private final LanguageComponent languageComponent;
    private final String languageDisplayName;
    private final IFile file;
    private final IDocument document;
    private final SpoofaxEditor editor;

    public EditorUpdateJob(
        LoggerFactory loggerFactory,
        PieRunner pieRunner,
        LanguageComponent languageComponent,
        IFile file,
        IDocument document,
        SpoofaxEditor editor
    ) {
        super(languageComponent.getLanguageInstance().getDisplayName() + " editor update");
        this.logger = loggerFactory.create(getClass());
        this.pieRunner = pieRunner;
        this.languageComponent = languageComponent;
        this.languageDisplayName = languageComponent.getLanguageInstance().getDisplayName();
        this.file = file;
        this.document = document;
        this.editor = editor;
    }

    @Override protected IStatus run(@NonNull IProgressMonitor monitor) {
        logger.debug("Running {} editor update job for {}", languageDisplayName, file);
        try {
            return update(monitor);
        } catch(@SuppressWarnings("unused") InterruptedException e) {
            return StatusUtil.cancel();
        } catch(ExecException e) {
            final String message = languageDisplayName + " editor update for " + file + " failed";
            logger.error(message, e);
            return StatusUtil.error(message, e);
        }
    }

    @Override public boolean belongsTo(Object family) {
        return file.equals(family);
    }

    private IStatus update(IProgressMonitor monitor) throws ExecException, InterruptedException {
        pieRunner.addOrUpdateEditor(languageComponent, file, (IDocument & IDocumentExtension4) document, editor,
            monitor);
        return StatusUtil.success();
    }
}
