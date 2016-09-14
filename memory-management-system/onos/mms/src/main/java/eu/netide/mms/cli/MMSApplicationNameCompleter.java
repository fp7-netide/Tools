/*
 * Copyright (c) 2016, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu
 * Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 * Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL), Fraunhofer-Institut für
 * Produktionstechnologie (IPT), Telcaria Ideas SL (TELCA) )
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author:
 * Antonio Marsico (antonio.marsico@create-net.org)
 */
package eu.netide.mms.cli;

import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onosproject.app.ApplicationService;
import org.onosproject.app.ApplicationState;
import org.onosproject.cli.AbstractCompleter;
import org.onosproject.core.Application;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import static org.onosproject.app.ApplicationState.ACTIVE;
import static org.onosproject.app.ApplicationState.INSTALLED;
import static org.onosproject.cli.AbstractShellCommand.get;

/**
 * Created by antonio on 22/03/16.
 */
public class MMSApplicationNameCompleter extends AbstractCompleter {

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        ApplicationService service = get(ApplicationService.class);
        Iterator<Application> it = service.getApplications().iterator();
        SortedSet<String> strings = delegate.getStrings();
        while (it.hasNext()) {
            Application app = it.next();
            ApplicationState state = service.getState(app.id());
//            if (previousApps.contains(app.id().name())) {
//                continue;
//            }
            if (state == INSTALLED || state == ACTIVE) {
                strings.add(app.id().name());
            }
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(buffer, cursor, candidates);
    }
}
