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

import eu.netide.mms.MMSServices;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DevicesListCommand;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;

import static org.onosproject.net.DeviceId.deviceId;

@Command(scope = "mms", name = "swapsize",
        description = "Check DB Status")
public class ListDBFlows extends AbstractShellCommand {

    private static final String FORMAT_DB =
            "  device=%s size=%d";
    private static final String FORMAT_DEVICE =
            "  %s";

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    String uri = null;

    @Override
    protected void execute() {
        MMSServices service = AbstractShellCommand.get(MMSServices.class);

        DeviceService deviceService = get(DeviceService.class);
        if (uri == null) {
            for (Device device : DevicesListCommand.getSortedDevices(deviceService)) {
                print(FORMAT_DEVICE, device.id());
            }

        } else {
            Integer size = service.getdbSizeFromCLI(deviceId(uri));

            print(FORMAT_DB, uri, size);
        }

    }

}
