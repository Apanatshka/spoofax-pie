package mb.calc.spoofax.task.debug;

import mb.calc.spoofax.task.CalcAnalyze;
import mb.calc.spoofax.task.CalcParse;
import mb.calc.spoofax.task.CalcToJava;
import mb.constraint.pie.ConstraintAnalyzeTaskDef;
import mb.pie.api.ExecContext;
import mb.pie.api.TaskDef;
import mb.resource.ResourceKey;
import mb.spoofax.core.language.command.CommandFeedback;
import mb.spoofax.core.language.command.ShowFeedback;
import mb.stratego.common.StrategoUtil;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.Objects;

public class CalcShowToJava implements TaskDef<CalcShowToJava.Args, CommandFeedback> {
    public static class Args implements Serializable {
        public final ResourceKey file;

        public Args(ResourceKey file) {
            this.file = file;
        }

        @Override public boolean equals(Object o) {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;
            final Args args = (Args)o;
            return file.equals(args.file);
        }

        @Override public int hashCode() {
            return Objects.hash(file);
        }

        @Override public String toString() {
            return "Args{" +
                "file=" + file +
                '}';
        }
    }


    private final CalcParse parse;
    private final CalcAnalyze analyze;
    private final CalcToJava calcToJava;

    @Inject public CalcShowToJava(CalcParse parse, CalcAnalyze analyze, CalcToJava calcToJava) {
        this.parse = parse;
        this.analyze = analyze;
        this.calcToJava = calcToJava;
    }


    @Override public String getId() {
        return getClass().getName();
    }

    @Override public CommandFeedback exec(ExecContext context, Args args) throws Exception {
        final ResourceKey file = args.file;
        return context
            .require(calcToJava, analyze.createSupplier(new ConstraintAnalyzeTaskDef.Input(file, parse.createAstSupplier(file))))
            .mapOrElse(
                ast -> CommandFeedback.of(ShowFeedback.showText(StrategoUtil.toString(ast), "Java implementation of '" + file + "'")),
                e -> CommandFeedback.ofTryExtractMessagesFrom(e, file)
            );
    }
}
