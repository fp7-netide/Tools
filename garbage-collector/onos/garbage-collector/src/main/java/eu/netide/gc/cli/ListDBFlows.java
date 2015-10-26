/*
 * Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu
 * Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 * Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL), Fraunhofer-Institut für
 * Produktionstechnologie (IPT), Telcaria Ideas SL (TELCA) )
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Author:
 * Antonio Marsico (antonio.marsico@create-net.org)
 */
package eu.netide.gc.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DevicesListCommand;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;

import static org.onosproject.net.DeviceId.deviceId;

import eu.netide.gc.GCServices;

@Command(scope = "onos", name = "mmsdblist",
        description = "Check DB Status")
public class ListDBFlows extends AbstractShellCommand {

    private static final String FORMAT_DB =
            "  device=%s size=%d";
    private static final String FORMAT_DEVICE =
            "  %s";

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = false, multiValued = false)
    String uri = null;

    @Override
    protected void execute() {
        GCServices service = AbstractShellCommand.get(GCServices.class);

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
