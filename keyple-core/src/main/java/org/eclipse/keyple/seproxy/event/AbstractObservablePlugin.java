/*
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License version 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 */

package org.eclipse.keyple.seproxy.event;

import org.eclipse.keyple.seproxy.ReadersPlugin;

/**
 * Observable plugin. These plugin can report when a reader is added or removed.
 */
public abstract class AbstractObservablePlugin extends AbstractLoggedObservable<AbstractPluginEvent>
        implements ReadersPlugin {
    public interface PluginObserver extends Observer {
        void update(AbstractPluginEvent event);
    }

    public int compareTo(ReadersPlugin o) {
        return this.getName().compareTo(o.getName());
    }
}
