package ru.vyarus.dropwizard.guice.cases.hkscope.support

import org.glassfish.hk2.api.Factory
import org.glassfish.jersey.server.model.Parameter
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider
import ru.vyarus.dropwizard.guice.module.installer.feature.jersey.GuiceManaged

import javax.ws.rs.ext.Provider

/**
 * @author Vyacheslav Rusakov
 * @since 19.01.2016
 */
@Provider
@GuiceManaged
class GuiceValueFactoryProvider implements ValueFactoryProvider {

    @Override
    Factory<?> getValueFactory(Parameter parameter) {
        return null
    }

    @Override
    ValueFactoryProvider.PriorityType getPriority() {
        return ValueFactoryProvider.Priority.LOW
    }
}
