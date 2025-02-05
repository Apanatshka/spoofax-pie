package mb.spoofax.lwb.eclipse.compiler;

import dagger.Component;
import mb.cfg.CfgComponent;
import mb.esv.EsvComponent;
import mb.libspoofax2.LibSpoofax2Component;
import mb.libspoofax2.LibSpoofax2ResourcesComponent;
import mb.libstatix.LibStatixComponent;
import mb.libstatix.LibStatixResourcesComponent;
import mb.log.dagger.LoggerComponent;
import mb.resource.dagger.ResourceServiceComponent;
import mb.sdf3.Sdf3Component;
import mb.sdf3_ext_statix.Sdf3ExtStatixComponent;
import mb.spoofax.lwb.compiler.dagger.Spoofax3CompilerComponent;
import mb.spoofax.lwb.compiler.dagger.Spoofax3CompilerModule;
import mb.spoofax.lwb.compiler.dagger.Spoofax3CompilerScope;
import mb.statix.StatixComponent;
import mb.str.StrategoComponent;
import mb.strategolib.StrategoLibComponent;
import mb.strategolib.StrategoLibResourcesComponent;

@Spoofax3CompilerScope
@Component(
    modules = {
        Spoofax3CompilerModule.class,
        EclipseSpoofax3CompilerJavaModule.class
    },
    dependencies = {
        LoggerComponent.class,
        ResourceServiceComponent.class,
        CfgComponent.class,
        Sdf3Component.class,
        StrategoComponent.class,
        EsvComponent.class,
        StatixComponent.class,

        Sdf3ExtStatixComponent.class,

        StrategoLibComponent.class,
        StrategoLibResourcesComponent.class,
        LibSpoofax2Component.class,
        LibSpoofax2ResourcesComponent.class,
        LibStatixComponent.class,
        LibStatixResourcesComponent.class
    }
)
public interface EclipseSpoofax3CompilerComponent extends Spoofax3CompilerComponent {

}
