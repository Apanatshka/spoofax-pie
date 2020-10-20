package mb.calc.spoofax.task;

import mb.calc.spoofax.CalcScope;
import mb.calc.spoofax.util.AnalyzedStrategoTransformTaskDef;
import mb.stratego.common.StrategoRuntime;

import javax.inject.Inject;
import javax.inject.Provider;

@CalcScope
public class CalcToJava extends AnalyzedStrategoTransformTaskDef {
    @Inject public CalcToJava(Provider<StrategoRuntime> strategoRuntimeProvider) {
        super(strategoRuntimeProvider, "program-to-java-fixed");
    }

    @Override public String getId() {
        return getClass().getName();
    }
}
