package ru.vyarus.dropwizard.guice.lifecycle

import com.google.inject.Binder
import com.google.inject.Module
import io.dropwizard.Application
import io.dropwizard.Bundle
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import ru.vyarus.dropwizard.guice.AbstractTest
import ru.vyarus.dropwizard.guice.GuiceBundle
import ru.vyarus.dropwizard.guice.bundle.lookup.PropertyBundleLookup
import ru.vyarus.dropwizard.guice.module.context.debug.report.diagnostic.DiagnosticConfig
import ru.vyarus.dropwizard.guice.module.context.debug.report.option.OptionsConfig
import ru.vyarus.dropwizard.guice.module.context.debug.report.tree.ContextTreeConfig
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBootstrap
import ru.vyarus.dropwizard.guice.module.installer.bundle.GuiceyBundle
import ru.vyarus.dropwizard.guice.module.installer.feature.jersey.JerseyFeatureInstaller
import ru.vyarus.dropwizard.guice.module.jersey.debug.service.HK2DebugFeature
import ru.vyarus.dropwizard.guice.module.lifecycle.GuiceyLifecycle
import ru.vyarus.dropwizard.guice.module.lifecycle.GuiceyLifecycleAdapter
import ru.vyarus.dropwizard.guice.module.lifecycle.event.GuiceyLifecycleEvent
import ru.vyarus.dropwizard.guice.module.lifecycle.event.HK2PhaseEvent
import ru.vyarus.dropwizard.guice.module.lifecycle.event.InjectorPhaseEvent
import ru.vyarus.dropwizard.guice.module.lifecycle.event.RunPhaseEvent
import ru.vyarus.dropwizard.guice.module.lifecycle.event.configuration.ConfigurationHooksProcessedEvent
import ru.vyarus.dropwizard.guice.module.lifecycle.event.configuration.InitializationEvent
import ru.vyarus.dropwizard.guice.module.lifecycle.event.hk.HK2ConfigurationEvent
import ru.vyarus.dropwizard.guice.module.lifecycle.event.hk.HK2ExtensionsInstalledByEvent
import ru.vyarus.dropwizard.guice.module.lifecycle.event.hk.HK2ExtensionsInstalledEvent
import ru.vyarus.dropwizard.guice.module.lifecycle.event.run.*
import ru.vyarus.dropwizard.guice.hook.GuiceyConfigurationHook
import ru.vyarus.dropwizard.guice.module.yaml.report.BindingsConfig
import ru.vyarus.dropwizard.guice.support.feature.DummyPlugin1
import ru.vyarus.dropwizard.guice.test.spock.UseDropwizardApp

/**
 * @author Vyacheslav Rusakov
 * @since 23.04.2018
 */
@UseDropwizardApp(value = App, hooks = XConf)
class EventsConsistencyTest extends AbstractTest {

    def "Check events consistency"() {

        expect: "all events called"
        Listener.called.size() == GuiceyLifecycle.values().size()

        and: "order correct"
        Listener.called == Arrays.asList(GuiceyLifecycle.values())
    }

    static class App extends Application<Configuration> {
        @Override
        void initialize(Bootstrap<Configuration> bootstrap) {
            bootstrap.addBundle(new DwBundle())
            bootstrap.addBundle(GuiceBundle.builder()
                    .listen(new Listener(),
                    // to call all methods in adapter and make coverage happy
                    new GuiceyLifecycleAdapter())
                    .enableAutoConfig("ru.vyarus.dropwizard.guice.support.feature")
                    .modules(new XMod())
                    .configureFromDropwizardBundles()
                    .disableBundles(DwBundle, LookupBundle)
                    .disableModules(XMod)
                    .disableInstallers(JerseyFeatureInstaller)
                    .disableExtensions(DummyPlugin1, HK2DebugFeature)
                    .searchCommands()
                    .printLifecyclePhasesDetailed()
                    .build())
        }

        @Override
        void run(Configuration configuration, Environment environment) throws Exception {
        }
    }

    static class XMod implements Module {
        @Override
        void configure(Binder binder) {

        }
    }

    static class DwBundle implements Bundle, GuiceyBundle {
        @Override
        void initialize(Bootstrap<?> bootstrap) {
        }

        @Override
        void run(Environment environment) {
        }

        @Override
        void initialize(GuiceyBootstrap bootstrap) {
        }
    }

    static class XConf implements GuiceyConfigurationHook {
        @Override
        void configure(GuiceBundle.Builder builder) {
            PropertyBundleLookup.enableBundles(LookupBundle.class)
        }
    }

    static class LookupBundle implements GuiceyBundle {
        @Override
        void initialize(GuiceyBootstrap bootstrap) {

        }
    }

    static class Listener extends GuiceyLifecycleAdapter {

        static List<GuiceyLifecycle> called = new ArrayList<>()

        @Override
        protected void configurationHooksProcessed(ConfigurationHooksProcessedEvent event) {
            baseChecks(event)
            assert event.hooks.size() == 2
            assert event.hooks[0] instanceof AbstractTest.GuiceyTestHook
            assert event.hooks[1] instanceof XConf
        }

        @Override
        protected void initialization(InitializationEvent event) {
            baseChecks(event)
            assert event.getBootstrap() != null
            assert event.getCommands().size() == 2
            assert event.getBootstrap().getCommands().containsAll(event.getCommands())
        }

        @Override
        protected void beforeRun(BeforeRunEvent event) {
            runChecks(event)
        }

        @Override
        protected void dwBundlesResolved(BundlesFromDwResolvedEvent event) {
            runChecks(event)
            assert event.getBundles().size() == 1
            assert event.getBundles()[0].class == DwBundle
        }

        @Override
        protected void lookupBundlesResolved(BundlesFromLookupResolvedEvent event) {
            runChecks(event)
            assert event.getBundles().size() == 1
            assert event.getBundles()[0].class == LookupBundle
        }

        @Override
        protected void bundlesResolved(BundlesResolvedEvent event) {
            runChecks(event)
            // dw and lookup bundles are disabled
            assert event.getBundles().size() == 3
            assert event.getDisabled().size() == 2
        }

        @Override
        protected void bundlesProcessed(BundlesProcessedEvent event) {
            runChecks(event)
            assert event.getBundles().size() == 3
            assert event.getDisabled().size() == 2
        }

        @Override
        protected void injectorCreation(InjectorCreationEvent event) {
            runChecks(event)
            assert event.modules.size() == 3
            assert event.overridingModules.isEmpty()
            assert event.disabled.size() == 1
        }

        @Override
        protected void installersResolved(InstallersResolvedEvent event) {
            runChecks(event)
            assert event.installers.size() == 8
            assert event.disabled.size() == 1
        }

        @Override
        protected void extensionsResolved(ExtensionsResolvedEvent event) {
            runChecks(event)
            assert event.extensions.size() == 13
            assert event.disabled.size() == 2
        }

        @Override
        protected void extensionsInstalledBy(ExtensionsInstalledByEvent event) {
            injectorChecks(event)
            assert event.getInstaller() != null
            assert !event.getInstalled().isEmpty()
        }

        @Override
        protected void extensionsInstalled(ExtensionsInstalledEvent event) {
            injectorChecks(event)
            assert !event.getExtensions().isEmpty()
        }

        @Override
        protected void applicationRun(ApplicationRunEvent event) {
            injectorChecks(event)
        }

        @Override
        protected void hk2Configuration(HK2ConfigurationEvent event) {
            hkCheck(event)
        }

        @Override
        protected void hk2ExtensionsInstalledBy(HK2ExtensionsInstalledByEvent event) {
            hkCheck(event)
            assert event.getInstaller() != null
            assert !event.getInstalled().isEmpty()
        }

        @Override
        protected void hk2ExtensionsInstalled(HK2ExtensionsInstalledEvent event) {
            hkCheck(event)
            assert !event.getExtensions().isEmpty()
        }

        private void baseChecks(GuiceyLifecycleEvent event) {
            assert event != null
            assert event.options != null
            if (!called.contains(event.getType())) {
                called.add(event.getType())
            }
        }

        private void runChecks(RunPhaseEvent event) {
            baseChecks(event)
            assert event.getBootstrap() != null
            assert event.getEnvironment() != null
            assert event.getConfiguration() != null
            assert event.getConfigurationTree() != null
            assert event.renderConfigurationBindings(new BindingsConfig().showNullValues()) != null
        }

        private void injectorChecks(InjectorPhaseEvent event) {
            runChecks(event)
            assert event.injector != null
            assert event.configurationInfo != null
            assert event.reportRenderer != null
            assert event.reportRenderer.renderStats(false) != null
            assert event.reportRenderer.renderOptions(new OptionsConfig()) != null
            assert event.reportRenderer.renderConfigurationSummary(new DiagnosticConfig()) != null
            assert event.reportRenderer.renderConfigurationTree(new ContextTreeConfig()) != null
        }

        private void hkCheck(HK2PhaseEvent event) {
            injectorChecks(event)
            assert event.getLocator() != null
        }
    }
}
