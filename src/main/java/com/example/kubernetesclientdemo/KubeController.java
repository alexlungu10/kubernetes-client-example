package com.example.kubernetesclientdemo;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KubeController {
    @GetMapping
    public void getRoot() {
        KubernetesClient kubeClient = new DefaultKubernetesClient();
        kubeClient.pods().inNamespace(kubeClient.getNamespace()).list().getItems().forEach(pod -> System.out.println(pod));
        kubeClient.services().inNamespace(kubeClient.getNamespace()).list().getItems().forEach(System.out::println);
    }
}
