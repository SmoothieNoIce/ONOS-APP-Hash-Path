/*
 * Copyright 2024-present Open Networking Foundation
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
 */
package nycu.wireless.finalproject;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onlab.packet.Ethernet;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyService;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceService deviceService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationId appId;
    private ConcurrentMap<Set<Criterion>, Intent> intentMap ;
    private WidestpathPacketProcessor packetProcessor;
    private DynamicWidestRouting widestPathRouting;

    //establish a matrix that represents the bandwidth of each edge
    public static int matrixLen; // support one less vertexes
    public static long[][] edgeCapacity;


    @Activate
    protected void activate() {

        appId = coreService.registerApplication("org.ece595.widest");
        intentMap = new ConcurrentHashMap<>();


        widestPathRouting = new DynamicWidestRouting(hostService, topologyService, deviceService);
        //widestPathRouting.setStatic();

        packetProcessor = new WidestpathPacketProcessor(appId, widestPathRouting,intentService, deviceService, intentMap);

        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));

        packetService.requestPackets(DefaultTrafficSelector.builder().matchEthType(Ethernet.TYPE_IPV4).build(),
                                     PacketPriority.REACTIVE, appId);

        try {
            final String fileName = "/home/mininet/widest-path-app/t2_6nodes_easy.txt";

            ReadEdgeParameter readedgetool = new ReadEdgeParameter(fileName);
            readedgetool.readBw();
        } catch (Exception e1) {
            log.warn("\n***************" +
                       "\n" + e1.toString() +
                       "\nCan not read edge parameters from ReadEdgeParameter!\n**************\n");
            return;
        }

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(packetProcessor);
        packetService.cancelPackets(DefaultTrafficSelector.builder()
                                            .matchEthType(Ethernet.TYPE_IPV4).build(),
                                    PacketPriority.REACTIVE, appId);

        intentMap.values().forEach(intent -> {
            IntentTimeout intentTimeout = new IntentTimeout(intent,intentService, intentMap, log);
            intentTimeout.schedule(0);
        });
        intentMap.clear();

        log.info("Stopped");

    }
}


