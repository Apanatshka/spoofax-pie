package mb.cfg.task;

import mb.cfg.CfgScope;
import mb.constraint.pie.ConstraintAnalyzeTaskDef;
import mb.pie.api.ExecContext;
import mb.stratego.common.StrategoRuntime;
import mb.stratego.pie.StrategoTransformTaskDef;
import org.spoofax.interpreter.terms.IStrategoTerm;

import javax.inject.Inject;

@CfgScope
public class CfgNormalize extends StrategoTransformTaskDef<CfgAnalyze.Output> {
    @Inject public CfgNormalize(CfgGetStrategoRuntimeProvider getStrategoRuntimeProvider) {
        super(getStrategoRuntimeProvider, "normalize");
    }

    @Override public String getId() {
        return getClass().getName();
    }

    @Override
    protected StrategoRuntime getStrategoRuntime(ExecContext context, ConstraintAnalyzeTaskDef.Output input) {
        return super.getStrategoRuntime(context, input).addContextObject(input.context);
    }

    @Override
    protected IStrategoTerm getAst(ExecContext context, ConstraintAnalyzeTaskDef.Output input) {
        return input.result.ast;
    }
}
