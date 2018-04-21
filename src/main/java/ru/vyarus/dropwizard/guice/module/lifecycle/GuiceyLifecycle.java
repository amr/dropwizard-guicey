package ru.vyarus.dropwizard.guice.module.lifecycle;

import ru.vyarus.dropwizard.guice.module.lifecycle.event.GuiceyLifecycleEvent;
import ru.vyarus.dropwizard.guice.module.lifecycle.event.configuration.ConfiguratorsProcessedEvent;
import ru.vyarus.dropwizard.guice.module.lifecycle.event.configuration.InitializationEvent;
import ru.vyarus.dropwizard.guice.module.lifecycle.event.hk.HkConfigurationEvent;
import ru.vyarus.dropwizard.guice.module.lifecycle.event.hk.HkExtensionsInstalledByEvent;
import ru.vyarus.dropwizard.guice.module.lifecycle.event.hk.HkExtensionsInstalledEvent;
import ru.vyarus.dropwizard.guice.module.lifecycle.event.run.*;

/**
 * Guicey lifecycle events. Enum should be used for differentiation of events in {@link GuiceyLifecycleListener}:
 * {@link GuiceyLifecycleEvent#getType()}.
 * <p>
 * Events specified in execution order.
 *
 * @author Vyacheslav Rusakov
 * @since 19.04.2018
 */
public enum GuiceyLifecycle {

    // -- Bundle.initialize()

    /**
     * Called after all registered {@link ru.vyarus.dropwizard.guice.module.support.conf.GuiceyConfigurator}
     * processing. Provides all instances of executed configurators. Not called if no configurators used.
     */
    ConfiguratorsProcessed(ConfiguratorsProcessedEvent.class),
    /**
     * Called after {@link ru.vyarus.dropwizard.guice.GuiceBundle#initialize(io.dropwizard.setup.Bootstrap)} method end.
     * Just a convenient moment to apply registrations into dropwizard {@link io.dropwizard.setup.Bootstrap} object
     * from listener.
     * <p>
     * If commands search is enabled, then all commands found in classpath will be provided in event.
     * <p>
     * Consider this point as somewhere inside of your application's
     * {@link io.dropwizard.Application#initialize(io.dropwizard.setup.Bootstrap)}.
     */
    Initialization(InitializationEvent.class),

    // -- Bundle.run()

    /**
     * Called if configuration from dw bundles enabled and at least one bundle recognized. Provides list of
     * recognized bundles (note: some of these bundles could be actually disabled and not used further).
     */
    BundlesFromDwResolved(BundlesFromDwResolvedEvent.class),
    /**
     * Called if at least one bundle recognized using bundles lookup. Provides list of recognized bundles
     * (note: some of these bundles could be disabled and not used further).
     */
    BundlesFromLookupResolved(BundlesFromLookupResolvedEvent.class),
    /**
     * Called after {@link ru.vyarus.dropwizard.guice.bundle.GuiceyBundleLookup} and resolution form dropwizard
     * bundles mechanisms when all top-level bundles are resolved. Provides a list of all enabled bundles.
     * Not called if no bundles registered.
     */
    BundlesResolved(BundlesResolvedEvent.class),
    /**
     * Called after bundles processing. Note that bundles could register other bundles and so resulted
     * list of installed bundles could be bigger (than in resolution event). Provides a list of all used bundles.
     * Called even if no bundles were used at all (to indicate major lifecycle point).
     */
    BundlesProcessed(BundlesProcessedEvent.class),
    /**
     * Called just before guice injector creation. Provides all configured modules (main and override).
     * Called even when no modules registered.
     */
    InjectorCreation(InjectorCreationEvent.class),
    /**
     * Called when installers resolved (from classpath scan, if enabled) and initialized. Provides list of all
     * enabled installers (which will be used for extensions recognition and installation). Called even if
     * no installers are resolved.
     * <p>
     * Guice context is creating at that moment.
     */
    InstallersResolved(InstallersResolvedEvent.class),
    /**
     * Called when all extensions detected (from classpath scan, if enabled). Provides list of all enabled
     * extension types (instances are not available yet).
     * <p>
     * Guice context is creating at that moment.
     */
    ExtensionsResolved(ExtensionsResolvedEvent.class),
    /**
     * Called after injector creation. Note that starting from this event you have access to injector object.
     * Extensions are not yet installed at this point!
     */
    InjectorCreated(InjectorCreatedEvent.class),
    /**
     * Called when installer installed all related extensions and only for installers actually performed
     * installations (extensions list never empty). Provides installer and installed extensions types.
     * <p>
     * NOTE: {@link ru.vyarus.dropwizard.guice.module.installer.install.JerseyInstaller} installers will no be
     * notified here, even if they participate in installation it is considered as incomplete at that point.
     * <p>
     * Extension instance could be obtained manually from injector.
     */
    ExtensionsInstalledBy(ExtensionsInstalledByEvent.class),
    /**
     * Called after all installers install related extensions.
     * Provides list of all used (enabled) extensions. Not called when no extensions installed.
     * <p>
     * Extension instance could be obtained manually from injector.
     */
    ExtensionsInstalled(ExtensionsInstalledEvent.class),
    /**
     * Called after
     * {@link ru.vyarus.dropwizard.guice.GuiceBundle#run(io.dropwizard.Configuration, io.dropwizard.setup.Environment)}
     * when guicey context is started, extensions installed (but not hk extensions, because neither jersey nor jetty
     * is't start yet).
     * <p>
     * At this point injection to registered commands is performed (this may be important if custom command
     * run application instead of "server").
     * <p>
     * This point is before
     * {@link io.dropwizard.Application#run(io.dropwizard.Configuration, io.dropwizard.setup.Environment)}. Ideal point
     * for jersey and jetty listeners installation (with shortcut event methods).
     */
    ApplicationRun(ApplicationRunEvent.class),

    // -- Application.run()

    /**
     * Hk context starting. At this point jersey is starting and jetty is only initializing. Since that point
     * hk {@link org.glassfish.hk2.api.ServiceLocator} is accessible.
     */
    HkConfiguration(HkConfigurationEvent.class),
    /**
     * Called when {@link ru.vyarus.dropwizard.guice.module.installer.install.JerseyInstaller} installer installed all
     * related extensions and only for installers actually performed installations (extensions list never empty).
     * Provides installer and installed extensions types.
     * <p>
     * At this point hk is not completely started and so hk managed extensions
     * ({@link ru.vyarus.dropwizard.guice.module.installer.feature.jersey.HK2Managed}) couldn't be obtained yet
     * (even though you have access to root service locator). But extensions managed by guice could be obtained
     * from guice context.
     */
    HkExtensionsInstalledBy(HkExtensionsInstalledByEvent.class),
    /**
     * Called after all {@link ru.vyarus.dropwizard.guice.module.installer.install.JerseyInstaller} installers install
     * related extensions and only when at least one extension was installed. Provides list of all used (enabled)
     * extensions.
     * <p>
     * At this point hk is not completely started and so hk managed extensions
     * ({@link ru.vyarus.dropwizard.guice.module.installer.feature.jersey.HK2Managed}) couldn't be obtained yet
     * (even though you have access to root service locator). But extensions managed by guice could be obtained
     * from guice context.
     * <p>
     * To listen hk lifecycle further use jersey events (like in
     * {@link ru.vyarus.dropwizard.guice.module.lifecycle.debug.DebugGuiceyLifecycle}).
     */
    HkExtensionsInstalled(HkExtensionsInstalledEvent.class);

    private final Class<? extends GuiceyLifecycleEvent> type;

    GuiceyLifecycle(final Class<? extends GuiceyLifecycleEvent> type) {
        this.type = type;
    }

    /**
     * @return type of related event
     */
    public Class<? extends GuiceyLifecycleEvent> getType() {
        return type;
    }
}
