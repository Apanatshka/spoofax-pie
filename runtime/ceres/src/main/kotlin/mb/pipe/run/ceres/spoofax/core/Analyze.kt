package mb.pipe.run.ceres.spoofax.core

import com.google.inject.Inject
import mb.ceres.BuildContext
import mb.ceres.BuildException
import mb.ceres.Builder
import mb.ceres.PathStampers
import mb.pipe.run.ceres.util.Tuple2
import mb.pipe.run.core.log.Logger
import mb.pipe.run.core.model.message.Msg
import mb.pipe.run.core.path.PPath
import mb.pipe.run.spoofax.util.MessageConverter
import org.metaborg.core.analysis.AnalysisException
import org.metaborg.core.language.LanguageIdentifier
import org.metaborg.core.messages.IMessage
import org.metaborg.spoofax.core.stratego.StrategoRuntimeFacet
import org.metaborg.spoofax.core.unit.ParseContrib
import org.metaborg.util.iterators.Iterables2
import org.spoofax.interpreter.terms.IStrategoTerm
import java.io.Serializable

class CoreAnalyze @Inject constructor(log: Logger, val messageConverter: MessageConverter) : Builder<CoreAnalyze.Input, CoreAnalyze.Output> {
  companion object {
    val id = "coreAnalyze"
  }

  data class Input(val langId: LanguageIdentifier, val project: PPath, val file: PPath, val ast: IStrategoTerm) : Serializable
  data class Output(val ast: IStrategoTerm?, val messages: ArrayList<Msg>) : Tuple2<IStrategoTerm?, ArrayList<Msg>>

  val log: Logger = log.forContext(CoreAnalyze::class.java)

  override val id = Companion.id
  override fun BuildContext.build(input: Input): Output {
    val spoofax = Spx.spoofax()
    val langImpl = spoofax.languageService.getImpl(input.langId) ?: throw BuildException("Language with id ${input.langId} does not exist or has not been loaded into Spoofax core")

    // Require Stratego runtime files
    val facet = langImpl.facet<StrategoRuntimeFacet>(StrategoRuntimeFacet::class.java)
    if (facet != null) {
      facet.ctreeFiles.forEach { require(it.cPath, PathStampers.hash) }
      facet.jarFiles.forEach { require(it.cPath, PathStampers.hash) }
    }

    // Perform analysis
    val resource = input.file.fileObject
    val project = spoofax.projectService.get(resource) ?: throw BuildException("Cannot analyze $resource, it does not belong to a project")
    val inputUnit = spoofax.unitService.inputUnit(resource, "hack", langImpl, null)
    val parseUnit = spoofax.unitService.parseUnit(inputUnit, ParseContrib(true, true, input.ast, Iterables2.empty<IMessage>(), -1))
    val spoofaxContext = spoofax.contextService.get(project.location(), project, langImpl)
    spoofaxContext.write().use {
      try {
        val analyzeResult = spoofax.analysisService.analyze(parseUnit, spoofaxContext)
        val result = analyzeResult.result();
        val ast = result.ast();
        val messages = messageConverter.toMsgs(result.messages());
        return Output(ast, messages)
      } catch(e: AnalysisException) {
        log.error("Analysis failed unexpectedly", e)
        return Output(null, ArrayList(0))
      }
    }
  }
}

fun BuildContext.analyze(input: CoreAnalyze.Input) = requireOutput(CoreAnalyze::class.java, input)