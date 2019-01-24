/**
 * Copyright (C) 2015 Red Hat, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.kubernetesclientdemo;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class DeploymentExamples {
    public static final String NAMESPACE = "default";
    public static final String SERVICE_ACCOUNT = "default";
    private static final Logger logger = LoggerFactory.getLogger(DeploymentExamples.class);
    public static final String MHP_DEMO = "mhp-demo";

    public static void main(String[] args) throws InterruptedException {
        Config config = new ConfigBuilder().build();
        KubernetesClient client = new DefaultKubernetesClient(config);

        try {
            // Create a namespace for all our stuff
            Namespace ns = new NamespaceBuilder().withNewMetadata().withName(NAMESPACE).addToLabels("this", "rocks").endMetadata().build();
            log("Created namespace", client.namespaces().createOrReplace(ns));


            ServiceAccount fabric8 = new ServiceAccountBuilder().withNewMetadata().withName(SERVICE_ACCOUNT).endMetadata().build();

            client.serviceAccounts().inNamespace(NAMESPACE).createOrReplace(fabric8);
            Deployment deployment = new DeploymentBuilder()
                    .withNewMetadata()
                    .withName(MHP_DEMO)
                    .endMetadata()
                    .withNewSpec()
                    .withReplicas(1)
                    .withNewSelector()
                    .addToMatchLabels("app", MHP_DEMO)
                    .endSelector()
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels("app", MHP_DEMO)
                    .endMetadata()
                    .withNewSpec()
                    .addNewContainer()
                    .withName(MHP_DEMO)
                    .withImage("nginx")
                    .addNewPort()
                    .withContainerPort(80)
                    .endPort()
                    .endContainer()
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();

            Service service = new ServiceBuilder()
                    .withNewMetadata()
                    .withName("mhp-demo-service")
                    .endMetadata()
                    .withNewSpec()
                    .withSelector(Collections.singletonMap("app", MHP_DEMO))
                    .addNewPort()
                    .withName("test-port")
                    .withProtocol("TCP")
                    .withPort(8081)
                    .withTargetPort(new IntOrString(80))
                    .endPort()
                    .withType("LoadBalancer")
                    .endSpec()
                    .withNewStatus()
                    .withNewLoadBalancer()
                    .addNewIngress()
                    .withHostname("localhost")
                    .endIngress()
                    .endLoadBalancer()
                    .endStatus()
                    .build();

            //CLEAN
            log("clean...");
            NamespacedKubernetesClient namespacedKubernetesClient = ((DefaultKubernetesClient) client).inNamespace(NAMESPACE);
            namespacedKubernetesClient.resource(deployment).delete();
            namespacedKubernetesClient.resource(service).delete();

            deployment = client.apps().deployments().inNamespace(NAMESPACE).create(deployment);
            log("Created deployment", deployment);

            System.err.println("Scaling up:" + deployment.getMetadata().getName());
            client.apps().deployments().inNamespace(NAMESPACE).withName(MHP_DEMO).scale(2, true);
            log("Created replica sets:", client.apps().replicaSets().inNamespace(NAMESPACE).list().getItems());
            System.err.println("Deleting:" + deployment.getMetadata().getName());


            //service
            service = client.services().inNamespace(NAMESPACE).create(service);
            log("Created service", service);
            log("Done.");

        } finally {
            //  client.namespaces().withName(NAMESPACE).delete();
            client.close();
        }
    }


    private static void log(String action, Object obj) {
        logger.info("{}: {}", action, obj);
    }

    private static void log(String action) {
        logger.info(action);
    }
}
